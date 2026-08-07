import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {
  Alert,
  DeviceEventEmitter,
  Modal,
  NativeModules,
  Pressable,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  useWindowDimensions,
  Vibration,
  View,
} from "react-native";
import InCallManager from "react-native-incall-manager";
import {MeetingRoomClient, RTCView, MeetingRoomClientState} from "./mediasoup/MeetingRoomClient";
import type {RoomChatMessage, RoomParticipant, RoomState} from "./types";
import type {MediaStream} from "react-native-webrtc";
import {colors, rgba} from "./theme";

type Props = {
  roomStateJson?: string;
  onAction?: (action: string, payload?: Record<string, unknown>) => void;
};

const {MeetingRoomBridge} = NativeModules;

const EMPTY_ROOM: RoomState = {
  participants: [],
  chatMessages: [],
  networkQuality: "Unknown",
  networkQualityLabel: "检测中",
  socketConnected: false,
  reconnecting: false,
  audioEnabled: true,
  videoEnabled: true,
  participantsLoading: false,
  participantsTotal: 0,
  handRaised: false,
  screenSharing: false,
  captionsEnabled: false,
  mediaState: {
    phase: "Idle",
    phaseLabel: "未开始",
    routerRtpCapabilitiesJson: "",
    localProducers: [],
    remoteProducers: [],
    consumers: [],
    mediaEngineReady: false,
  },
  availableAudioRoutes: ["Speaker"],
  selectedAudioRoute: "Speaker",
};

function MeetingRoom({roomStateJson, onAction}: Props): React.JSX.Element {
  const [roomState, setRoomState] = useState(() => parseRoomState(roomStateJson));
  const [clientState, setClientState] = useState<MeetingRoomClientState>({
    phase: "idle",
    remoteStreams: {},
    remoteParticipants: [],
    chatMessages: [],
    consumerLayers: {},
  });
  const [messageDraft, setMessageDraft] = useState("");
  const [spotlightPeerId, setSpotlightPeerId] = useState<string>();
  const [effectType, setEffectType] = useState<"none" | "blur" | "replace">("none");
  const [virtualBackground, setVirtualBackground] = useState("office");
  const [keepScreenAwake, setKeepScreenAwake] = useState(true);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [moreTab, setMoreTab] = useState<"actions" | "background" | "audio" | "android">("actions");
  const [roomControls, setRoomControls] = useState({
    audioEnabled: roomState.audioEnabled,
    videoEnabled: roomState.videoEnabled,
    handRaised: roomState.handRaised,
    screenSharing: roomState.screenSharing,
    captionsEnabled: roomState.captionsEnabled,
    selectedSheet: roomState.selectedSheet,
  });
  const {width} = useWindowDimensions();
  const client = useRef(new MeetingRoomClient()).current;
  const meetingStartedAt = useRef(Date.now()).current;
  const performAction = useCallback((action: string, payload?: Record<string, unknown>) => {
    onAction?.(action, payload);
    perform(action, payload);
  }, [onAction]);

  useEffect(() => {
    const subscription = DeviceEventEmitter.addListener(
      "BlueSkyMeetingRoomState",
      payload => setRoomState(parseRoomState(payload?.roomStateJson)),
    );
    return () => subscription.remove();
  }, []);

  useEffect(() => {
    setRoomState(parseRoomState(roomStateJson));
  }, [roomStateJson]);

  useEffect(() => {
    client.subscribe(setClientState);
    MeetingRoomBridge?.setKeepScreenOn?.(true);
    InCallManager.start({media: "video"});
    return () => {
      client.close();
      MeetingRoomBridge?.setKeepScreenOn?.(false);
      InCallManager.stop();
    };
  }, [client]);

  useEffect(() => {
    MeetingRoomBridge?.setKeepScreenOn?.(keepScreenAwake);
  }, [keepScreenAwake]);

  useEffect(() => {
    performAction("setVideoEffect", {type: effectType, background: virtualBackground});
  }, [effectType, virtualBackground, performAction]);

  useEffect(() => {
    const timer = setInterval(() => {
      setElapsedSeconds(Math.floor((Date.now() - meetingStartedAt) / 1000));
    }, 1000);
    return () => clearInterval(timer);
  }, [meetingStartedAt]);

  useEffect(() => {
    if (clientState.phase === "removed" || clientState.phase === "closed") {
      Alert.alert("提示", clientState.message || "会议已结束", [
        {
          text: "确定",
          onPress: async () => {
            await client.close();
            performAction("leaveRoom");
          },
        },
      ]);
    }
  }, [clientState.phase, clientState.message, client, performAction]);

  useEffect(() => {
    client.update({
      ...roomState,
      audioEnabled: roomControls.audioEnabled,
      videoEnabled: roomControls.videoEnabled,
      handRaised: roomControls.handRaised,
      screenSharing: roomControls.screenSharing,
      captionsEnabled: roomControls.captionsEnabled,
      selectedSheet: roomControls.selectedSheet,
    });
  }, [client, roomState, roomControls]);

  useEffect(() => {
    setRoomControls(prev => ({
      ...prev,
      audioEnabled: clientState.localAudioEnabled ?? prev.audioEnabled,
      videoEnabled: clientState.localVideoEnabled ?? prev.videoEnabled,
    }));
  }, [clientState.localAudioEnabled, clientState.localVideoEnabled]);

  const uiRoomState = useMemo(
    () => ({
      ...roomState,
      audioEnabled: clientState.localAudioEnabled ?? roomControls.audioEnabled,
      videoEnabled: clientState.localVideoEnabled ?? roomControls.videoEnabled,
      handRaised: roomControls.handRaised,
      screenSharing: roomControls.screenSharing,
      captionsEnabled: roomControls.captionsEnabled,
      selectedSheet: roomControls.selectedSheet,
    }),
    [roomState, roomControls, clientState.localAudioEnabled, clientState.localVideoEnabled],
  );
  const isHost = uiRoomState.meeting?.hostId === uiRoomState.currentUserId;
  const participants = useMemo<RoomParticipant[]>(
    () => {
      const localFallback: RoomParticipant = {
        peerId: "local",
        userId: "",
        username: "我",
        role: "Member",
        roleLabel: "成员",
        status: "Online",
        statusLabel: "在线",
        isLocal: true,
        audioEnabled: uiRoomState.audioEnabled,
        videoEnabled: uiRoomState.videoEnabled,
        handRaised: uiRoomState.handRaised,
        speaking: false,
      };
      const base = roomState.participants.length > 0 ? roomState.participants : [localFallback];
      const byPeerId = new Map(base.map(participant => [participant.peerId, participant]));
      clientState.remoteParticipants.forEach(participant => {
        const existing = byPeerId.get(participant.peerId);
        byPeerId.set(participant.peerId, existing ? {...existing, ...participant} : participant);
      });
      return Array.from(byPeerId.values()).map(participant =>
        participant.isLocal
          ? {
              ...participant,
              audioEnabled: uiRoomState.audioEnabled,
              videoEnabled: uiRoomState.videoEnabled,
              handRaised: uiRoomState.handRaised,
            }
          : participant,
      );
    },
    [roomState.participants, clientState.remoteParticipants, uiRoomState],
  );
  const chatMessages = useMemo(
    () => [...roomState.chatMessages, ...clientState.chatMessages],
    [roomState.chatMessages, clientState.chatMessages],
  );
  const activeSpeaker =
    participants.find(item => item.peerId === clientState.globalSpotlightPeerId) ??
    participants.find(item => item.peerId === spotlightPeerId) ??
    participants.find(item => item.peerId === uiRoomState.activeSpeakerPeerId) ??
    participants[0];
  const raisedHands = participants.filter(item => item.handRaised);
  const isCompact = width < 600;
  const act = async (action: string, payload?: Record<string, unknown>) => {
    Vibration.vibrate(8);
    if (action === "leaveRoom") {
      Alert.alert("离开会议", "确定要离开当前会议吗？", [
        {text: "取消", style: "cancel"},
        {
          text: "离开",
          style: "destructive",
          onPress: async () => {
            await client.close();
            perform("leaveRoom");
          },
        },
      ]);
      return;
    }
    if (action === "closeMeeting") {
      await client.closeRoom().catch(() => undefined);
      client.close();
      perform("closeMeeting");
      return;
    }
    if (action === "toggleAudio") {
      const next = !uiRoomState.audioEnabled;
      setRoomControls(prev => ({...prev, audioEnabled: next}));
      await client.toggleAudio(next);
      performAction(action, {enabled: next});
      return;
    }
    if (action === "toggleVideo") {
      const next = !uiRoomState.videoEnabled;
      setRoomControls(prev => ({...prev, videoEnabled: next}));
      await client.toggleVideo(next);
      performAction(action, {enabled: next});
      return;
    }
    if (action === "toggleHandRaised") {
      const next = !uiRoomState.handRaised;
      setRoomControls(prev => ({...prev, handRaised: next}));
      await client.toggleHandRaised(next);
      performAction(action, {enabled: next});
      return;
    }
    if (action === "toggleScreenShare") {
      const next = !uiRoomState.screenSharing;
      setRoomControls(prev => ({...prev, screenSharing: next}));
      performAction(action, payload);
      return;
    }
    if (action === "toggleCaptions") {
      const next = !uiRoomState.captionsEnabled;
      setRoomControls(prev => ({...prev, captionsEnabled: next}));
      performAction(action, payload);
      return;
    }
    if (action === "switchCamera") {
      await client.switchCamera();
      return;
    }
    if (action === "sendMessage") {
      const content = String(payload?.content || "").trim();
      if (content) {
        await client.sendMessage(content);
      }
      return;
    }
    if (action === "hostToggleParticipantAudio" || action === "hostToggleParticipantVideo" || action === "removeParticipant") {
      const participant = findParticipant(payload, participants);
      if (!participant) return;
      if (action === "hostToggleParticipantAudio") {
        await client.hostToggleParticipantAudio(participant);
      } else if (action === "hostToggleParticipantVideo") {
        await client.hostToggleParticipantVideo(participant);
      } else {
        await client.removeParticipant(participant);
      }
      return;
    }
    if (action === "setSpotlight") {
      const participant = findParticipant(payload, participants);
      if (!participant) return;
      setSpotlightPeerId(participant.peerId);
      await client.setSpotlight(participant.peerId, true);
      return;
    }
    if (action === "clearSpotlight") {
      setSpotlightPeerId(undefined);
      await client.setSpotlight(null, false);
      return;
    }
    if (action === "requestSpotlight") {
      await client.requestSpotlight();
      return;
    }
    if (action === "muteAll") {
      await client.muteAll();
      return;
    }
    if (action === "disableAllVideo") {
      await client.disableAllVideo();
      return;
    }
    if (action === "openSheet") {
      setRoomControls(prev => ({...prev, selectedSheet: (payload?.sheet as RoomState["selectedSheet"]) || undefined}));
      performAction(action, payload);
      return;
    }
    if (action === "closeSheet") {
      setRoomControls(prev => ({...prev, selectedSheet: undefined}));
      performAction(action, payload);
      return;
    }
    performAction(action, payload);
  };

  return (
    <SafeAreaView style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={colors.backgroundDark} />
      <View style={styles.stage}>
        <RemoteAudioStreams remoteStreams={clientState.remoteStreams} />
        <VideoTile
          participant={activeSpeaker}
          stream={activeSpeaker.isLocal ? clientState.localStream : clientState.remoteStreams[activeSpeaker.peerId]}
          prominent
        />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={[styles.filmstrip, isCompact && styles.compactFilmstrip]}>
          {participants
            .filter(item => item.peerId !== activeSpeaker.peerId)
            .slice(0, 8)
            .map(participant => (
              <VideoTile
                key={participant.peerId}
                participant={participant}
                stream={participant.isLocal ? clientState.localStream : clientState.remoteStreams[participant.peerId]}
                onPress={() => setSpotlightPeerId(participant.peerId)}
              />
            ))}
        </ScrollView>
      </View>

      <View style={styles.topBar}>
        <View style={styles.titleBlock}>
          <Text style={styles.title} numberOfLines={1}>
            {uiRoomState.meeting?.title || "会议房间"}
          </Text>
          <Text style={styles.caption} numberOfLines={1}>
            {formatDuration(elapsedSeconds)} · {uiRoomState.mediaState.phaseLabel} · {clientState.phase === "failed" ? clientState.message : (clientState.networkQualityLabel || uiRoomState.networkQualityLabel)} {clientState.rttMillis ? `(${clientState.rttMillis}ms)` : ""}
          </Text>
        </View>
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{participants.length} 人</Text>
        </View>
      </View>

      {raisedHands.length > 0 && (
        <Pressable style={styles.handBanner} onPress={() => act("openSheet", {sheet: "Members"})}>
          <Text style={styles.handText} numberOfLines={1}>
            {raisedHands[0].username} 举手了{raisedHands.length > 1 ? `，还有 ${raisedHands.length - 1} 人` : ""}
          </Text>
        </Pressable>
      )}

      <View style={styles.controls}>
        <Control label={uiRoomState.audioEnabled ? "静音" : "开麦"} onPress={() => act("toggleAudio")} />
        <Control label={uiRoomState.videoEnabled ? "关视频" : "开视频"} onPress={() => act("toggleVideo")} />
        <Control label="挂断" danger onPress={() => act("leaveRoom")} />
        <Control label="成员" onPress={() => act("openSheet", {sheet: "Members"})} />
        <Control label="聊天" onPress={() => act("openSheet", {sheet: "Chat"})} />
        <Control label="更多" onPress={() => act("openSheet", {sheet: "More"})} />
      </View>

      <Modal visible={!!uiRoomState.selectedSheet} animationType="slide" transparent onRequestClose={() => act("closeSheet")}>
        <Pressable style={styles.scrim} onPress={() => act("closeSheet")} />
        <View style={styles.sheet}>
          <SheetTabs selected={uiRoomState.selectedSheet} onSelect={sheet => act("openSheet", {sheet})} />
          {uiRoomState.selectedSheet === "Members" && (
            <Members
              participants={participants}
              isHost={isHost}
              onHostToggleParticipantAudio={participant => act("hostToggleParticipantAudio", participant)}
              onHostToggleParticipantVideo={participant => act("hostToggleParticipantVideo", participant)}
              onRemoveParticipant={participant => act("removeParticipant", participant)}
              onMuteAll={() => act("muteAll")}
              onDisableAllVideo={() => act("disableAllVideo")}
              onSetSpotlight={participant => act("setSpotlight", participant)}
              onClearSpotlight={() => act("clearSpotlight")}
            />
          )}
          {uiRoomState.selectedSheet === "Chat" && (
            <Chat
              messages={chatMessages}
              draft={messageDraft}
              onDraftChanged={setMessageDraft}
              onSend={() => {
                if (messageDraft.trim()) {
                  act("sendMessage", {content: messageDraft.trim()});
                  setMessageDraft("");
                }
              }}
            />
          )}
          {uiRoomState.selectedSheet === "More" && (
            <More
              roomState={uiRoomState}
              tab={moreTab}
              onTabChanged={setMoreTab}
              effectType={effectType}
              onEffectTypeChanged={setEffectType}
              virtualBackground={virtualBackground}
              onVirtualBackgroundChanged={setVirtualBackground}
              keepScreenAwake={keepScreenAwake}
              onKeepScreenAwakeChanged={setKeepScreenAwake}
              onToggleHandRaised={() => act("toggleHandRaised")}
              onToggleScreenShare={() => act("toggleScreenShare")}
              onSwitchCamera={() => act("switchCamera")}
              onToggleCaptions={() => act("toggleCaptions")}
              onCloseMeeting={() => act("closeMeeting")}
              onRequestSpotlight={() => act("requestSpotlight")}
              onPerform={performAction}
            />
          )}
        </View>
      </Modal>
    </SafeAreaView>
  );
}

function VideoTile({participant, stream, prominent = false, onPress}: {participant: RoomParticipant; stream?: MediaStream; prominent?: boolean; onPress?: () => void}) {
  return (
    <Pressable 
      android_ripple={{color: 'rgba(255,255,255,0.15)'}}
      style={[styles.tile, prominent ? styles.prominentTile : styles.smallTile]} 
      onPress={onPress}
    >
      {participant.videoEnabled && stream ? (
        // @ts-ignore
        <RTCView stream={stream} objectFit="cover" style={StyleSheet.absoluteFill} />
      ) : (
        <View style={styles.avatarWrap}>
          <View style={[styles.avatar, prominent && styles.prominentAvatar]}>
            <Text style={styles.avatarText}>{participant.username.slice(0, 1) || "会"}</Text>
          </View>
          <Text style={styles.placeholder}>{participant.videoEnabled ? "等待视频流" : "摄像头已关闭"}</Text>
        </View>
      )}
      <View style={styles.tileFooter}>
        <Text style={styles.tileName} numberOfLines={1}>
          {participant.isLocal ? `${participant.username}（我）` : participant.username}
        </Text>
        <Text style={styles.tileIcon}>{participant.audioEnabled ? "Mic" : "Muted"}</Text>
      </View>
    </Pressable>
  );
}

function RemoteAudioStreams({remoteStreams}: {remoteStreams: Record<string, MediaStream>}) {
  return (
    <View pointerEvents="none" style={styles.hiddenAudioViews}>
      {Object.entries(remoteStreams)
        .filter(([, stream]) => stream.getAudioTracks().length > 0 && stream.getVideoTracks().length === 0)
        .map(([peerId, stream]) => (
          // @ts-ignore
          <RTCView key={`audio-${peerId}`} stream={stream} style={styles.hiddenAudioView} />
        ))}
    </View>
  );
}

function SheetTabs({selected, onSelect}: {selected?: string; onSelect: (sheet: string) => void}) {
  return (
    <View style={styles.sheetTabs}>
      {[
        ["Members", "成员"],
        ["Chat", "聊天"],
        ["More", "更多"],
      ].map(([sheet, label]) => (
        <Pressable 
          key={sheet} 
          android_ripple={{color: selected === sheet ? rgba(colors.primary, 0.2) : 'rgba(0,0,0,0.1)'}}
          style={[styles.sheetTab, selected === sheet && styles.sheetTabActive]} 
          onPress={() => onSelect(sheet)}
        >
          <Text style={[styles.sheetTabText, selected === sheet && styles.sheetTabTextActive]}>{label}</Text>
        </Pressable>
      ))}
    </View>
  );
}

function Members({
  participants,
  isHost,
  onHostToggleParticipantAudio,
  onHostToggleParticipantVideo,
  onRemoveParticipant,
  onMuteAll,
  onDisableAllVideo,
  onSetSpotlight,
  onClearSpotlight,
}: {
  participants: RoomParticipant[];
  isHost: boolean;
  onHostToggleParticipantAudio: (participant: RoomParticipant) => void;
  onHostToggleParticipantVideo: (participant: RoomParticipant) => void;
  onRemoveParticipant: (participant: RoomParticipant) => void;
  onMuteAll: () => void;
  onDisableAllVideo: () => void;
  onSetSpotlight: (participant: RoomParticipant) => void;
  onClearSpotlight: () => void;
}) {
  return (
    <ScrollView>
      <View style={styles.sheetHeaderWithActions}>
        <Text style={[styles.sheetTitle, styles.sheetTitleNoMargin]}>成员</Text>
        {isHost && (
          <View style={styles.rowActions}>
            <Control compact label="全体静音" onPress={onMuteAll} />
            <Control compact label="全体关视频" onPress={onDisableAllVideo} />
            <Control compact label="取消焦点" onPress={onClearSpotlight} />
          </View>
        )}
      </View>
      {participants.map(participant => (
        <View key={participant.peerId} style={styles.memberRow}>
          <Text style={styles.memberName}>{participant.username}</Text>
          <Text style={styles.memberMeta}>{participant.roleLabel} · {participant.statusLabel}</Text>
          {!participant.isLocal && (
            <View style={styles.rowActions}>
              <Control compact label={participant.audioEnabled ? "静音" : "开麦"} onPress={() => onHostToggleParticipantAudio(participant)} />
              <Control compact label={participant.videoEnabled ? "关视频" : "开视频"} onPress={() => onHostToggleParticipantVideo(participant)} />
              <Control compact label="设为焦点" onPress={() => onSetSpotlight(participant)} />
              <Control compact danger label="移出" onPress={() => onRemoveParticipant(participant)} />
            </View>
          )}
        </View>
      ))}
    </ScrollView>
  );
}

function Chat({messages, draft, onDraftChanged, onSend}: {messages: RoomChatMessage[]; draft: string; onDraftChanged: (value: string) => void; onSend: () => void}) {
  return (
    <View>
      <Text style={styles.sheetTitle}>聊天</Text>
      <View style={styles.rowActions}>
        <Control compact label="保存记录" onPress={() => Alert.alert("聊天记录", `当前可保存 ${messages.length} 条消息`)} />
        <Control compact label="清空本地" onPress={() => Alert.alert("清空聊天", "移动端仅清空当前本地展示，服务端记录不受影响。")} />
      </View>
      <ScrollView style={styles.chatList}>
        {messages.slice(-40).map(message => (
          <Text key={message.id} style={styles.chatLine}>{message.senderName}: {message.content}</Text>
        ))}
      </ScrollView>
      <View style={styles.inputRow}>
        <TextInput value={draft} onChangeText={onDraftChanged} placeholder="输入消息" placeholderTextColor="#6b7280" style={styles.input} />
        <Control compact label="发送" onPress={onSend} />
      </View>
    </View>
  );
}

function More({
  roomState,
  tab,
  onTabChanged,
  effectType,
  onEffectTypeChanged,
  virtualBackground,
  onVirtualBackgroundChanged,
  keepScreenAwake,
  onKeepScreenAwakeChanged,
  onToggleHandRaised,
  onToggleScreenShare,
  onSwitchCamera,
  onToggleCaptions,
  onCloseMeeting,
  onRequestSpotlight,
  onPerform,
}: {
  roomState: RoomState;
  tab: "actions" | "background" | "audio" | "android";
  onTabChanged: (tab: "actions" | "background" | "audio" | "android") => void;
  effectType: "none" | "blur" | "replace";
  onEffectTypeChanged: (effect: "none" | "blur" | "replace") => void;
  virtualBackground: string;
  onVirtualBackgroundChanged: (background: string) => void;
  keepScreenAwake: boolean;
  onKeepScreenAwakeChanged: (enabled: boolean) => void;
  onToggleHandRaised: () => void;
  onToggleScreenShare: () => void;
  onSwitchCamera: () => void;
  onToggleCaptions: () => void;
  onCloseMeeting: () => void;
  onRequestSpotlight: () => void;
  onPerform: (action: string, payload?: Record<string, unknown>) => void;
}) {
  return (
    <View>
      <Text style={styles.sheetTitle}>更多</Text>
      <View style={styles.segmented}>
        {[
          ["actions", "操作"],
          ["background", "背景"],
          ["audio", "音频"],
          ["android", "Android"],
        ].map(([key, label]) => (
          <Pressable key={key} style={[styles.segment, tab === key && styles.segmentActive]} onPress={() => onTabChanged(key as any)}>
            <Text style={[styles.segmentText, tab === key && styles.segmentTextActive]}>{label}</Text>
          </Pressable>
        ))}
      </View>
      {tab === "actions" && (
        <View style={styles.moreGrid}>
          <Control label={roomState.screenSharing ? "停止共享" : "屏幕共享"} onPress={onToggleScreenShare} />
          <Control label={roomState.handRaised ? "取消举手" : "举手"} onPress={onToggleHandRaised} />
          <Control label="切换摄像头" onPress={onSwitchCamera} />
          <Control label={roomState.captionsEnabled ? "关闭字幕" : "字幕"} onPress={onToggleCaptions} />
          <Control label="申请焦点" onPress={onRequestSpotlight} />
          <Control label="设置" onPress={() => onPerform("openMeetingSettings")} />
          <Control danger label="关闭会议" onPress={onCloseMeeting} />
        </View>
      )}
      {tab === "background" && (
        <View>
          <View style={styles.moreGrid}>
            <Choice label="无效果" active={effectType === "none"} onPress={() => onEffectTypeChanged("none")} />
            <Choice label="背景虚化" active={effectType === "blur"} onPress={() => onEffectTypeChanged("blur")} />
            <Choice label="虚拟背景" active={effectType === "replace"} onPress={() => onEffectTypeChanged("replace")} />
          </View>
          <View style={styles.backgroundGrid}>
            {["office", "classroom", "blue", "gray"].map(item => (
              <Choice key={item} label={backgroundLabel(item)} active={virtualBackground === item} onPress={() => onVirtualBackgroundChanged(item)} />
            ))}
          </View>
          <Text style={styles.note}>背景特效将由 RN 媒体层接入 Rust/Wasm 或原生图像处理后应用到发布轨道。</Text>
        </View>
      )}
      {tab === "audio" && (
        <View>
          <View style={styles.moreGrid}>
            {roomState.availableAudioRoutes.map(route => (
              <Choice key={route} label={route === "Speaker" ? "扬声器" : "听筒"} active={roomState.selectedAudioRoute === route} onPress={() => onPerform("selectAudioRoute", {route})} />
            ))}
          </View>
          <Choice label="智能降噪" active onPress={() => Alert.alert("智能降噪", "移动端音频处理入口已预留，可接入 RN 音频处理模块。")} />
        </View>
      )}
      {tab === "android" && (
        <View style={styles.moreGrid}>
          <Choice label={keepScreenAwake ? "关闭常亮" : "屏幕常亮"} active={keepScreenAwake} onPress={() => onKeepScreenAwakeChanged(!keepScreenAwake)} />
          <Choice label="画中画" active={false} onPress={() => MeetingRoomBridge?.enterPictureInPicture?.()} />
          <Choice label="听筒优化" active={roomState.selectedAudioRoute === "Earpiece"} onPress={() => onPerform("selectAudioRoute", {route: "Earpiece"})} />
        </View>
      )}
    </View>
  );
}

function Choice({label, active, onPress}: {label: string; active: boolean; onPress: () => void}) {
  return (
    <Pressable 
      android_ripple={{color: active ? rgba(colors.primary, 0.2) : 'rgba(0,0,0,0.1)'}}
      style={[styles.choice, active && styles.choiceActive]} 
      onPress={onPress}
    >
      <Text style={[styles.choiceText, active && styles.choiceTextActive]} numberOfLines={1}>{label}</Text>
    </Pressable>
  );
}

function Control({label, danger = false, compact = false, onPress}: {label: string; danger?: boolean; compact?: boolean; onPress: () => void}) {
  return (
    <Pressable 
      android_ripple={{color: danger ? rgba(colors.error, 0.2) : 'rgba(0,0,0,0.1)'}}
      style={[styles.control, danger && styles.dangerControl, compact && styles.compactControl]} 
      onPress={onPress}
    >
      <Text style={[styles.controlText, danger && styles.dangerText]} numberOfLines={1}>{label}</Text>
    </Pressable>
  );
}

function perform(action: string, payload?: Record<string, unknown>) {
  MeetingRoomBridge?.perform?.(action, payload ?? {});
}

function findParticipant(payload: Record<string, unknown> | undefined, participants: RoomParticipant[]) {
  if (!payload) return undefined;
  const peerId = typeof payload.peerId === "string" ? payload.peerId : "";
  const userId = typeof payload.userId === "string" ? payload.userId : "";
  return participants.find(participant =>
    (peerId && participant.peerId === peerId) || (userId && participant.userId === userId),
  );
}

function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const remainSeconds = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainSeconds).padStart(2, "0")}`;
}

function backgroundLabel(value: string) {
  return {
    office: "办公室",
    classroom: "教室",
    blue: "蓝色",
    gray: "灰色",
  }[value] ?? value;
}

function parseRoomState(value?: string): RoomState {
  if (!value) return EMPTY_ROOM;
  try {
    return {...EMPTY_ROOM, ...JSON.parse(value)};
  } catch {
    return EMPTY_ROOM;
  }
}

const styles = StyleSheet.create({
  root: {flex: 1, backgroundColor: colors.backgroundDark},
  stage: {flex: 1, padding: 16, paddingBottom: 112, gap: 12},
  hiddenAudioViews: {position: "absolute", width: 1, height: 1, opacity: 0},
  hiddenAudioView: {width: 1, height: 1},
  prominentTile: {flex: 1},
  smallTile: {width: 150, height: 112, marginRight: 12},
  tile: {borderRadius: 12, overflow: "hidden", backgroundColor: colors.surfaceDark, elevation: 2},
  avatarWrap: {flex: 1, alignItems: "center", justifyContent: "center"},
  avatar: {width: 46, height: 46, borderRadius: 23, backgroundColor: colors.primary, alignItems: "center", justifyContent: "center"},
  prominentAvatar: {width: 88, height: 88, borderRadius: 44},
  avatarText: {color: colors.textDark, fontWeight: "700", fontSize: 22},
  placeholder: {marginTop: 8, color: colors.textMutedDark, fontSize: 12},
  filmstrip: {maxHeight: 124},
  compactFilmstrip: {maxHeight: 112},
  tileFooter: {position: "absolute", left: 0, right: 0, bottom: 0, padding: 10, backgroundColor: rgba(colors.backgroundDark, 0.84), flexDirection: "row", alignItems: "center"},
  tileName: {flex: 1, color: colors.textDark, fontSize: 13, fontWeight: "600"},
  tileIcon: {color: colors.textMutedDark, fontSize: 11},
  topBar: {position: "absolute", top: 16, left: 16, right: 16, flexDirection: "row", gap: 10, alignItems: "center"},
  titleBlock: {flex: 1, borderRadius: 12, paddingHorizontal: 16, paddingVertical: 10, backgroundColor: rgba(colors.backgroundDark, 0.85), elevation: 4},
  title: {color: colors.textDark, fontWeight: "700", fontSize: 16},
  caption: {color: colors.textMutedDark, fontSize: 12, marginTop: 4},
  badge: {borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10, backgroundColor: rgba(colors.backgroundDark, 0.85), elevation: 4},
  badgeText: {color: colors.textDark, fontWeight: "600", fontSize: 12},
  handBanner: {position: "absolute", top: 76, left: 16, right: 16, borderRadius: 12, paddingHorizontal: 16, paddingVertical: 12, backgroundColor: rgba(colors.speaking, 0.95), elevation: 6},
  handText: {color: colors.textDark, fontSize: 14, fontWeight: "700"},
  controls: {position: "absolute", left: 0, right: 0, bottom: 0, padding: 16, paddingBottom: 24, backgroundColor: colors.surface, flexDirection: "row", gap: 8, elevation: 16, borderTopWidth: 1, borderTopColor: colors.surfaceVariant},
  control: {flex: 1, minHeight: 48, borderRadius: 10, borderWidth: 1, borderColor: colors.surfaceVariant, backgroundColor: colors.surface, alignItems: "center", justifyContent: "center", paddingHorizontal: 8, elevation: 1},
  compactControl: {flex: 0, minHeight: 38, paddingHorizontal: 12},
  dangerControl: {borderColor: colors.errorContainer, backgroundColor: colors.errorContainer},
  controlText: {fontSize: 13, color: colors.text, fontWeight: "700"},
  dangerText: {color: colors.error},
  scrim: {flex: 1, backgroundColor: rgba("#000000", 0.4)},
  sheet: {maxHeight: "75%", backgroundColor: colors.surface, padding: 20, borderTopLeftRadius: 16, borderTopRightRadius: 16, elevation: 24},
  sheetTabs: {flexDirection: "row", gap: 10, marginBottom: 16},
  sheetTab: {flex: 1, minHeight: 40, borderRadius: 10, borderWidth: 1, borderColor: colors.surfaceVariant, alignItems: "center", justifyContent: "center"},
  sheetTabActive: {backgroundColor: colors.primary, borderColor: colors.primary},
  sheetTabText: {fontSize: 14, fontWeight: "700", color: colors.text},
  sheetTabTextActive: {color: colors.surface},
  sheetTitle: {fontSize: 20, fontWeight: "800", color: colors.text, marginBottom: 14},
  sheetTitleNoMargin: {marginBottom: 0},
  sheetHeaderWithActions: {flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 14},
  memberRow: {paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: colors.surfaceVariant},
  memberName: {fontSize: 15, fontWeight: "700", color: colors.text},
  memberMeta: {fontSize: 12, color: colors.textMuted, marginTop: 2},
  rowActions: {flexDirection: "row", gap: 8, marginTop: 8},
  chatList: {height: 280},
  chatLine: {fontSize: 14, color: colors.text, paddingVertical: 5},
  inputRow: {flexDirection: "row", gap: 8, alignItems: "center", marginTop: 12},
  input: {flex: 1, minHeight: 42, borderWidth: 1, borderColor: colors.outline, borderRadius: 8, paddingHorizontal: 12, color: colors.text},
  moreGrid: {flexDirection: "row", flexWrap: "wrap", gap: 10},
  segmented: {flexDirection: "row", gap: 6, marginBottom: 14},
  segment: {flex: 1, minHeight: 34, borderRadius: 8, backgroundColor: colors.surfaceVariant, alignItems: "center", justifyContent: "center"},
  segmentActive: {backgroundColor: colors.primaryContainer},
  segmentText: {fontSize: 12, fontWeight: "700", color: colors.textMuted},
  segmentTextActive: {color: colors.primaryContainerDark},
  backgroundGrid: {flexDirection: "row", flexWrap: "wrap", gap: 10, marginTop: 12},
  choice: {minWidth: 94, minHeight: 44, borderRadius: 8, borderWidth: 1, borderColor: colors.outline, alignItems: "center", justifyContent: "center", paddingHorizontal: 10, backgroundColor: colors.surface},
  choiceActive: {borderColor: colors.primary, backgroundColor: colors.primaryContainer},
  choiceText: {fontSize: 13, color: colors.text, fontWeight: "700"},
  choiceTextActive: {color: colors.primaryContainerDark},
  note: {fontSize: 12, color: colors.textMuted, lineHeight: 18, marginTop: 12},
});

export default MeetingRoom;
