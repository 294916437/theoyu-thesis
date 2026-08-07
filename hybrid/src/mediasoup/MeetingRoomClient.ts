import { Device, types as mediasoupTypes} from "mediasoup-client";
import {
  mediaDevices,
  MediaStream,
  MediaStreamTrack,
  RTCView,
} from "react-native-webrtc";
import {Platform} from "react-native";
import {io, Socket} from "socket.io-client";
import type {RoomChatMessage, RoomParticipant, RoomState, SfuProducerState, SfuTransportState} from "../types";

export type MeetingRoomClientState = {
  phase: "idle" | "connecting" | "ready" | "failed" | "removed" | "closed";
  message?: string;
  localStream?: MediaStream;
  remoteStreams: Record<string, MediaStream>;
  remoteParticipants: RoomParticipant[];
  chatMessages: RoomChatMessage[];
  networkQuality?: string;
  networkQualityLabel?: string;
  rttMillis?: number;
  globalSpotlightPeerId?: string;
  spotlightRequest?: {
    requesterId: string;
    requesterUsername: string;
  };
  consumerLayers: Record<string, {producerId: string; spatialLayer: number | null; temporalLayer: number | null}>;
  lastStats?: unknown;
};

type Listener = (state: MeetingRoomClientState) => void;

const DEFAULT_SFU_SOCKET_URL = "http://10.0.2.2:3000";

const SIMULCAST_ENCODINGS = [
  { rid: "r0", scaleResolutionDownBy: 4, maxBitrate: 100000 },
  { rid: "r1", scaleResolutionDownBy: 2, maxBitrate: 300000 },
  { rid: "r2", scaleResolutionDownBy: 1, maxBitrate: 900000 },
];

export class MeetingRoomClient {
  private device?: mediasoupTypes.Device;
  private socket?: Socket;
  private sendTransport?: mediasoupTypes.Transport;
  private recvTransport?: mediasoupTypes.Transport;
  private roomId = "";
  private currentUserId = "";
  private currentUsername = "";
  private producers: mediasoupTypes.Producer[] = [];
  private consumers: mediasoupTypes.Consumer[] = [];
  private consumedProducerIds = new Set<string>();
  private remoteProducers = new Map<string, SfuProducerState>();
  private state: MeetingRoomClientState = {phase: "idle", remoteStreams: {}, remoteParticipants: [], chatMessages: [], consumerLayers: {}};
  private listener?: Listener;

  subscribe(listener: Listener) {
    this.listener = listener;
    listener(this.state);
  }

  async connect(roomState: RoomState) {
    if (!roomState.meeting || this.state.phase === "connecting") {
      return;
    }
    const nextRoomId = roomState.meeting.roomId || roomState.meeting.roomNo;
    if (this.device && this.roomId === nextRoomId && this.state.phase === "ready") {
      return;
    }

    this.setState({phase: "connecting", message: "正在初始化媒体引擎"});

    try {
      this.roomId = nextRoomId;
      this.currentUserId = roomState.currentUserId || "";
      this.currentUsername = roomState.currentUsername || "我";
      
      let sfuUrl = roomState.meeting.sfuServerUrl || DEFAULT_SFU_SOCKET_URL;
      sfuUrl = sfuUrl.replace(/^ws:\/\//, "http://").replace(/^wss:\/\//, "https://");
      
      if (Platform.OS === "android") {
        sfuUrl = sfuUrl.replace("127.0.0.1", "10.0.2.2").replace("localhost", "10.0.2.2");
      }
      
      this.socket = io(sfuUrl, {
        transports: ["websocket"],
        auth: {token: roomState.authToken},
      });
      await this.waitForSocket();
      const joinResponse = await this.emitAck("joinRoom", {
        roomId: this.roomId,
        userId: this.currentUserId,
        username: this.currentUsername,
        token: roomState.authToken,
        withMedia: true,
      });
      this.setState({remoteParticipants: parseJoinRoomParticipants(joinResponse)});

      this.device = new Device({handlerName: "ReactNative106"});
      const routerResponse = await this.emitAck("getRouterRtpCapabilities", {
        roomId: this.roomId,
      });
      const routerCapabilities =
        routerResponse?.rtpCapabilities ??
        parseJson(roomState.mediaState.routerRtpCapabilitiesJson);
      if (!routerCapabilities) {
        throw new Error("缺少 SFU router capabilities");
      }
      await this.device.load({routerRtpCapabilities: routerCapabilities});

      await this.createTransports();
      const localStream = await this.createLocalStream(roomState);
      await this.publishLocalTracks(localStream);
      this.bindRoomEvents();
      await this.consumeRemoteProducers([
        ...roomState.mediaState.remoteProducers,
        ...parseJoinRoomProducers(joinResponse),
      ]);

      this.setState({phase: "ready", message: undefined, localStream});
    } catch (error) {
      this.setState({
        phase: "failed",
        message: error instanceof Error ? error.message : "媒体引擎初始化失败",
      });
    }
  }

  async update(roomState: RoomState) {
    if (!this.device && roomState.meeting && roomState.authToken) {
      await this.connect(roomState);
      return;
    }

    await this.consumeRemoteProducers(roomState.mediaState.remoteProducers);
    this.producers.forEach(producer => {
      if (producer.kind === "audio") {
        this.setProducerEnabled(producer, roomState.audioEnabled);
      }
      if (producer.kind === "video") {
        this.setProducerEnabled(producer, roomState.videoEnabled);
      }
    });
  }

  close() {
    this.socket?.emit("leaveRoom", {roomId: this.roomId});
    this.state.localStream?.getTracks().forEach(track => track.stop());
    Object.values(this.state.remoteStreams).forEach(stream => {
      stream.getTracks().forEach(track => track.stop());
    });
    this.consumers.forEach(consumer => consumer.close());
    this.producers.forEach(producer => producer.close());
    this.sendTransport?.close();
    this.recvTransport?.close();
    this.socket?.disconnect();
    this.consumedProducerIds.clear();
    this.remoteProducers.clear();
    this.roomId = "";
    this.currentUserId = "";
    this.currentUsername = "";
    this.setState({phase: "idle", remoteStreams: {}, remoteParticipants: [], chatMessages: [], consumerLayers: {}, localStream: undefined, message: undefined, networkQuality: undefined, networkQualityLabel: undefined, rttMillis: undefined, globalSpotlightPeerId: undefined, spotlightRequest: undefined, lastStats: undefined});
  }

  private async createTransports() {
    if (!this.device) return;
    const sendTransport = await this.createSfuTransport(true, false);
    this.sendTransport = this.device.createSendTransport(toTransportOptions(sendTransport));
    this.bindTransportEvents(this.sendTransport);

    const recvTransport = await this.createSfuTransport(false, true);
    this.recvTransport = this.device.createRecvTransport(toTransportOptions(recvTransport));
    this.bindTransportEvents(this.recvTransport);
  }

  private bindTransportEvents(transport: mediasoupTypes.Transport) {
    transport.on("connect", ({dtlsParameters}, callback, errback) => {
      this.emitAck("connectWebRtcTransport", {
        roomId: this.roomId,
        transportId: transport.id,
        dtlsParameters,
      })
        .then(() => callback())
        .catch(errback);
    });

    transport.on("produce", ({kind, rtpParameters, appData}, callback, errback) => {
      this.emitAck("produce", {
        roomId: this.roomId,
        transportId: transport.id,
        kind,
        rtpParameters,
        appData,
      })
        .then(response => callback({id: response?.id ?? ""}))
        .catch(errback);
    });
  }

  private async createLocalStream(roomState: RoomState) {
    if (!roomState.audioEnabled && !roomState.videoEnabled) {
      return new MediaStream();
    }
    return mediaDevices.getUserMedia({
      audio: roomState.audioEnabled,
      video: roomState.videoEnabled
        ? {
            facingMode: "user",
            width: 640,
            height: 480,
            frameRate: 30,
          }
        : false,
    }) as Promise<MediaStream>;
  }

  private async produceVideoSafely(track: MediaStreamTrack) {
    if (!this.sendTransport) return null;
    try {
      const producer = await this.sendTransport.produce({
        track: track as any,
        encodings: SIMULCAST_ENCODINGS,
        appData: {source: "react-native"},
      });
      return producer;
    } catch (err) {
      console.warn("Failed to produce video with simulcast on Android, falling back to single encoding...", err);
      try {
        const producer = await this.sendTransport.produce({
          track: track as any,
          appData: {source: "react-native"},
        });
        return producer;
      } catch (fallbackErr) {
        console.error("Fallback video production also failed", fallbackErr);
        throw fallbackErr;
      }
    }
  }

  private async publishLocalTracks(stream: MediaStream) {
    if (!this.sendTransport) return;
    for (const track of stream.getTracks() as MediaStreamTrack[]) {
      const isVideo = track.kind === "video";
      if (isVideo) {
        const producer = await this.produceVideoSafely(track);
        if (producer) {
          this.producers.push(producer);
          this.monitorProducerScore(producer);
        }
      } else {
        const producer = await this.sendTransport.produce({
          track: track as any,
          appData: {source: "react-native"},
        });
        this.producers.push(producer);
        this.monitorProducerScore(producer);
      }
    }
  }

  private bindRoomEvents() {
    this.socket?.off("connect");
    this.socket?.on("connect", () => {
      this.setState({message: undefined});
    });

    this.socket?.off("disconnect");
    this.socket?.on("disconnect", reason => {
      if (this.state.phase === "removed" || this.state.phase === "closed") {
        return;
      }
      this.setState({phase: "failed", message: `Socket 已断开：${reason}`});
    });

    this.socket?.off("connect_error");
    this.socket?.on("connect_error", error => {
      this.setState({phase: "failed", message: error?.message || "Socket 连接失败"});
    });

    this.socket?.off("ping");
    this.socket?.on("ping", data => {
      this.socket?.emit("pong", { timestamp: data?.timestamp || Date.now() });
    });

    this.socket?.off("rtt");
    this.socket?.on("rtt", data => {
      this.setState({ rttMillis: data?.rtt });
    });

    this.socket?.off("removedFromRoom");
    this.socket?.on("removedFromRoom", event => {
      this.setState({ phase: "removed", message: event?.reason || "您已被主持人移出会议" });
    });

    this.socket?.off("roomClosed");
    this.socket?.on("roomClosed", event => {
      this.setState({ phase: "closed", message: event?.reason ? `会议已关闭：${event.reason}` : "会议已由主持人关闭" });
    });

    this.socket?.off("spotlightRequest");
    this.socket?.on("spotlightRequest", data => {
      this.setState({
        spotlightRequest: {
          requesterId: data?.requesterId ?? "",
          requesterUsername: data?.requesterUsername ?? "参会者",
        },
        message: `${data?.requesterUsername ?? "参会者"} 申请成为焦点画面`,
      });
    });

    this.socket?.off("spotlightChanged");
    this.socket?.on("spotlightChanged", data => {
      this.setState({ globalSpotlightPeerId: data?.active ? data.targetPeerId : undefined });
    });

    this.socket?.off("newPeer");
    this.socket?.on("newPeer", peer => {
      this.upsertRemoteParticipant(toParticipant(peer));
    });

    this.socket?.off("peerLeft");
    this.socket?.on("peerLeft", ({peerId, userId}: {peerId?: string; userId?: string}) => {
      this.setState({
        remoteParticipants: this.state.remoteParticipants.filter(
          participant => participant.peerId !== peerId && participant.userId !== userId,
        ),
      });
    });

    this.socket?.off("newProducer");
    this.socket?.on("newProducer", producer => {
      const normalized = normalizeProducer(producer);
      this.upsertRemoteParticipant(toParticipant(producer, normalized.kind));
      this.consumeRemoteProducers([normalized]).catch(error => {
        this.setState({
          phase: "failed",
          message: error instanceof Error ? error.message : "订阅远端流失败",
        });
      });
    });
    this.socket?.off("consumerClosed");
    this.socket?.on("consumerClosed", ({consumerId}: {consumerId?: string}) => {
      this.consumers = this.consumers.filter(consumer => consumer.id !== consumerId);
    });

    this.socket?.off("consumerLayersChanged");
    this.socket?.on("consumerLayersChanged", event => {
      if (!event?.consumerId) return;
      this.setState({
        consumerLayers: {
          ...this.state.consumerLayers,
          [event.consumerId]: {
            producerId: event.producerId ?? "",
            spatialLayer: event.spatialLayer ?? null,
            temporalLayer: event.temporalLayer ?? null,
          },
        },
      });
    });

    this.socket?.off("producerStateChanged");
    this.socket?.on("producerStateChanged", event => {
      this.applyProducerState(event?.peerId, event?.kind, !event?.paused);
    });

    this.socket?.off("producerPaused");
    this.socket?.on("producerPaused", event => {
      const producer = this.remoteProducers.get(event?.producerId);
      this.applyProducerState(event?.peerId, event?.kind ?? producer?.kind, false);
    });

    this.socket?.off("producerResumed");
    this.socket?.on("producerResumed", event => {
      const producer = this.remoteProducers.get(event?.producerId);
      this.applyProducerState(event?.peerId, event?.kind ?? producer?.kind, true);
    });

    this.socket?.off("producerClosed");
    this.socket?.on("producerClosed", event => {
      this.removeRemoteProducer(event?.producerId, event?.peerId);
    });

    this.socket?.off("handRaiseChanged");
    this.socket?.on("handRaiseChanged", event => {
      this.applyHandRaiseState(event?.peerId || event?.userId, !!event?.raised, event?.username);
    });
  }

  async closeRoom() {
    await this.emitAck("closeRoom", {
      roomId: this.roomId,
      reason: "host_closed",
    });
  }

  async toggleAudio(enabled?: boolean) {
    let producer = this.producers.find(item => item.kind === "audio");
    const nextEnabled = enabled ?? (producer ? !(producer as any).paused : true);
    
    if (nextEnabled && !producer) {
      try {
        const stream = await mediaDevices.getUserMedia({ audio: true }) as MediaStream;
        const audioTrack = stream.getAudioTracks()[0];
        if (audioTrack) {
          if (!this.state.localStream) {
            this.setState({ localStream: stream });
          } else {
            this.state.localStream.addTrack(audioTrack);
            this.setState({ localStream: this.state.localStream });
          }
          if (this.sendTransport) {
            producer = await this.sendTransport.produce({
              track: audioTrack as any,
              appData: { source: "react-native" },
            });
            this.producers.push(producer);
            this.monitorProducerScore(producer);
          }
        }
      } catch (err) {
        console.warn("Failed to create audio track:", err);
      }
    }

    this.updateLocalTrackEnabled("audio", nextEnabled);
    if (producer) {
      this.setProducerEnabled(producer, nextEnabled, false);
    }
    await this.emitAck("toggleAudio", {roomId: this.roomId, enabled: nextEnabled}).catch(() => undefined);
  }

  async toggleVideo(enabled?: boolean) {
    let producer = this.producers.find(item => item.kind === "video");
    const nextEnabled = enabled ?? (producer ? !(producer as any).paused : true);
    
    if (nextEnabled && !producer) {
      try {
        const stream = await mediaDevices.getUserMedia({
          video: {
            facingMode: "user",
            width: 640,
            height: 480,
            frameRate: 30,
          },
        }) as MediaStream;
        const videoTrack = stream.getVideoTracks()[0];
        if (videoTrack) {
          if (!this.state.localStream) {
            this.setState({ localStream: stream });
          } else {
            this.state.localStream.addTrack(videoTrack);
            this.setState({ localStream: this.state.localStream });
          }
          if (this.sendTransport) {
            producer = await this.produceVideoSafely(videoTrack) || undefined;
            if (producer) {
              this.producers.push(producer);
              this.monitorProducerScore(producer);
            }
          }
        }
      } catch (err) {
        console.warn("Failed to create video track:", err);
      }
    }

    this.updateLocalTrackEnabled("video", nextEnabled);
    if (producer) {
      this.setProducerEnabled(producer, nextEnabled, false);
    }
    await this.emitAck("toggleVideo", {roomId: this.roomId, enabled: nextEnabled}).catch(() => undefined);
  }

  async switchCamera() {
    const videoTrack = this.state.localStream?.getVideoTracks()[0] as any;
    if (videoTrack?.switchCamera) {
      await videoTrack.switchCamera();
    }
  }

  async toggleHandRaised(raised: boolean) {
    this.setState({
      remoteParticipants: this.state.remoteParticipants.map(participant =>
        participant.peerId === this.currentUserId || participant.userId === this.currentUserId
          ? {...participant, handRaised: raised}
          : participant,
      ),
    });
    await this.emitAck("setHandRaised", {raised}).catch(() => undefined);
  }

  async sendMessage(content: string) {
    const message = {
      id: `${Date.now()}`,
      senderName: this.currentUsername || "我",
      content: content.trim(),
      timestamp: new Date().toLocaleTimeString([], {hour: "2-digit", minute: "2-digit"}),
      isLocal: true,
    };
    this.setState({chatMessages: [...this.state.chatMessages, message]});
  }

  async pauseConsumer(consumerId: string) {
    await this.emitAck("pauseConsumer", {roomId: this.roomId, consumerId});
  }

  async resumeConsumer(consumerId: string) {
    await this.emitAck("resumeConsumer", {roomId: this.roomId, consumerId});
  }

  async closeProducer(producerId: string) {
    await this.emitAck("closeProducer", {roomId: this.roomId, producerId});
    const producer = this.producers.find(item => item.id === producerId);
    producer?.close();
    this.producers = this.producers.filter(item => item.id !== producerId);
  }

  async setPreferredLayers(consumerId: string, spatialLayer: number, temporalLayer?: number) {
    await this.emitAck("setPreferredLayers", {
      roomId: this.roomId,
      consumerId,
      spatialLayer,
      temporalLayer,
    });
  }

  async getStats(target?: {producerId?: string; consumerId?: string}) {
    const response = await this.emitAck("getStats", {
      roomId: this.roomId,
      producerId: target?.producerId,
      consumerId: target?.consumerId,
    });
    this.setState({lastStats: response?.stats});
    return response?.stats;
  }

  async hostToggleParticipantAudio(participant: RoomParticipant) {
    await this.emitAck("hostToggleAudio", {
      roomId: this.roomId,
      targetPeerId: participant.peerId || participant.userId,
      enabled: !participant.audioEnabled,
    });
  }

  async hostToggleParticipantVideo(participant: RoomParticipant) {
    await this.emitAck("hostToggleVideo", {
      roomId: this.roomId,
      targetPeerId: participant.peerId || participant.userId,
      enabled: !participant.videoEnabled,
    });
  }

  async removeParticipant(participant: RoomParticipant) {
    await this.emitAck("removeParticipant", {
      targetPeerId: participant.peerId || participant.userId,
    });
  }

  async muteAll() {
    await this.emitAck("hostMuteAll", { roomId: this.roomId }).catch(() => undefined);
  }

  async disableAllVideo() {
    await this.emitAck("hostDisableAllVideo", { roomId: this.roomId }).catch(() => undefined);
  }

  async requestSpotlight() {
    await this.emitAck("requestSpotlight", {
      roomId: this.roomId,
      requesterId: this.currentUserId,
      requesterUsername: this.currentUsername,
    }).catch(() => undefined);
  }

  async setSpotlight(targetPeerId: string | null, active: boolean) {
    await this.emitAck("setSpotlight", {
      roomId: this.roomId,
      targetPeerId,
      active,
    }).catch(() => undefined);
  }

  private setProducerEnabled(producer: mediasoupTypes.Producer, enabled: boolean, syncWithServer = true) {
    const paused = !!(producer as any).paused;
    if (enabled && paused) {
      producer.resume();
      if (syncWithServer) {
        this.emitAck("resumeProducer", {
          roomId: this.roomId,
          producerId: producer.id,
        }).catch(() => undefined);
      }
    }
    if (!enabled && !paused) {
      producer.pause();
      if (syncWithServer) {
        this.emitAck("pauseProducer", {
          roomId: this.roomId,
          producerId: producer.id,
        }).catch(() => undefined);
      }
    }
  }

  private updateLocalTrackEnabled(kind: "audio" | "video", enabled: boolean) {
    const tracks = kind === "audio" ? this.state.localStream?.getAudioTracks() : this.state.localStream?.getVideoTracks();
    tracks?.forEach(track => {
      track.enabled = enabled;
    });
  }

  private applyProducerState(peerId: string | undefined, kind: string | undefined, enabled: boolean) {
    if (!peerId || !kind) return;
    this.setState({
      remoteParticipants: this.state.remoteParticipants.map(participant => {
        if (participant.peerId !== peerId && participant.userId !== peerId) {
          return participant;
        }
        if (kind === "audio") {
          return {...participant, audioEnabled: enabled, speaking: enabled};
        }
        if (kind === "video") {
          return {...participant, videoEnabled: enabled};
        }
        return participant;
      }),
    });
  }

  private applyHandRaiseState(peerId: string | undefined, raised: boolean, username?: string) {
    if (!peerId) return;
    const existing = this.state.remoteParticipants.find(participant => participant.peerId === peerId || participant.userId === peerId);
    if (!existing && raised) {
      this.upsertRemoteParticipant({
        peerId,
        userId: peerId,
        username: username || "参会者",
        role: "Member",
        roleLabel: "成员",
        status: "Online",
        statusLabel: "在线",
        isLocal: false,
        audioEnabled: true,
        videoEnabled: true,
        handRaised: raised,
        speaking: false,
      });
      return;
    }
    this.setState({
      remoteParticipants: this.state.remoteParticipants.map(participant =>
        participant.peerId === peerId || participant.userId === peerId
          ? {...participant, handRaised: raised}
          : participant,
      ),
    });
  }

  private removeRemoteProducer(producerId?: string, peerId?: string) {
    if (!producerId) return;
    const producer = this.remoteProducers.get(producerId);
    this.remoteProducers.delete(producerId);
    this.consumedProducerIds.delete(producerId);
    const closedConsumers = this.consumers.filter(consumer => consumer.producerId === producerId);
    closedConsumers.forEach(consumer => consumer.close());
    this.consumers = this.consumers.filter(consumer => consumer.producerId !== producerId);
    const targetPeerId = peerId || producer?.peerId || producer?.userId;
    if (targetPeerId && producer?.kind) {
      this.applyProducerState(targetPeerId, producer.kind, false);
    }
  }

  private upsertRemoteParticipant(participant: RoomParticipant) {
    if (!participant.peerId || participant.userId === this.currentUserId || participant.peerId === this.currentUserId) {
      return;
    }
    const existing = this.state.remoteParticipants.find(item => item.peerId === participant.peerId || item.userId === participant.userId);
    this.setState({
      remoteParticipants: existing
        ? this.state.remoteParticipants.map(item =>
            item.peerId === existing.peerId || item.userId === existing.userId
              ? {
                  ...item,
                  ...participant,
                  audioEnabled: item.audioEnabled || participant.audioEnabled,
                  videoEnabled: item.videoEnabled || participant.videoEnabled,
                }
              : item,
          )
        : [...this.state.remoteParticipants, participant],
    });
  }

  private async consumeRemoteProducers(remoteProducers: SfuProducerState[]) {
    if (!this.recvTransport || !this.device || !this.socket) return;
    for (const producer of remoteProducers) {
      if (!producer.id) {
        continue;
      }
      this.remoteProducers.set(producer.id, producer);
      if (this.consumedProducerIds.has(producer.id)) {
        continue;
      }

      const response = await this.emitAck("consume", {
        roomId: this.roomId,
        producerId: producer.id,
        rtpCapabilities: this.device.rtpCapabilities,
      });
      if (!response?.id || !response?.rtpParameters) continue;

      const consumer = await this.recvTransport.consume({
        id: response.id,
        producerId: response.producerId ?? producer.id,
        kind: response.kind ?? producer.kind,
        rtpParameters: response.rtpParameters,
      });
      this.consumers.push(consumer);
      this.monitorConsumerScore(consumer);
      this.consumedProducerIds.add(producer.id);
      await this.emitAck("resumeConsumer", {
        roomId: this.roomId,
        consumerId: consumer.id,
      }).catch(() => undefined);
      const peerId = producer.peerId || producer.userId || producer.id;
      let stream = this.state.remoteStreams[peerId];
      if (stream) {
        stream.addTrack(consumer.track as any);
      } else {
        stream = new MediaStream([consumer.track as any]);
      }
      this.setState({
        remoteStreams: {
          ...this.state.remoteStreams,
          [peerId]: stream,
        },
      });
    }
  }

  private monitorProducerScore(producer: mediasoupTypes.Producer) {
    (producer as any).on("score", (scoreArray: any) => {
      const score = Array.isArray(scoreArray) && scoreArray.length > 0 ? scoreArray[0].score : (scoreArray as any)?.score;
      this.updateNetworkQuality(score);
    });
  }

  private monitorConsumerScore(consumer: mediasoupTypes.Consumer) {
    (consumer as any).on("score", (score: any) => {
      this.updateNetworkQuality((score as any)?.score);
    });
  }

  private updateNetworkQuality(score?: number) {
    if (typeof score !== "number") return;
    let quality = "good";
    let label = "流畅";
    if (score >= 8) {
      quality = "excellent";
      label = "极佳";
    } else if (score < 5) {
      quality = "poor";
      label = "较差";
    }
    this.setState({ networkQuality: quality, networkQualityLabel: label });
  }

  private emitAck(event: string, payload: Record<string, unknown>): Promise<any> {
    return new Promise((resolve, reject) => {
      if (!this.socket) {
        reject(new Error("Socket 未连接"));
        return;
      }
      this.socket.timeout(8000).emit(event, payload, (error: Error | null, response: any) => {
        if (error) {
          reject(error);
          return;
        }
        if (response?.error) {
          reject(new Error(response.error));
          return;
        }
        resolve(response);
      });
    });
  }

  private waitForSocket(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.socket) {
        reject(new Error("Socket 未初始化"));
        return;
      }
      if (this.socket.connected) {
        resolve();
        return;
      }
      this.socket.once("connect", () => resolve());
      this.socket.once("connect_error", reject);
    });
  }

  private async createSfuTransport(producing: boolean, consuming: boolean): Promise<SfuTransportState> {
    const response = await this.emitAck("createWebRtcTransport", {
      roomId: this.roomId,
      producing,
      consuming,
    });
    return {
      id: response?.id ?? "",
      direction: producing ? "Send" : "Recv",
      connected: false,
      iceParametersJson: JSON.stringify(response?.iceParameters ?? {}),
      iceCandidatesJson: JSON.stringify(response?.iceCandidates ?? []),
      dtlsParametersJson: JSON.stringify(response?.dtlsParameters ?? {}),
      sctpParametersJson: JSON.stringify(response?.sctpParameters ?? {}),
    };
  }

  private setState(next: Partial<MeetingRoomClientState>) {
    this.state = {...this.state, ...next};
    this.listener?.(this.state);
  }
}

export {RTCView};

function toTransportOptions(transport: SfuTransportState) {
  return {
    id: transport.id,
    iceParameters: parseJson(transport.iceParametersJson),
    iceCandidates: parseJson(transport.iceCandidatesJson) ?? [],
    dtlsParameters: parseJson(transport.dtlsParametersJson),
    sctpParameters: parseJson(transport.sctpParametersJson),
  };
}

function parseJson(value?: string) {
  if (!value) return undefined;
  try {
    return JSON.parse(value);
  } catch {
    return undefined;
  }
}

function parseJoinRoomProducers(response: any): SfuProducerState[] {
  const peers = Array.isArray(response?.peers) ? response.peers : [];
  return peers.flatMap((peer: any) => {
    const producers = Array.isArray(peer?.producers) ? peer.producers : [];
    return producers.map((producer: any) =>
      normalizeProducer({
        ...producer,
        peerId: peer?.peerId,
        userId: peer?.userId,
        username: peer?.username,
      }),
    );
  });
}

function parseJoinRoomParticipants(response: any): RoomParticipant[] {
  const peers = Array.isArray(response?.peers) ? response.peers : [];
  return peers.map((peer: any) => toParticipant(peer));
}

function normalizeProducer(producer: any): SfuProducerState {
  return {
    id: producer?.id ?? producer?.producerId ?? "",
    peerId: producer?.peerId ?? "",
    userId: producer?.userId ?? "",
    username: producer?.username ?? "",
    kind: producer?.kind ?? "",
    paused: !!producer?.paused,
    local: false,
  };
}

function toParticipant(peer: any, producerKind?: string): RoomParticipant {
  return {
    peerId: peer?.peerId ?? peer?.id ?? peer?.userId ?? "",
    userId: peer?.userId ?? "",
    username: peer?.username ?? peer?.name ?? "参会者",
    role: "Member",
    roleLabel: "成员",
    status: "Online",
    statusLabel: "在线",
    isLocal: false,
    audioEnabled: producerKind ? producerKind === "audio" : true,
    videoEnabled: producerKind ? producerKind === "video" : true,
    handRaised: false,
    speaking: producerKind === "audio",
  };
}
