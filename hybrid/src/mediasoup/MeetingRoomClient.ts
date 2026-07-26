import {Device, types as mediasoupTypes} from "mediasoup-client";
import {
  mediaDevices,
  MediaStream,
  MediaStreamTrack,
  RTCView,
} from "react-native-webrtc";
import {io, Socket} from "socket.io-client";
import type {RoomState, SfuProducerState, SfuTransportState} from "../types";

export type MeetingRoomClientState = {
  phase: "idle" | "connecting" | "ready" | "failed";
  message?: string;
  localStream?: MediaStream;
  remoteStreams: Record<string, MediaStream>;
};

type Listener = (state: MeetingRoomClientState) => void;

const DEFAULT_SFU_SOCKET_URL = "http://10.0.2.2:3000";

export class MeetingRoomClient {
  private device?: mediasoupTypes.Device;
  private socket?: Socket;
  private sendTransport?: mediasoupTypes.Transport;
  private recvTransport?: mediasoupTypes.Transport;
  private roomId = "";
  private producers: mediasoupTypes.Producer[] = [];
  private consumers: mediasoupTypes.Consumer[] = [];
  private consumedProducerIds = new Set<string>();
  private state: MeetingRoomClientState = {phase: "idle", remoteStreams: {}};
  private listener?: Listener;

  subscribe(listener: Listener) {
    this.listener = listener;
    listener(this.state);
  }

  async connect(roomState: RoomState) {
    if (!roomState.meeting || this.state.phase === "connecting") {
      return;
    }

    this.setState({phase: "connecting", message: "正在初始化媒体引擎"});

    try {
      this.roomId = roomState.meeting.roomId || roomState.meeting.roomNo;
      this.socket = io(roomState.meeting.sfuServerUrl || DEFAULT_SFU_SOCKET_URL, {
        transports: ["websocket"],
        auth: {token: roomState.authToken},
      });
      await this.waitForSocket();
      const joinResponse = await this.emitAck("joinRoom", {
        roomId: this.roomId,
        userId: roomState.currentUserId,
        username: roomState.currentUsername,
        token: roomState.authToken,
        withMedia: true,
      });

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
    if (!this.device && roomState.mediaState.routerRtpCapabilitiesJson) {
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
    this.consumers.forEach(consumer => consumer.close());
    this.producers.forEach(producer => producer.close());
    this.sendTransport?.close();
    this.recvTransport?.close();
    this.socket?.disconnect();
    this.consumedProducerIds.clear();
    this.roomId = "";
    this.setState({phase: "idle", remoteStreams: {}});
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

  private async publishLocalTracks(stream: MediaStream) {
    if (!this.sendTransport) return;
    for (const track of stream.getTracks() as MediaStreamTrack[]) {
      const producer = await this.sendTransport.produce({
        track: track as any,
        appData: {source: "react-native"},
      });
      this.producers.push(producer);
    }
  }

  private bindRoomEvents() {
    this.socket?.off("newProducer");
    this.socket?.on("newProducer", producer => {
      this.consumeRemoteProducers([normalizeProducer(producer)]).catch(error => {
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
  }

  private setProducerEnabled(producer: mediasoupTypes.Producer, enabled: boolean) {
    const paused = !!(producer as any).paused;
    if (enabled && paused) {
      producer.resume();
      this.emitAck("resumeProducer", {
        roomId: this.roomId,
        producerId: producer.id,
      }).catch(() => undefined);
    }
    if (!enabled && !paused) {
      producer.pause();
      this.emitAck("pauseProducer", {
        roomId: this.roomId,
        producerId: producer.id,
      }).catch(() => undefined);
    }
  }

  private async consumeRemoteProducers(remoteProducers: SfuProducerState[]) {
    if (!this.recvTransport || !this.device || !this.socket) return;
    for (const producer of remoteProducers) {
      if (!producer.id || this.consumedProducerIds.has(producer.id)) {
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
      this.consumedProducerIds.add(producer.id);
      await this.emitAck("resumeConsumer", {
        roomId: this.roomId,
        consumerId: consumer.id,
      }).catch(() => undefined);
      const stream = new MediaStream([consumer.track as any]);
      this.setState({
        remoteStreams: {
          ...this.state.remoteStreams,
          [producer.peerId || producer.userId || producer.id]: stream,
        },
      });
    }
  }

  private emitAck(event: string, payload: Record<string, unknown>): Promise<any> {
    return new Promise((resolve, reject) => {
      if (!this.socket) {
        reject(new Error("Socket 未连接"));
        return;
      }
      this.socket.timeout(8000).emit(event, payload, (error: Error | null, response: any) => {
        error ? reject(error) : resolve(response);
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
