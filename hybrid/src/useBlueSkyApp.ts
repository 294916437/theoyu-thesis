import {useCallback, useEffect, useMemo, useRef, useState} from "react";
import type React from "react";
import {Alert, PermissionsAndroid, Platform} from "react-native";
import {authApi, asArray, firstString, responseDataObject, roomApi, userApi} from "./api";
import type {
  AuthUiState,
  CreateMeetingForm,
  MainTab,
  MainUiState,
  MeetingCreateType,
  MeetingSummary,
  ProfileEditForm,
  UserProfile,
  UserSummary,
} from "./appTypes";
import {clearSession, currentSession, loadSession, saveSession, saveUserProfile} from "./session";
import type {RoomParticipant, RoomState, SfuProducerState} from "./types";

const PHONE_REGEX = /^1[3-9]\d{9}$/;
const CODE_LENGTH = 6;
const CODE_COUNTDOWN_SECONDS = 180;
const MEETING_NO_MAX_LENGTH = 12;
const MEETING_NO_MIN_LENGTH = 4;
const TITLE_MAX_LENGTH = 50;
const DESCRIPTION_MAX_LENGTH = 500;
const DEFAULT_SFU_SOCKET_URL = "http://10.0.2.2:3000";

const defaultCreateForm = (): CreateMeetingForm => {
  const date = new Date(Date.now() + 15 * 60 * 1000);
  return {
    title: "",
    type: "Instant",
    startDate: formatDate(date),
    startTime: formatTime(date),
    description: "",
    audioEnabled: true,
    videoEnabled: true,
  };
};

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

const initialAuthState: AuthUiState = {
  phone: "",
  code: "",
  agreeTerms: false,
  loading: false,
  sendingCode: false,
  countdownSeconds: 0,
  authenticated: false,
};

const initialMainState: MainUiState = {
  selectedTab: "Home",
  route: "Tabs",
  userSummary: {userId: "", displayName: "会议用户", phone: "", avatar: "", online: false},
  upcomingMeetings: [],
  recentMeetings: [],
  homeMeetingNo: "",
  joinMeetingNo: "",
  activeRoom: EMPTY_ROOM,
  createForm: defaultCreateForm(),
  profileEditForm: {nickname: "", sex: 0, introduction: ""},
  audioRoute: "Speaker",
  availableAudioRoutes: ["Speaker"],
  cameraPermissionGranted: false,
  audioPermissionGranted: false,
  isLoading: false,
  isSubmitting: false,
  isLoggingOut: false,
  loggedOut: false,
};

export function useBlueSkyApp() {
  const [booting, setBooting] = useState(true);
  const [auth, setAuth] = useState<AuthUiState>(initialAuthState);
  const [main, setMain] = useState<MainUiState>(initialMainState);
  const countdownRef = useRef<ReturnType<typeof setInterval> | undefined>(undefined);

  const refresh = useCallback(async () => {
    setMain(state => ({...state, isLoading: true, message: undefined, loggedOut: false}));
    const session = currentSession();
    if (session.userId) {
      await userApi.setUserOnlineStatus(session.userId).catch(() => undefined);
    }
    const [userSummary, online, upcoming, recent] = await Promise.all([
      loadUserSummary().catch(() => undefined),
      session.userId ? userApi.getUserOnlineStatus(session.userId).catch(() => undefined) : undefined,
      roomApi.fetchUpcomingMeetings().catch(() => undefined),
      roomApi.fetchRecentMeetings().catch(() => undefined),
    ]);
    setMain(state => ({
      ...state,
      userSummary: userSummary ? {...userSummary, online: parseOnline(online)} : state.userSummary,
      upcomingMeetings: parseMeetingList(upcoming),
      recentMeetings: parseMeetingList(recent),
      isLoading: false,
    }));
  }, []);

  useEffect(() => {
    let mounted = true;
    (async () => {
      const session = await loadSession();
      await wait(900);
      if (!mounted) return;
      if (session.token) {
        setAuth(state => ({...state, authenticated: true}));
        await refresh();
      }
      setBooting(false);
    })();
    return () => {
      mounted = false;
      clearCountdown(countdownRef);
    };
  }, [refresh]);

  const actions = useMemo(
    () => ({
      onPhoneChanged(phone: string) {
        setAuth(state => ({...state, phone: digits(phone).slice(0, 11), phoneError: undefined, message: undefined}));
      },
      onCodeChanged(code: string) {
        setAuth(state => ({...state, code: digits(code).slice(0, CODE_LENGTH), codeError: undefined, message: undefined}));
      },
      onAgreeTermsChanged(agreeTerms: boolean) {
        setAuth(state => ({...state, agreeTerms, message: undefined}));
      },
      async sendVerificationCode() {
        const state = auth;
        if (!PHONE_REGEX.test(state.phone)) {
          setAuth(current => ({...current, phoneError: "请输入正确的手机号", message: "请输入正确的手机号"}));
          return;
        }
        if (state.countdownSeconds > 0) {
          setAuth(current => ({...current, message: "请稍后再试"}));
          return;
        }
        setAuth(current => ({...current, sendingCode: true, message: undefined}));
        try {
          const response: any = await authApi.getVerificationCode(state.phone);
          setAuth(current => ({...current, message: isSuccessful(response) ? "验证码已发送" : responseMessage(response) || "发送验证码失败"}));
          if (isSuccessful(response)) startCountdown(setAuth, countdownRef);
        } catch (error) {
          setAuth(current => ({...current, message: errorMessage(error)}));
        } finally {
          setAuth(current => ({...current, sendingCode: false}));
        }
      },
      async submitLogin() {
        const validation = validateAuth(auth);
        if (validation) {
          setAuth(current => ({...current, ...validation}));
          return;
        }
        setAuth(current => ({...current, loading: true, message: undefined}));
        try {
          const response: any = await authApi.login({phone: auth.phone, code: auth.code, type: 1});
          if (!isSuccessful(response)) {
            setAuth(current => ({...current, codeError: "验证码错误", message: "验证码错误"}));
            return;
          }
          const data = responseDataObject(response);
          const token = firstString(data, "token");
          const userId = firstString(data, "userId");
          if (!token || !userId) {
            setAuth(current => ({...current, message: "登录响应缺少 token 或 userId"}));
            return;
          }
          await saveSession(token, userId);
          const profile = await userApi.getUserProfile(userId).catch(() => undefined);
          const profileData = responseDataObject(profile);
          await saveUserProfile({
            userId,
            nickname: firstString(profileData, "nickname", "username", "name", "displayName"),
            phone: firstString(profileData, "phone", "mobile"),
            avatar: firstString(profileData, "avatar"),
          });
          setAuth(current => ({...current, authenticated: true, message: "登录成功"}));
          await refresh();
        } catch (error) {
          setAuth(current => ({...current, message: errorMessage(error)}));
        } finally {
          setAuth(current => ({...current, loading: false}));
        }
      },
      consumeAuthMessage() {
        setAuth(state => ({...state, message: undefined}));
      },
      selectTab(tab: MainTab) {
        setMain(state => ({...state, selectedTab: tab, route: "Tabs", message: undefined}));
        if (tab === "Profile") refresh();
      },
      updateHomeMeetingNo(value: string) {
        setMain(state => ({...state, homeMeetingNo: cleanMeetingNo(value)}));
      },
      openJoinMeeting(initialMeetingNo = "") {
        setMain(state => ({...state, route: "JoinMeeting", joinMeetingNo: cleanMeetingNo(initialMeetingNo), joinError: undefined, validatedMeeting: undefined}));
      },
      openCreateMeeting(type: MeetingCreateType) {
        setMain(state => ({
          ...state,
          route: "CreateMeeting",
          createForm: {...defaultCreateForm(), type, title: `${state.userSummary.displayName}的会议`},
          message: undefined,
        }));
      },
      backToTabs() {
        setMain(state => ({...state, route: "Tabs", message: undefined}));
      },
      updateJoinMeetingNo(value: string) {
        setMain(state => ({...state, joinMeetingNo: cleanMeetingNo(value), joinError: undefined, validatedMeeting: undefined, message: undefined}));
      },
      updateCreateForm(patch: Partial<CreateMeetingForm>) {
        setMain(state => ({
          ...state,
          createForm: {
            ...state.createForm,
            ...patch,
            title: patch.title !== undefined ? patch.title.slice(0, TITLE_MAX_LENGTH) : state.createForm.title,
            description: patch.description !== undefined ? patch.description.slice(0, DESCRIPTION_MAX_LENGTH) : state.createForm.description,
          },
        }));
      },
      updateProfileForm(patch: Partial<ProfileEditForm>) {
        setMain(state => ({
          ...state,
          profileEditForm: {
            ...state.profileEditForm,
            ...patch,
            nickname: patch.nickname !== undefined ? patch.nickname.slice(0, TITLE_MAX_LENGTH) : state.profileEditForm.nickname,
            introduction: patch.introduction !== undefined ? patch.introduction.slice(0, 100) : state.profileEditForm.introduction,
          },
        }));
      },
      async validateJoinMeeting() {
        if (main.joinMeetingNo.length < MEETING_NO_MIN_LENGTH) {
          setMain(state => ({...state, joinError: "请输入正确的会议号"}));
          return;
        }
        setMain(state => ({...state, isSubmitting: true, message: undefined}));
        try {
          const response = await roomApi.validateMeetingNo(main.joinMeetingNo);
          const meeting = parseMeeting(response);
          if (!meeting) {
            setMain(state => ({...state, joinError: "会议不存在，请检查会议号", validatedMeeting: undefined}));
          } else {
            setMain(state => ({...state, validatedMeeting: meeting, joinError: undefined}));
          }
        } catch (error) {
          setMain(state => ({...state, joinError: mapJoinFailure(errorMessage(error))}));
        } finally {
          setMain(state => ({...state, isSubmitting: false}));
        }
      },
      async joinValidatedMeeting() {
        if (main.validatedMeeting) await preparePreJoin(main.validatedMeeting, setMain);
      },
      async joinMeetingDirectly(meeting: MeetingSummary) {
        await preparePreJoin(meeting, setMain);
      },
      async createMeeting() {
        const form = main.createForm;
        if (form.title.trim().length < 2) {
          setMain(state => ({...state, message: "会议标题至少需要 2 个字符"}));
          return;
        }
        setMain(state => ({...state, isSubmitting: true, message: undefined}));
        try {
          const startTime = `${form.startDate} ${form.startTime}:00`;
          const response = await roomApi.createMeeting({
            title: form.title.trim(),
            type: form.type === "Instant" ? 1 : 2,
            startTime,
            maxParticipants: 15,
            settings: buildSettingsJson(form),
          });
          const meeting = parseMeeting(response);
          if (!meeting) throw new Error("创建会议响应缺少会议信息");
          await preparePreJoin(meeting, setMain);
          await refresh();
        } catch (error) {
          setMain(state => ({...state, message: errorMessage(error)}));
        } finally {
          setMain(state => ({...state, isSubmitting: false}));
        }
      },
      async enterMeetingFromPreview() {
        const meeting = main.preJoinMeeting;
        if (!meeting) {
          setMain(state => ({...state, message: "缺少会议信息，无法进入房间"}));
          return;
        }
        const permissions = await requestMediaPermissions();
        setMain(state => ({
          ...state,
          cameraPermissionGranted: permissions.camera,
          audioPermissionGranted: permissions.audio,
          permissionHint: permissionHint(permissions.camera, permissions.audio),
        }));
        if (!permissions.camera || !permissions.audio) return;
        const session = currentSession();
        const localPeerId = session.userId || "local";
        const localParticipant: RoomParticipant = {
          peerId: localPeerId,
          userId: session.userId || "",
          username: main.userSummary.displayName,
          role: meeting.hostId === session.userId ? "Host" : "Member",
          roleLabel: meeting.hostId === session.userId ? "主持人" : "成员",
          status: "Online",
          statusLabel: "在线",
          isLocal: true,
          audioEnabled: main.createForm.audioEnabled,
          videoEnabled: main.createForm.videoEnabled,
          handRaised: false,
          speaking: false,
        };
        setMain(state => ({
          ...state,
          route: "Room",
          activeRoom: {
            ...EMPTY_ROOM,
            currentUserId: session.userId,
            currentPeerId: localPeerId,
            currentUsername: state.userSummary.displayName,
            authToken: session.token,
            meeting,
            participants: [localParticipant],
            activeSpeakerPeerId: localPeerId,
            socketConnected: true,
            audioEnabled: state.createForm.audioEnabled,
            videoEnabled: state.createForm.videoEnabled,
            networkQuality: "Excellent",
            networkQualityLabel: "网络良好",
            mediaState: {
              ...EMPTY_ROOM.mediaState,
              phase: "Joining",
              phaseLabel: "加入房间",
              localProducers: buildLocalProducerPlaceholders(localPeerId, state.userSummary.displayName, state.createForm.audioEnabled, state.createForm.videoEnabled),
            },
          },
        }));
      },
      async openProfileEditor() {
        const userId = main.userSummary.userId;
        if (!userId) {
          setMain(state => ({...state, message: "未登录，请先登录"}));
          return;
        }
        setMain(state => ({...state, isLoading: true, message: undefined}));
        try {
          const response = await userApi.getUserProfile(userId);
          const data = responseDataObject(response) || {};
          const profile: UserProfile = {
            userId,
            avatar: firstString(data, "avatar") || "",
            nickname: firstString(data, "nickname") || "",
            userAppId: firstString(data, "userAppId") || "",
            sex: Number(firstString(data, "sex") || 0),
            phone: firstString(data, "phone") || "",
            age: Number(firstString(data, "age") || 0),
            birthday: firstString(data, "birthday"),
            backgroundImg: firstString(data, "backgroundImg") || "",
            introduction: firstString(data, "introduction") || "",
          };
          setMain(state => ({
            ...state,
            userProfile: profile,
            profileEditForm: {nickname: profile.nickname, sex: profile.sex, birthday: profile.birthday, introduction: profile.introduction},
            route: "EditProfile",
          }));
        } catch (error) {
          setMain(state => ({...state, message: errorMessage(error)}));
        } finally {
          setMain(state => ({...state, isLoading: false}));
        }
      },
      async saveProfile() {
        const userId = main.userSummary.userId;
        const form = main.profileEditForm;
        const nickname = form.nickname.trim();
        if (!userId) {
          setMain(state => ({...state, message: "缺少用户信息，请重新登录"}));
          return;
        }
        if (nickname.length < 2) {
          setMain(state => ({...state, message: "昵称至少需要 2 个字符"}));
          return;
        }
        setMain(state => ({...state, isSubmitting: true, message: undefined}));
        try {
          const data = new FormData();
          data.append("userId", userId);
          data.append("nickname", nickname);
          data.append("sex", String(form.sex));
          data.append("introduction", form.introduction.trim());
          if (form.birthday) data.append("birthday", form.birthday);
          if (form.avatarUri) {
            data.append("avatar", {uri: form.avatarUri, type: "image/jpeg", name: "avatar.jpg"} as any);
          }
          const response = await userApi.updateUserProfile(data);
          const body = responseDataObject(response) || {};
          const avatar = firstString(body, "avatar") || main.userSummary.avatar;
          await saveUserProfile({userId, nickname, phone: main.userSummary.phone, avatar});
          setMain(state => ({...state, userSummary: {...state.userSummary, displayName: nickname, avatar}, route: "Tabs", message: "资料已更新"}));
          await refresh();
        } catch (error) {
          setMain(state => ({...state, message: errorMessage(error)}));
        } finally {
          setMain(state => ({...state, isSubmitting: false}));
        }
      },
      async logout() {
        const userId = main.userSummary.userId;
        setMain(state => ({...state, isLoggingOut: true, message: undefined}));
        if (userId) await userApi.setUserOfflineStatus(userId).catch(() => undefined);
        await authApi.logout().catch(() => undefined);
        await clearSession();
        setAuth(initialAuthState);
        setMain({...initialMainState, loggedOut: true});
      },
      handleRoomAction(action: string, payload?: Record<string, unknown>) {
        handleRoomAction(action, payload, setMain, refresh);
      },
      consumeMainMessage() {
        setMain(state => ({...state, message: undefined}));
      },
    }),
    [auth, main, refresh],
  );

  return {booting, auth, main, actions};
}

async function loadUserSummary(): Promise<UserSummary> {
  const session = currentSession();
  if (!session.userId) {
    return {userId: "", displayName: "会议用户", phone: "", avatar: "", online: false};
  }
  const profile = await userApi.getUserProfile(session.userId);
  const data = responseDataObject(profile) || {};
  const nickname = firstString(data, "nickname", "username", "name", "displayName") || session.nickname || "会议用户";
  const phone = firstString(data, "phone", "mobile") || session.phone || "";
  const avatar = firstString(data, "avatar") || session.avatar || "";
  await saveUserProfile({userId: session.userId, nickname, phone, avatar});
  return {userId: session.userId, displayName: nickname, phone, avatar, online: false};
}

async function preparePreJoin(meeting: MeetingSummary, setMain: React.Dispatch<React.SetStateAction<MainUiState>>) {
  setMain(state => ({...state, isSubmitting: true, message: undefined}));
  try {
    const session = currentSession();
    const response = await roomApi.joinMeeting({
      roomId: meeting.roomId || meeting.roomNo,
      roomNo: meeting.roomNo,
      userId: session.userId,
      audioEnabled: true,
      videoEnabled: true,
    }).catch(() => undefined);
    const joinedMeeting = parseMeeting(response) || meeting;
    setMain(state => ({...state, preJoinMeeting: joinedMeeting, route: "PreJoin", createForm: {...state.createForm, audioEnabled: true, videoEnabled: true}}));
  } finally {
    setMain(state => ({...state, isSubmitting: false}));
  }
}

function handleRoomAction(action: string, payload: Record<string, unknown> | undefined, setMain: React.Dispatch<React.SetStateAction<MainUiState>>, refresh: () => Promise<void>) {
  if (action === "leaveRoom") {
    setMain(state => ({...state, route: "Tabs", selectedTab: "Meetings", activeRoom: EMPTY_ROOM, message: "已离开会议"}));
    refresh();
    return;
  }
  if (action === "toggleAudio" || action === "toggleVideo" || action === "toggleHandRaised") {
    const enabled = payload?.enabled === true;
    setMain(state => {
      const localPeerId = state.activeRoom.participants.find(participant => participant.isLocal)?.peerId;
      return {
        ...state,
        activeRoom: {
          ...state.activeRoom,
          audioEnabled: action === "toggleAudio" ? enabled : state.activeRoom.audioEnabled,
          videoEnabled: action === "toggleVideo" ? enabled : state.activeRoom.videoEnabled,
          handRaised: action === "toggleHandRaised" ? enabled : state.activeRoom.handRaised,
          participants: state.activeRoom.participants.map(participant =>
            participant.peerId === localPeerId
              ? {
                  ...participant,
                  audioEnabled: action === "toggleAudio" ? enabled : participant.audioEnabled,
                  videoEnabled: action === "toggleVideo" ? enabled : participant.videoEnabled,
                  handRaised: action === "toggleHandRaised" ? enabled : participant.handRaised,
                }
              : participant,
          ),
        },
      };
    });
    return;
  }
  if (action === "openSheet") {
    const sheet = payload?.sheet === "Members" || payload?.sheet === "Chat" || payload?.sheet === "More" ? payload.sheet : undefined;
    let roomId = "";
    let localUserId = "";
    setMain(state => {
      roomId = state.activeRoom.meeting?.roomId || "";
      localUserId = state.userSummary.userId;
      return {
        ...state,
        activeRoom: {
          ...state.activeRoom,
          selectedSheet: sheet,
          participantsLoading: sheet === "Members" ? true : state.activeRoom.participantsLoading,
        },
      };
    });
    if (sheet === "Members" && roomId) {
      roomApi.fetchParticipantsList(roomId)
        .then(response => {
          const participants = parseParticipantList(response, localUserId);
          setMain(state => ({
            ...state,
            activeRoom: {
              ...state.activeRoom,
              participants: mergeParticipants(state.activeRoom.participants, participants).sort(sortParticipants),
              participantsTotal: participants.length || state.activeRoom.participantsTotal,
              participantsLoading: false,
            },
          }));
        })
        .catch(error => {
          setMain(state => ({...state, activeRoom: {...state.activeRoom, participantsLoading: false}, message: errorMessage(error)}));
        });
    }
    return;
  }
  if (action === "closeSheet") {
    setMain(state => ({...state, activeRoom: {...state.activeRoom, selectedSheet: undefined}}));
    return;
  }
  if (action === "closeMeeting") {
    setMain(state => {
      const roomId = state.activeRoom.meeting?.roomId;
      if (roomId) roomApi.closeMeeting(roomId).catch(() => undefined);
      return {...state, route: "Tabs", selectedTab: "Meetings", activeRoom: EMPTY_ROOM, message: "会议已关闭"};
    });
    refresh();
    return;
  }
  if (action === "toggleScreenShare" || action === "toggleCaptions" || action === "toggleHandRaised") {
    setMain(state => ({
      ...state,
      activeRoom: {
        ...state.activeRoom,
        screenSharing: action === "toggleScreenShare" ? !state.activeRoom.screenSharing : state.activeRoom.screenSharing,
        captionsEnabled: action === "toggleCaptions" ? !state.activeRoom.captionsEnabled : state.activeRoom.captionsEnabled,
        handRaised: action === "toggleHandRaised" ? !state.activeRoom.handRaised : state.activeRoom.handRaised,
      },
    }));
    return;
  }
  if (action === "selectAudioRoute") {
    const route = payload?.route === "Earpiece" ? "Earpiece" : "Speaker";
    setMain(state => ({...state, audioRoute: route, activeRoom: {...state.activeRoom, selectedAudioRoute: route}}));
    return;
  }
  if (action === "openMeetingSettings") {
    Alert.alert("会议设置", "移动端会议设置入口已保留。");
  }
}

function parseParticipantList(value: any, localUserId: string): RoomParticipant[] {
  return asArray(value).map(item => parseParticipantFromObject(item, localUserId)).filter(Boolean) as RoomParticipant[];
}

function parseParticipantFromObject(body: any, localUserId: string): RoomParticipant | undefined {
  if (!body || typeof body !== "object") return undefined;
  const userId = firstString(body, "userId", "id") || "";
  const roleCode = Number(body.role || 1);
  const statusCode = Number(body.status || 1);
  return {
    peerId: firstString(body, "peerId", "socketId") || userId,
    userId,
    username: firstString(body, "userName", "username", "name", "nickname") || "参会者",
    avatar: firstString(body, "avatar") || "",
    role: roleCode === 2 ? "Host" : "Member",
    roleLabel: roleCode === 2 ? "主持人" : "成员",
    status: statusCode === 1 ? "Online" : "Offline",
    statusLabel: statusCode === 1 ? "在线" : "离线",
    isLocal: Boolean(userId && userId === localUserId),
    audioEnabled: body.audioMuted !== true,
    videoEnabled: body.videoMuted !== true,
    handRaised: body.handRaised === true,
    speaking: body.speaking === true,
    joinedAt: firstString(body, "joinedAt") || "",
  };
}

function mergeParticipants(current: RoomParticipant[], fetched: RoomParticipant[]): RoomParticipant[] {
  const fetchedByUserId = new Map(fetched.filter(item => item.userId).map(item => [item.userId, item]));
  const merged = current.map(participant => {
    const fetchedParticipant = participant.userId ? fetchedByUserId.get(participant.userId) : undefined;
    return fetchedParticipant
      ? {...fetchedParticipant, peerId: participant.peerId || fetchedParticipant.peerId, isLocal: participant.isLocal || fetchedParticipant.isLocal, speaking: participant.speaking, handRaised: participant.handRaised}
      : participant;
  });
  const knownUserIds = new Set(merged.map(item => item.userId).filter(Boolean));
  return merged.concat(fetched.filter(item => item.userId && !knownUserIds.has(item.userId)));
}

function sortParticipants(a: RoomParticipant, b: RoomParticipant): number {
  if (a.role !== b.role) return a.role === "Host" ? -1 : 1;
  if (a.status !== b.status) return a.status === "Online" ? -1 : 1;
  if (a.isLocal !== b.isLocal) return a.isLocal ? -1 : 1;
  return a.username.localeCompare(b.username);
}

function parseMeetingList(value: any): MeetingSummary[] {
  return asArray(value).map(parseMeetingFromObject).filter(Boolean) as MeetingSummary[];
}

function parseMeeting(value: any): MeetingSummary | undefined {
  const data = responseDataObject(value) || value?.meeting || value;
  return parseMeetingFromObject(data);
}

function parseMeetingFromObject(body: any): MeetingSummary | undefined {
  if (!body || typeof body !== "object") return undefined;
  const host = body.host || {};
  const roomId = firstString(body, "roomId", "id") || "";
  const roomNo = firstString(body, "roomNo", "meetingNo") || "";
  if (!roomId && !roomNo) return undefined;
  return {
    roomId,
    roomNo,
    title: firstString(body, "title", "name") || "未命名会议",
    hostId: firstString(body, "hostId") || firstString(host, "id", "userId") || "",
    hostName: firstString(body, "hostName") || firstString(host, "nickname", "username", "name") || "",
    startTime: firstString(body, "startTime", "createdTime") || "",
    endTime: firstString(body, "endTime") || "",
    status: firstString(body, "status") || "",
    maxParticipants: toOptionalNumber(body.maxParticipants),
    participantCount: toOptionalNumber(body.participantCount),
    description: firstString(body, "description", "settings") || "",
    sfuServerUrl: firstString(body, "sfuServerUrl", "socketUrl", "url") || DEFAULT_SFU_SOCKET_URL,
  };
}

function buildLocalProducerPlaceholders(peerId: string, username: string, audioEnabled: boolean, videoEnabled: boolean): SfuProducerState[] {
  return [
    audioEnabled ? {id: "", peerId, userId: peerId, username, kind: "audio", paused: false, local: true} : undefined,
    videoEnabled ? {id: "", peerId, userId: peerId, username, kind: "video", paused: false, local: true} : undefined,
  ].filter(Boolean) as SfuProducerState[];
}

function validateAuth(state: AuthUiState): Partial<AuthUiState> | undefined {
  if (!state.agreeTerms) return {message: "请先同意用户协议和隐私政策"};
  if (!PHONE_REGEX.test(state.phone)) return {phoneError: "请输入正确的手机号", message: "请输入正确的手机号"};
  if (state.code.length !== CODE_LENGTH) return {codeError: "请输入6位验证码", message: "请输入正确的验证码"};
  return undefined;
}

function startCountdown(setAuth: React.Dispatch<React.SetStateAction<AuthUiState>>, countdownRef: React.MutableRefObject<ReturnType<typeof setInterval> | undefined>) {
  clearCountdown(countdownRef);
  setAuth(state => ({...state, countdownSeconds: CODE_COUNTDOWN_SECONDS}));
  countdownRef.current = setInterval(() => {
    setAuth(state => {
      if (state.countdownSeconds <= 1) {
        if (countdownRef.current) clearInterval(countdownRef.current);
        return {...state, countdownSeconds: 0};
      }
      return {...state, countdownSeconds: state.countdownSeconds - 1};
    });
  }, 1000);
}

function clearCountdown(countdownRef: React.MutableRefObject<ReturnType<typeof setInterval> | undefined>) {
  const countdown = countdownRef.current;
  if (countdown) clearInterval(countdown);
  countdownRef.current = undefined;
}

async function requestMediaPermissions() {
  if (Platform.OS !== "android") return {camera: true, audio: true};
  const result = await PermissionsAndroid.requestMultiple([
    PermissionsAndroid.PERMISSIONS.CAMERA,
    PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
  ]);
  return {
    camera: result[PermissionsAndroid.PERMISSIONS.CAMERA] === PermissionsAndroid.RESULTS.GRANTED,
    audio: result[PermissionsAndroid.PERMISSIONS.RECORD_AUDIO] === PermissionsAndroid.RESULTS.GRANTED,
  };
}

function permissionHint(camera: boolean, audio: boolean): string | undefined {
  if (!camera && !audio) return "需要相机和麦克风权限后才能进入会议";
  if (!camera) return "需要相机权限以完成本地视频预览";
  if (!audio) return "需要麦克风权限以完成本地音频检查";
  return undefined;
}

function parseOnline(value: any): boolean {
  const data = responseDataObject(value) || value;
  return data === true || data?.online === true || data?.status === 1 || data?.status === "1";
}

function isSuccessful(value: any): boolean {
  return value?.success === true;
}

function responseMessage(value: any): string | undefined {
  return firstString(value, "notification", "message");
}

function mapJoinFailure(message: string): string {
  if (message.includes("权限")) return "当前账号无权限加入该会议";
  if (message.includes("结束") || message.includes("关闭")) return "会议已结束，无法加入";
  if (message.includes("不存在") || message.toLowerCase().includes("not found")) return "会议不存在，请检查会议号";
  return "请输入正确的会议号";
}

function buildSettingsJson(form: CreateMeetingForm): string {
  return JSON.stringify({
    enableRecording: false,
    allowedCodecs: ["opus", "VP8"],
    enableWaitingRoom: false,
    disableCamera: !form.videoEnabled,
    muteAudio: !form.audioEnabled,
    description: form.description,
  });
}

function cleanMeetingNo(value: string): string {
  return value.replace(/[^a-zA-Z0-9]/g, "").slice(0, MEETING_NO_MAX_LENGTH);
}

function digits(value: string): string {
  return value.replace(/\D/g, "");
}

function formatDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function formatTime(date: Date): string {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function pad(value: number): string {
  return String(value).padStart(2, "0");
}

function toOptionalNumber(value: unknown): number | undefined {
  const next = Number(value);
  return Number.isFinite(next) ? next : undefined;
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "操作失败";
}

function wait(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
