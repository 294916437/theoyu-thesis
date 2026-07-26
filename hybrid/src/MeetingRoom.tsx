import React, {useEffect, useMemo, useRef, useState} from "react";
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
import type {RoomParticipant, RoomState} from "./types";

type Props = {
  roomStateJson?: string;
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

function MeetingRoom({roomStateJson}: Props): React.JSX.Element {
  const [roomState, setRoomState] = useState(() => parseRoomState(roomStateJson));
  const [clientState, setClientState] = useState<MeetingRoomClientState>({
    phase: "idle",
    remoteStreams: {},
  });
  const [messageDraft, setMessageDraft] = useState("");
  const [spotlightPeerId, setSpotlightPeerId] = useState<string>();
  const [effectType, setEffectType] = useState<"none" | "blur" | "replace">("none");
  const [virtualBackground, setVirtualBackground] = useState("office");
  const [keepScreenAwake, setKeepScreenAwake] = useState(true);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [moreTab, setMoreTab] = useState<"actions" | "background" | "audio" | "android">("actions");
  const {width} = useWindowDimensions();
  const client = useRef(new MeetingRoomClient()).current;
  const meetingStartedAt = useRef(Date.now()).current;

  useEffect(() => {
    const subscription = DeviceEventEmitter.addListener(
      "BlueSkyMeetingRoomState",
      payload => setRoomState(parseRoomState(payload?.roomStateJson)),
    );
    return () => subscription.remove();
  }, []);

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
    const timer = setInterval(() => {
      setElapsedSeconds(Math.floor((Date.now() - meetingStartedAt) / 1000));
    }, 1000);
    return () => clearInterval(timer);
  }, [meetingStartedAt]);

  useEffect(() => {
    client.update(roomState);
  }, [client, roomState]);

  const participants = useMemo<RoomParticipant[]>(
    () =>
      roomState.participants.length > 0
        ? roomState.participants
        : [{
            peerId: "local",
            userId: "",
            username: "我",
            role: "Member",
            roleLabel: "成员",
            status: "Online",
            statusLabel: "在线",
            isLocal: true,
            audioEnabled: roomState.audioEnabled,
            videoEnabled: roomState.videoEnabled,
            handRaised: false,
            speaking: false,
          }],
    [roomState],
  );
  const activeSpeaker =
    participants.find(item => item.peerId === spotlightPeerId) ??
    participants.find(item => item.peerId === roomState.activeSpeakerPeerId) ??
    participants[0];
  const raisedHands = participants.filter(item => item.handRaised);
  const isCompact = width < 600;
  const act = (action: string, payload?: Record<string, unknown>) => {
    Vibration.vibrate(8);
    if (action === "leaveRoom") {
      Alert.alert("离开会议", "确定要离开当前会议吗？", [
        {text: "取消", style: "cancel"},
        {text: "离开", style: "destructive", onPress: () => perform("leaveRoom")},
      ]);
      return;
    }
    perform(action, payload);
  };

  return (
    <SafeAreaView style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor="#111827" />
      <View style={styles.stage}>
        <VideoTile
          participant={activeSpeaker}
          streamUrl={activeSpeaker.isLocal ? clientState.localStream?.toURL() : clientState.remoteStreams[activeSpeaker.peerId]?.toURL()}
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
                streamUrl={participant.isLocal ? clientState.localStream?.toURL() : clientState.remoteStreams[participant.peerId]?.toURL()}
                onPress={() => setSpotlightPeerId(participant.peerId)}
              />
            ))}
        </ScrollView>
      </View>

      <View style={styles.topBar}>
        <View style={styles.titleBlock}>
          <Text style={styles.title} numberOfLines={1}>
            {roomState.meeting?.title || "会议房间"}
          </Text>
          <Text style={styles.caption} numberOfLines={1}>
            {formatDuration(elapsedSeconds)} · {roomState.mediaState.phaseLabel} · {clientState.phase === "failed" ? clientState.message : roomState.networkQualityLabel}
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
        <Control label={roomState.audioEnabled ? "静音" : "开麦"} onPress={() => act("toggleAudio")} />
        <Control label={roomState.videoEnabled ? "关视频" : "开视频"} onPress={() => act("toggleVideo")} />
        <Control label="挂断" danger onPress={() => act("leaveRoom")} />
        <Control label="成员" onPress={() => act("openSheet", {sheet: "Members"})} />
        <Control label="聊天" onPress={() => act("openSheet", {sheet: "Chat"})} />
        <Control label="更多" onPress={() => act("openSheet", {sheet: "More"})} />
      </View>

      <Modal visible={!!roomState.selectedSheet} animationType="slide" transparent onRequestClose={() => act("closeSheet")}>
        <Pressable style={styles.scrim} onPress={() => act("closeSheet")} />
        <View style={styles.sheet}>
          <SheetTabs selected={roomState.selectedSheet} onSelect={sheet => act("openSheet", {sheet})} />
          {roomState.selectedSheet === "Members" && <Members participants={participants} />}
          {roomState.selectedSheet === "Chat" && (
            <Chat
              roomState={roomState}
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
          {roomState.selectedSheet === "More" && (
            <More
              roomState={roomState}
              tab={moreTab}
              onTabChanged={setMoreTab}
              effectType={effectType}
              onEffectTypeChanged={setEffectType}
              virtualBackground={virtualBackground}
              onVirtualBackgroundChanged={setVirtualBackground}
              keepScreenAwake={keepScreenAwake}
              onKeepScreenAwakeChanged={setKeepScreenAwake}
            />
          )}
        </View>
      </Modal>
    </SafeAreaView>
  );
}

function VideoTile({participant, streamUrl, prominent = false, onPress}: {participant: RoomParticipant; streamUrl?: string; prominent?: boolean; onPress?: () => void}) {
  return (
    <Pressable style={[styles.tile, prominent ? styles.prominentTile : styles.smallTile]} onPress={onPress}>
      {participant.videoEnabled && streamUrl ? (
        <RTCView streamURL={streamUrl} objectFit="cover" style={StyleSheet.absoluteFill} />
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

function SheetTabs({selected, onSelect}: {selected?: string; onSelect: (sheet: string) => void}) {
  return (
    <View style={styles.sheetTabs}>
      {[
        ["Members", "成员"],
        ["Chat", "聊天"],
        ["More", "更多"],
      ].map(([sheet, label]) => (
        <Pressable key={sheet} style={[styles.sheetTab, selected === sheet && styles.sheetTabActive]} onPress={() => onSelect(sheet)}>
          <Text style={[styles.sheetTabText, selected === sheet && styles.sheetTabTextActive]}>{label}</Text>
        </Pressable>
      ))}
    </View>
  );
}

function Members({participants}: {participants: RoomParticipant[]}) {
  return (
    <ScrollView>
      <Text style={styles.sheetTitle}>成员</Text>
      {participants.map(participant => (
        <View key={participant.peerId} style={styles.memberRow}>
          <Text style={styles.memberName}>{participant.username}</Text>
          <Text style={styles.memberMeta}>{participant.roleLabel} · {participant.statusLabel}</Text>
          {!participant.isLocal && (
            <View style={styles.rowActions}>
              <Control compact label={participant.audioEnabled ? "静音" : "开麦"} onPress={() => perform("hostToggleParticipantAudio", participant)} />
              <Control compact label={participant.videoEnabled ? "关视频" : "开视频"} onPress={() => perform("hostToggleParticipantVideo", participant)} />
              <Control compact danger label="移出" onPress={() => perform("removeParticipant", participant)} />
            </View>
          )}
        </View>
      ))}
    </ScrollView>
  );
}

function Chat({roomState, draft, onDraftChanged, onSend}: {roomState: RoomState; draft: string; onDraftChanged: (value: string) => void; onSend: () => void}) {
  return (
    <View>
      <Text style={styles.sheetTitle}>聊天</Text>
      <View style={styles.rowActions}>
        <Control compact label="保存记录" onPress={() => Alert.alert("聊天记录", `当前可保存 ${roomState.chatMessages.length} 条消息`)} />
        <Control compact label="清空本地" onPress={() => Alert.alert("清空聊天", "移动端仅清空当前本地展示，服务端记录不受影响。")} />
      </View>
      <ScrollView style={styles.chatList}>
        {roomState.chatMessages.slice(-40).map(message => (
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
          <Control label={roomState.screenSharing ? "停止共享" : "屏幕共享"} onPress={() => perform("toggleScreenShare")} />
          <Control label={roomState.handRaised ? "取消举手" : "举手"} onPress={() => perform("toggleHandRaised")} />
          <Control label="切换摄像头" onPress={() => perform("switchCamera")} />
          <Control label={roomState.captionsEnabled ? "关闭字幕" : "字幕"} onPress={() => perform("toggleCaptions")} />
          <Control label="设置" onPress={() => perform("openMeetingSettings")} />
          <Control danger label="关闭会议" onPress={() => perform("closeMeeting")} />
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
              <Choice key={route} label={route === "Speaker" ? "扬声器" : "听筒"} active={roomState.selectedAudioRoute === route} onPress={() => perform("selectAudioRoute", {route})} />
            ))}
          </View>
          <Choice label="智能降噪" active onPress={() => Alert.alert("智能降噪", "移动端音频处理入口已预留，可接入 RN 音频处理模块。")} />
        </View>
      )}
      {tab === "android" && (
        <View style={styles.moreGrid}>
          <Choice label={keepScreenAwake ? "关闭常亮" : "屏幕常亮"} active={keepScreenAwake} onPress={() => onKeepScreenAwakeChanged(!keepScreenAwake)} />
          <Choice label="画中画" active={false} onPress={() => MeetingRoomBridge?.enterPictureInPicture?.()} />
          <Choice label="听筒优化" active={roomState.selectedAudioRoute === "Earpiece"} onPress={() => perform("selectAudioRoute", {route: "Earpiece"})} />
        </View>
      )}
    </View>
  );
}

function Choice({label, active, onPress}: {label: string; active: boolean; onPress: () => void}) {
  return (
    <Pressable style={[styles.choice, active && styles.choiceActive]} onPress={onPress}>
      <Text style={[styles.choiceText, active && styles.choiceTextActive]} numberOfLines={1}>{label}</Text>
    </Pressable>
  );
}

function Control({label, danger = false, compact = false, onPress}: {label: string; danger?: boolean; compact?: boolean; onPress: () => void}) {
  return (
    <Pressable style={[styles.control, danger && styles.dangerControl, compact && styles.compactControl]} onPress={onPress}>
      <Text style={[styles.controlText, danger && styles.dangerText]} numberOfLines={1}>{label}</Text>
    </Pressable>
  );
}

function perform(action: string, payload?: Record<string, unknown>) {
  MeetingRoomBridge?.perform?.(action, payload ?? {});
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
  root: {flex: 1, backgroundColor: "#111827"},
  stage: {flex: 1, padding: 12, paddingBottom: 96, gap: 10},
  prominentTile: {flex: 1},
  smallTile: {width: 150, height: 112, marginRight: 8},
  tile: {borderRadius: 8, overflow: "hidden", backgroundColor: "#1f2937"},
  avatarWrap: {flex: 1, alignItems: "center", justifyContent: "center"},
  avatar: {width: 46, height: 46, borderRadius: 23, backgroundColor: "#2563eb", alignItems: "center", justifyContent: "center"},
  prominentAvatar: {width: 88, height: 88, borderRadius: 44},
  avatarText: {color: "white", fontWeight: "700", fontSize: 22},
  placeholder: {marginTop: 8, color: "#d1d5db", fontSize: 12},
  filmstrip: {maxHeight: 120},
  compactFilmstrip: {maxHeight: 108},
  tileFooter: {position: "absolute", left: 0, right: 0, bottom: 0, padding: 8, backgroundColor: "rgba(17,24,39,0.84)", flexDirection: "row", alignItems: "center"},
  tileName: {flex: 1, color: "white", fontSize: 13, fontWeight: "600"},
  tileIcon: {color: "#d1d5db", fontSize: 11},
  topBar: {position: "absolute", top: 12, left: 12, right: 12, flexDirection: "row", gap: 8, alignItems: "center"},
  titleBlock: {flex: 1, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8, backgroundColor: "rgba(17,24,39,0.78)"},
  title: {color: "white", fontWeight: "700", fontSize: 16},
  caption: {color: "#d1d5db", fontSize: 12, marginTop: 2},
  badge: {borderRadius: 8, paddingHorizontal: 10, paddingVertical: 9, backgroundColor: "rgba(17,24,39,0.78)"},
  badgeText: {color: "white", fontWeight: "600", fontSize: 12},
  handBanner: {position: "absolute", top: 68, left: 12, right: 12, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 9, backgroundColor: "rgba(22,163,74,0.92)"},
  handText: {color: "white", fontSize: 13, fontWeight: "700"},
  controls: {position: "absolute", left: 0, right: 0, bottom: 0, padding: 12, paddingBottom: 18, backgroundColor: "#f9fafb", flexDirection: "row", gap: 6},
  control: {flex: 1, minHeight: 42, borderRadius: 8, borderWidth: 1, borderColor: "#d1d5db", backgroundColor: "white", alignItems: "center", justifyContent: "center", paddingHorizontal: 6},
  compactControl: {flex: 0, minHeight: 34, paddingHorizontal: 10},
  dangerControl: {borderColor: "#fecaca", backgroundColor: "#fff1f2"},
  controlText: {fontSize: 12, color: "#111827", fontWeight: "700"},
  dangerText: {color: "#dc2626"},
  scrim: {flex: 1, backgroundColor: "rgba(0,0,0,0.34)"},
  sheet: {maxHeight: "70%", backgroundColor: "white", padding: 18, borderTopLeftRadius: 8, borderTopRightRadius: 8},
  sheetTabs: {flexDirection: "row", gap: 8, marginBottom: 14},
  sheetTab: {flex: 1, minHeight: 36, borderRadius: 8, borderWidth: 1, borderColor: "#d1d5db", alignItems: "center", justifyContent: "center"},
  sheetTabActive: {backgroundColor: "#2563eb", borderColor: "#2563eb"},
  sheetTabText: {fontSize: 13, fontWeight: "700", color: "#374151"},
  sheetTabTextActive: {color: "white"},
  sheetTitle: {fontSize: 20, fontWeight: "800", color: "#111827", marginBottom: 14},
  memberRow: {paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: "#e5e7eb"},
  memberName: {fontSize: 15, fontWeight: "700", color: "#111827"},
  memberMeta: {fontSize: 12, color: "#6b7280", marginTop: 2},
  rowActions: {flexDirection: "row", gap: 8, marginTop: 8},
  chatList: {height: 280},
  chatLine: {fontSize: 14, color: "#111827", paddingVertical: 5},
  inputRow: {flexDirection: "row", gap: 8, alignItems: "center", marginTop: 12},
  input: {flex: 1, minHeight: 42, borderWidth: 1, borderColor: "#d1d5db", borderRadius: 8, paddingHorizontal: 12, color: "#111827"},
  moreGrid: {flexDirection: "row", flexWrap: "wrap", gap: 10},
  segmented: {flexDirection: "row", gap: 6, marginBottom: 14},
  segment: {flex: 1, minHeight: 34, borderRadius: 8, backgroundColor: "#f3f4f6", alignItems: "center", justifyContent: "center"},
  segmentActive: {backgroundColor: "#dbeafe"},
  segmentText: {fontSize: 12, fontWeight: "700", color: "#4b5563"},
  segmentTextActive: {color: "#1d4ed8"},
  backgroundGrid: {flexDirection: "row", flexWrap: "wrap", gap: 10, marginTop: 12},
  choice: {minWidth: 94, minHeight: 44, borderRadius: 8, borderWidth: 1, borderColor: "#d1d5db", alignItems: "center", justifyContent: "center", paddingHorizontal: 10, backgroundColor: "white"},
  choiceActive: {borderColor: "#2563eb", backgroundColor: "#eff6ff"},
  choiceText: {fontSize: 13, color: "#111827", fontWeight: "700"},
  choiceTextActive: {color: "#1d4ed8"},
  note: {fontSize: 12, color: "#6b7280", lineHeight: 18, marginTop: 12},
});

export default MeetingRoom;
