import React, {useEffect} from "react";
import {
  ActivityIndicator,
  Alert,
  Pressable,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from "react-native";
import type {AuthUiState, CreateMeetingForm, MainTab, MainUiState, MeetingCreateType, MeetingSummary, ProfileEditForm} from "./appTypes";
import {colors, rgba} from "./theme";

type AuthActions = {
  onPhoneChanged: (value: string) => void;
  onCodeChanged: (value: string) => void;
  onAgreeTermsChanged: (value: boolean) => void;
  sendVerificationCode: () => void;
  submitLogin: () => void;
  consumeAuthMessage: () => void;
};

type MainActions = {
  selectTab: (tab: MainTab) => void;
  updateHomeMeetingNo: (value: string) => void;
  openJoinMeeting: (value?: string) => void;
  openCreateMeeting: (type: MeetingCreateType) => void;
  backToTabs: () => void;
  updateJoinMeetingNo: (value: string) => void;
  validateJoinMeeting: () => void;
  joinValidatedMeeting: () => void;
  joinMeetingDirectly: (meeting: MeetingSummary) => void;
  updateCreateForm: (patch: Partial<CreateMeetingForm>) => void;
  createMeeting: () => void;
  enterMeetingFromPreview: () => void;
  openProfileEditor: () => void;
  updateProfileForm: (patch: Partial<ProfileEditForm>) => void;
  saveProfile: () => void;
  logout: () => void;
  consumeMainMessage: () => void;
};

export function SplashScreen() {
  return (
    <SafeAreaView style={styles.splash}>
      <StatusBar barStyle="light-content" backgroundColor={colors.primary} />
      <View style={styles.logo}>
        <Text style={styles.logoText}>B</Text>
      </View>
      <Text style={styles.splashTitle}>Blue Sky</Text>
      <Text style={styles.splashSubtitle}>Secure video meetings</Text>
    </SafeAreaView>
  );
}

export function AuthScreen({state, actions}: {state: AuthUiState; actions: AuthActions}) {
  useEffect(() => {
    if (state.message) {
      Alert.alert("提示", state.message, [{text: "确定", onPress: actions.consumeAuthMessage}]);
    }
  }, [state.message, actions]);

  const canSendCode = /^1[3-9]\d{9}$/.test(state.phone) && state.countdownSeconds === 0 && !state.sendingCode;
  const canSubmit = /^1[3-9]\d{9}$/.test(state.phone) && state.code.length === 6 && state.agreeTerms && !state.loading;

  return (
    <SafeAreaView style={styles.root}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.background} />
      <ScrollView contentContainerStyle={styles.authContent} keyboardShouldPersistTaps="handled">
        <Text style={styles.authTitle}>欢迎回来</Text>
        <Text style={styles.muted}>使用手机号验证码登录，新用户将自动完成注册。</Text>
        <Field label="手机号" value={state.phone} onChangeText={actions.onPhoneChanged} keyboardType="phone-pad" placeholder="请输入 11 位手机号" error={state.phoneError} />
        <Field label="验证码" value={state.code} onChangeText={actions.onCodeChanged} keyboardType="number-pad" placeholder="6 位验证码" error={state.codeError} />
        <View style={styles.inlineRow}>
          <Switch value={state.agreeTerms} onValueChange={actions.onAgreeTermsChanged} />
          <Text style={styles.inlineText}>我已阅读并同意用户协议和隐私政策</Text>
        </View>
        <View style={styles.buttonRow}>
          <Button label={state.countdownSeconds > 0 ? `${state.countdownSeconds}s` : "获取验证码"} onPress={actions.sendVerificationCode} disabled={!canSendCode} secondary />
          <Button label={state.loading ? "登录中" : "登录"} onPress={actions.submitLogin} disabled={!canSubmit} />
        </View>
        <Text style={styles.note}>新用户可直接使用验证码注册登录</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

export function MainScreen({state, actions, room}: {state: MainUiState; actions: MainActions; room: React.ReactNode}) {
  useEffect(() => {
    if (state.message) {
      Alert.alert("提示", state.message, [{text: "确定", onPress: actions.consumeMainMessage}]);
    }
  }, [state.message, actions]);

  if (state.route === "Room") {
    return <>{room}</>;
  }

  const title = routeTitle(state);
  return (
    <SafeAreaView style={styles.root}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.background} />
      <View style={styles.appBar}>
        {state.route !== "Tabs" && <Pressable onPress={actions.backToTabs} style={styles.backButton}><Text style={styles.backText}>返回</Text></Pressable>}
        <Text style={styles.appBarTitle}>{title}</Text>
      </View>
      {state.isLoading && <ActivityIndicator style={styles.loading} color={colors.primary} />}
      {state.route === "Tabs" && (
        <>
          <ScrollView contentContainerStyle={styles.page}>
            {state.selectedTab === "Home" && <HomeScreen state={state} actions={actions} />}
            {state.selectedTab === "Meetings" && <MeetingsScreen state={state} actions={actions} />}
            {state.selectedTab === "Profile" && <ProfileScreen state={state} actions={actions} />}
          </ScrollView>
          <TabBar selected={state.selectedTab} onSelect={actions.selectTab} />
        </>
      )}
      {state.route === "JoinMeeting" && <JoinMeetingScreen state={state} actions={actions} />}
      {state.route === "CreateMeeting" && <CreateMeetingScreen state={state} actions={actions} />}
      {state.route === "PreJoin" && <PreJoinScreen state={state} actions={actions} />}
      {state.route === "EditProfile" && <ProfileEditScreen state={state} actions={actions} />}
    </SafeAreaView>
  );
}

function HomeScreen({state, actions}: {state: MainUiState; actions: MainActions}) {
  return (
    <View>
      <Text style={styles.heroTitle}>你好，{state.userSummary.displayName}</Text>
      <Text style={styles.muted}>快速创建或加入实时视频会议</Text>
      <View style={styles.panel}>
        <Field label="会议号" value={state.homeMeetingNo} onChangeText={actions.updateHomeMeetingNo} placeholder="输入会议号加入" />
        <Button label="加入会议" onPress={() => actions.openJoinMeeting(state.homeMeetingNo)} />
      </View>
      <View style={styles.grid}>
        <Button label="立即会议" onPress={() => actions.openCreateMeeting("Instant")} />
        <Button label="预约会议" onPress={() => actions.openCreateMeeting("Scheduled")} secondary />
      </View>
      <SectionTitle title="近期会议" action="查看全部" onPress={() => actions.selectTab("Meetings")} />
      {state.recentMeetings.slice(0, 3).map(meeting => <MeetingCard key={`${meeting.roomId}-${meeting.roomNo}`} meeting={meeting} onPress={() => actions.joinMeetingDirectly(meeting)} />)}
    </View>
  );
}

function MeetingsScreen({state, actions}: {state: MainUiState; actions: MainActions}) {
  return (
    <View>
      <SectionTitle title="预约会议" />
      {state.upcomingMeetings.length === 0 && <Empty text="暂无预约会议" />}
      {state.upcomingMeetings.map(meeting => <MeetingCard key={`up-${meeting.roomId}-${meeting.roomNo}`} meeting={meeting} onPress={() => actions.joinMeetingDirectly(meeting)} />)}
      <SectionTitle title="最近会议" />
      {state.recentMeetings.length === 0 && <Empty text="暂无最近会议" />}
      {state.recentMeetings.map(meeting => <MeetingCard key={`recent-${meeting.roomId}-${meeting.roomNo}`} meeting={meeting} onPress={() => actions.joinMeetingDirectly(meeting)} />)}
    </View>
  );
}

function ProfileScreen({state, actions}: {state: MainUiState; actions: MainActions}) {
  return (
    <View>
      <View style={styles.profileHeader}>
        <View style={styles.avatar}><Text style={styles.avatarText}>{state.userSummary.displayName.slice(0, 1)}</Text></View>
        <View style={styles.profileMeta}>
          <Text style={styles.profileName}>{state.userSummary.displayName}</Text>
          <Text style={styles.muted}>{state.userSummary.phone || state.userSummary.userId || "未绑定手机号"}</Text>
          <Text style={styles.statusText}>{state.userSummary.online ? "在线" : "离线"}</Text>
        </View>
      </View>
      <Button label="编辑资料" onPress={actions.openProfileEditor} />
      <Button label={state.isLoggingOut ? "退出中" : "退出登录"} onPress={actions.logout} danger />
    </View>
  );
}

function JoinMeetingScreen({state, actions}: {state: MainUiState; actions: MainActions}) {
  return (
    <ScrollView contentContainerStyle={styles.page} keyboardShouldPersistTaps="handled">
      <Field label="会议号" value={state.joinMeetingNo} onChangeText={actions.updateJoinMeetingNo} placeholder="请输入会议号" error={state.joinError} />
      {state.validatedMeeting && <MeetingCard meeting={state.validatedMeeting} onPress={actions.joinValidatedMeeting} />}
      <View style={styles.buttonRow}>
        <Button label="验证会议" onPress={actions.validateJoinMeeting} secondary disabled={state.isSubmitting} />
        <Button label="加入" onPress={actions.joinValidatedMeeting} disabled={!state.validatedMeeting || state.isSubmitting} />
      </View>
    </ScrollView>
  );
}

function CreateMeetingScreen({state, actions}: {state: MainUiState; actions: MainActions}) {
  const form = state.createForm;
  return (
    <ScrollView contentContainerStyle={styles.page} keyboardShouldPersistTaps="handled">
      <Field label="会议标题" value={form.title} onChangeText={title => actions.updateCreateForm({title})} placeholder="请输入会议标题" />
      <Segmented
        value={form.type}
        items={[["Instant", "立即会议"], ["Scheduled", "预约会议"]]}
        onChange={type => actions.updateCreateForm({type: type as MeetingCreateType})}
      />
      <View style={styles.grid}>
        <Field label="日期" value={form.startDate} onChangeText={startDate => actions.updateCreateForm({startDate})} placeholder="yyyy-MM-dd" />
        <Field label="时间" value={form.startTime} onChangeText={startTime => actions.updateCreateForm({startTime})} placeholder="HH:mm" />
      </View>
      <Field label="描述" value={form.description} onChangeText={description => actions.updateCreateForm({description})} placeholder="会议说明" multiline />
      <ToggleRow label="默认开启麦克风" value={form.audioEnabled} onValueChange={audioEnabled => actions.updateCreateForm({audioEnabled})} />
      <ToggleRow label="默认开启摄像头" value={form.videoEnabled} onValueChange={videoEnabled => actions.updateCreateForm({videoEnabled})} />
      <Button label={state.isSubmitting ? "创建中" : "创建会议"} onPress={actions.createMeeting} disabled={state.isSubmitting} />
    </ScrollView>
  );
}

function PreJoinScreen({state, actions}: {state: MainUiState; actions: MainActions}) {
  const meeting = state.preJoinMeeting;
  return (
    <ScrollView contentContainerStyle={styles.page}>
      <View style={styles.preview}>
        <Text style={styles.previewText}>{state.createForm.videoEnabled ? "本地视频预览" : "摄像头已关闭"}</Text>
      </View>
      <Text style={styles.heroTitle}>{meeting?.title || "会前预览"}</Text>
      <Text style={styles.muted}>会议号：{meeting?.roomNo || "-"}</Text>
      {state.permissionHint && <Text style={styles.errorText}>{state.permissionHint}</Text>}
      <ToggleRow label="麦克风" value={state.createForm.audioEnabled} onValueChange={audioEnabled => actions.updateCreateForm({audioEnabled})} />
      <ToggleRow label="摄像头" value={state.createForm.videoEnabled} onValueChange={videoEnabled => actions.updateCreateForm({videoEnabled})} />
      <Button label="进入会议" onPress={actions.enterMeetingFromPreview} disabled={state.isSubmitting} />
    </ScrollView>
  );
}

function ProfileEditScreen({state, actions}: {state: MainUiState; actions: MainActions}) {
  const form = state.profileEditForm;
  return (
    <ScrollView contentContainerStyle={styles.page} keyboardShouldPersistTaps="handled">
      <Field label="昵称" value={form.nickname} onChangeText={nickname => actions.updateProfileForm({nickname})} />
      <Field label="性别" value={String(form.sex)} onChangeText={sex => actions.updateProfileForm({sex: Number(sex) || 0})} keyboardType="number-pad" />
      <Field label="生日" value={form.birthday || ""} onChangeText={birthday => actions.updateProfileForm({birthday})} placeholder="yyyy-MM-dd" />
      <Field label="个人简介" value={form.introduction} onChangeText={introduction => actions.updateProfileForm({introduction})} multiline />
      <Button label={state.isSubmitting ? "保存中" : "保存资料"} onPress={actions.saveProfile} disabled={state.isSubmitting} />
    </ScrollView>
  );
}

function TabBar({selected, onSelect}: {selected: MainTab; onSelect: (tab: MainTab) => void}) {
  const tabs: Array<[MainTab, string, string]> = [["Home", "首", "首页"], ["Meetings", "会", "会议"], ["Profile", "我", "我的"]];
  return (
    <View style={styles.tabBar}>
      {tabs.map(([tab, icon, label]) => (
        <Pressable key={tab} style={styles.tab} onPress={() => onSelect(tab)}>
          <Text style={[styles.tabIcon, selected === tab && styles.tabActive]}>{icon}</Text>
          <Text style={[styles.tabLabel, selected === tab && styles.tabActive]}>{label}</Text>
        </Pressable>
      ))}
    </View>
  );
}

function Field({label, error, ...inputProps}: React.ComponentProps<typeof TextInput> & {label: string; error?: string}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput placeholderTextColor="#8E939B" style={[styles.input, inputProps.multiline && styles.textArea]} {...inputProps} />
      {!!error && <Text style={styles.errorText}>{error}</Text>}
    </View>
  );
}

function Button({label, onPress, disabled, secondary, danger}: {label: string; onPress: () => void; disabled?: boolean; secondary?: boolean; danger?: boolean}) {
  return (
    <Pressable
      android_ripple={{color: rgba(danger ? colors.error : colors.primary, 0.18)}}
      style={[styles.button, secondary && styles.secondaryButton, danger && styles.dangerButton, disabled && styles.disabledButton]}
      onPress={onPress}
      disabled={disabled}
    >
      <Text style={[styles.buttonText, secondary && styles.secondaryButtonText, danger && styles.dangerButtonText]}>{label}</Text>
    </Pressable>
  );
}

function ToggleRow({label, value, onValueChange}: {label: string; value: boolean; onValueChange: (value: boolean) => void}) {
  return (
    <View style={styles.toggleRow}>
      <Text style={styles.label}>{label}</Text>
      <Switch value={value} onValueChange={onValueChange} />
    </View>
  );
}

function Segmented({value, items, onChange}: {value: string; items: Array<[string, string]>; onChange: (value: string) => void}) {
  return (
    <View style={styles.segmented}>
      {items.map(([itemValue, label]) => (
        <Pressable key={itemValue} style={[styles.segment, value === itemValue && styles.segmentActive]} onPress={() => onChange(itemValue)}>
          <Text style={[styles.segmentText, value === itemValue && styles.segmentTextActive]}>{label}</Text>
        </Pressable>
      ))}
    </View>
  );
}

function SectionTitle({title, action, onPress}: {title: string; action?: string; onPress?: () => void}) {
  return (
    <View style={styles.sectionTitle}>
      <Text style={styles.sectionText}>{title}</Text>
      {action && onPress && <Pressable onPress={onPress}><Text style={styles.linkText}>{action}</Text></Pressable>}
    </View>
  );
}

function MeetingCard({meeting, onPress}: {meeting: MeetingSummary; onPress: () => void}) {
  return (
    <Pressable style={styles.meetingCard} onPress={onPress}>
      <Text style={styles.meetingTitle}>{meeting.title}</Text>
      <Text style={styles.muted}>会议号：{meeting.roomNo || meeting.roomId}</Text>
      <Text style={styles.muted}>{meeting.startTime || "立即会议"} · {meeting.hostName || "主持人"}</Text>
    </Pressable>
  );
}

function Empty({text}: {text: string}) {
  return <Text style={styles.empty}>{text}</Text>;
}

function routeTitle(state: MainUiState) {
  if (state.route === "Tabs") return {Home: "首页", Meetings: "会议", Profile: "我的"}[state.selectedTab];
  if (state.route === "Room") return "会议房间";
  return {CreateMeeting: "创建会议", JoinMeeting: "加入会议", PreJoin: "会前预览", EditProfile: "编辑资料"}[state.route] || "BlueSky";
}

const styles = StyleSheet.create({
  root: {flex: 1, backgroundColor: colors.background},
  splash: {flex: 1, alignItems: "center", justifyContent: "center", backgroundColor: colors.primary},
  logo: {width: 72, height: 72, borderRadius: 16, alignItems: "center", justifyContent: "center", backgroundColor: colors.surface},
  logoText: {fontSize: 36, fontWeight: "900", color: colors.primary},
  splashTitle: {marginTop: 20, color: colors.textDark, fontSize: 26, fontWeight: "900"},
  splashSubtitle: {marginTop: 6, color: colors.primaryContainer, fontSize: 14},
  authContent: {flexGrow: 1, justifyContent: "center", padding: 24},
  authTitle: {fontSize: 30, fontWeight: "900", color: colors.text},
  appBar: {minHeight: 56, paddingHorizontal: 16, flexDirection: "row", alignItems: "center", borderBottomWidth: 1, borderBottomColor: colors.surfaceVariant, backgroundColor: colors.surface},
  appBarTitle: {fontSize: 20, fontWeight: "800", color: colors.text},
  backButton: {paddingRight: 16, paddingVertical: 10},
  backText: {color: colors.primary, fontWeight: "700"},
  loading: {position: "absolute", top: 64, right: 20, zIndex: 2},
  page: {padding: 16, paddingBottom: 96},
  heroTitle: {fontSize: 24, fontWeight: "900", color: colors.text, marginBottom: 6},
  muted: {fontSize: 13, color: colors.textMuted, lineHeight: 20},
  panel: {marginTop: 20, padding: 14, borderRadius: 8, borderWidth: 1, borderColor: colors.surfaceVariant, backgroundColor: colors.surface},
  grid: {flexDirection: "row", gap: 10},
  field: {flex: 1, marginTop: 14},
  label: {fontSize: 14, fontWeight: "700", color: colors.text, marginBottom: 8},
  input: {minHeight: 46, borderWidth: 1, borderColor: colors.outlineVariant, borderRadius: 8, paddingHorizontal: 12, color: colors.text, backgroundColor: colors.surface},
  textArea: {minHeight: 92, textAlignVertical: "top", paddingTop: 12},
  errorText: {marginTop: 6, color: colors.error, fontSize: 12},
  inlineRow: {flexDirection: "row", alignItems: "center", gap: 8, marginTop: 16},
  inlineText: {flex: 1, color: colors.text, fontSize: 13},
  buttonRow: {flexDirection: "row", gap: 10, marginTop: 18},
  button: {flex: 1, minHeight: 48, borderRadius: 8, alignItems: "center", justifyContent: "center", backgroundColor: colors.primary, paddingHorizontal: 12, marginTop: 14},
  secondaryButton: {backgroundColor: colors.primaryContainer, borderWidth: 1, borderColor: colors.primary},
  dangerButton: {backgroundColor: colors.errorContainer, borderWidth: 1, borderColor: colors.error},
  disabledButton: {opacity: 0.45},
  buttonText: {fontSize: 14, fontWeight: "800", color: colors.surface},
  secondaryButtonText: {color: colors.primary},
  dangerButtonText: {color: colors.error},
  note: {marginTop: 16, color: colors.textMuted, fontSize: 12},
  sectionTitle: {marginTop: 24, marginBottom: 8, flexDirection: "row", alignItems: "center", justifyContent: "space-between"},
  sectionText: {fontSize: 18, fontWeight: "900", color: colors.text},
  linkText: {fontSize: 13, fontWeight: "700", color: colors.primary},
  meetingCard: {padding: 14, borderRadius: 8, borderWidth: 1, borderColor: colors.surfaceVariant, backgroundColor: colors.surface, marginBottom: 10},
  meetingTitle: {fontSize: 16, fontWeight: "800", color: colors.text, marginBottom: 6},
  empty: {paddingVertical: 18, color: colors.textMuted, textAlign: "center"},
  profileHeader: {flexDirection: "row", alignItems: "center", marginBottom: 20},
  avatar: {width: 64, height: 64, borderRadius: 32, alignItems: "center", justifyContent: "center", backgroundColor: colors.primary},
  avatarText: {color: colors.surface, fontSize: 26, fontWeight: "900"},
  profileMeta: {flex: 1, marginLeft: 14},
  profileName: {fontSize: 22, fontWeight: "900", color: colors.text},
  statusText: {marginTop: 4, color: colors.speaking, fontSize: 12, fontWeight: "700"},
  preview: {height: 220, borderRadius: 8, alignItems: "center", justifyContent: "center", backgroundColor: colors.videoTile, marginBottom: 18},
  previewText: {color: colors.onVideoTile, fontWeight: "800"},
  toggleRow: {minHeight: 54, flexDirection: "row", alignItems: "center", justifyContent: "space-between", borderBottomWidth: 1, borderBottomColor: colors.surfaceVariant},
  segmented: {flexDirection: "row", gap: 8, marginTop: 16},
  segment: {flex: 1, minHeight: 42, borderRadius: 8, borderWidth: 1, borderColor: colors.surfaceVariant, alignItems: "center", justifyContent: "center"},
  segmentActive: {backgroundColor: colors.primaryContainer, borderColor: colors.primary},
  segmentText: {fontSize: 14, fontWeight: "800", color: colors.textMuted},
  segmentTextActive: {color: colors.primary},
  tabBar: {position: "absolute", left: 0, right: 0, bottom: 0, minHeight: 68, flexDirection: "row", borderTopWidth: 1, borderTopColor: colors.surfaceVariant, backgroundColor: colors.surface},
  tab: {flex: 1, alignItems: "center", justifyContent: "center"},
  tabIcon: {fontSize: 18, fontWeight: "900", color: colors.textMuted},
  tabLabel: {fontSize: 12, color: colors.textMuted, marginTop: 2},
  tabActive: {color: colors.primary},
});
