export type RoomParticipant = {
  peerId: string;
  userId: string;
  username: string;
  avatar?: string;
  role: "Host" | "Member";
  roleLabel: string;
  status: "Online" | "Offline";
  statusLabel: string;
  isLocal: boolean;
  audioEnabled: boolean;
  videoEnabled: boolean;
  handRaised: boolean;
  speaking: boolean;
  joinedAt?: string;
};

export type RoomChatMessage = {
  id: string;
  senderName: string;
  content: string;
  timestamp: string;
  isLocal: boolean;
};

export type SfuTransportState = {
  id: string;
  direction: "Send" | "Recv";
  connected: boolean;
  iceParametersJson: string;
  iceCandidatesJson: string;
  dtlsParametersJson: string;
  sctpParametersJson: string;
};

export type SfuProducerState = {
  id: string;
  peerId: string;
  userId: string;
  username: string;
  kind: "audio" | "video" | string;
  paused: boolean;
  local: boolean;
};

export type SfuConsumerState = {
  id: string;
  producerId: string;
  kind: "audio" | "video" | string;
  peerId: string;
  resumed: boolean;
  producerPaused: boolean;
};

export type RoomState = {
  currentUserId?: string;
  currentPeerId?: string;
  currentUsername?: string;
  authToken?: string;
  meeting?: {
    roomId: string;
    roomNo: string;
    title: string;
    hostId: string;
    hostName: string;
    sfuServerUrl: string;
  };
  participants: RoomParticipant[];
  chatMessages: RoomChatMessage[];
  activeSpeakerPeerId?: string;
  networkQuality: string;
  networkQualityLabel: string;
  socketConnected: boolean;
  reconnecting: boolean;
  rttMillis?: number;
  audioEnabled: boolean;
  videoEnabled: boolean;
  selectedSheet?: "Members" | "Chat" | "More";
  roomNotice?: string;
  participantsLoading: boolean;
  participantsTotal: number;
  handRaised: boolean;
  screenSharing: boolean;
  captionsEnabled: boolean;
  mediaState: {
    phase: string;
    phaseLabel: string;
    routerRtpCapabilitiesJson: string;
    sendTransport?: SfuTransportState;
    recvTransport?: SfuTransportState;
    localProducers: SfuProducerState[];
    remoteProducers: SfuProducerState[];
    consumers: SfuConsumerState[];
    error?: string;
    mediaEngineReady: boolean;
  };
  availableAudioRoutes: string[];
  selectedAudioRoute: string;
};
