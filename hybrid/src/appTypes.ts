import type {RoomState} from "./types";

export type MainTab = "Home" | "Meetings" | "Profile";
export type MainRoute = "Tabs" | "CreateMeeting" | "JoinMeeting" | "PreJoin" | "Room" | "EditProfile";
export type MeetingCreateType = "Instant" | "Scheduled";
export type AudioRoute = "Speaker" | "Earpiece";

export type AuthSession = {
  token?: string;
  userId?: string;
  nickname?: string;
  phone?: string;
  avatar?: string;
};

export type AuthUiState = {
  phone: string;
  code: string;
  agreeTerms: boolean;
  loading: boolean;
  sendingCode: boolean;
  countdownSeconds: number;
  phoneError?: string;
  codeError?: string;
  message?: string;
  authenticated: boolean;
};

export type UserSummary = {
  userId: string;
  displayName: string;
  phone: string;
  avatar: string;
  online: boolean;
};

export type UserProfile = {
  userId: string;
  avatar: string;
  nickname: string;
  userAppId: string;
  sex: number;
  phone: string;
  age: number;
  birthday?: string;
  backgroundImg: string;
  introduction: string;
};

export type MeetingSummary = {
  roomId: string;
  roomNo: string;
  title: string;
  hostId: string;
  hostName: string;
  startTime: string;
  endTime: string;
  status: string;
  maxParticipants?: number;
  participantCount?: number;
  description: string;
  sfuServerUrl: string;
};

export type CreateMeetingForm = {
  title: string;
  type: MeetingCreateType;
  startDate: string;
  startTime: string;
  description: string;
  audioEnabled: boolean;
  videoEnabled: boolean;
};

export type ProfileEditForm = {
  nickname: string;
  avatarUri?: string;
  sex: number;
  birthday?: string;
  introduction: string;
};

export type MainUiState = {
  selectedTab: MainTab;
  route: MainRoute;
  userSummary: UserSummary;
  userProfile?: UserProfile;
  upcomingMeetings: MeetingSummary[];
  recentMeetings: MeetingSummary[];
  homeMeetingNo: string;
  joinMeetingNo: string;
  joinError?: string;
  validatedMeeting?: MeetingSummary;
  preJoinMeeting?: MeetingSummary;
  activeRoom: RoomState;
  createForm: CreateMeetingForm;
  profileEditForm: ProfileEditForm;
  audioRoute: AudioRoute;
  availableAudioRoutes: AudioRoute[];
  cameraPermissionGranted: boolean;
  audioPermissionGranted: boolean;
  permissionHint?: string;
  isLoading: boolean;
  isSubmitting: boolean;
  isLoggingOut: boolean;
  message?: string;
  loggedOut: boolean;
};
