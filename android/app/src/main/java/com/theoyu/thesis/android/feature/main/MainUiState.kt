package com.theoyu.thesis.android.feature.main

import java.time.LocalDate
import java.time.LocalTime
import org.webrtc.VideoTrack

data class MainUiState(
    val selectedTab: MainTab = MainTab.Home,
    val route: MainRoute = MainRoute.Tabs,
    val previousRoute: MainRoute = MainRoute.Tabs,
    val userSummary: UserSummary = UserSummary(),
    val upcomingMeetings: List<MeetingSummary> = emptyList(),
    val recentMeetings: List<MeetingSummary> = emptyList(),
    val homeMeetingNo: String = "",
    val joinMeetingNo: String = "",
    val joinError: JoinMeetingError? = null,
    val validatedMeeting: MeetingSummary? = null,
    val preJoinMeeting: MeetingSummary? = null,
    val activeRoom: RoomUiState = RoomUiState(),
    val createForm: CreateMeetingForm = CreateMeetingForm(),
    val profileEditForm: ProfileEditForm = ProfileEditForm(),
    val profileEditOpen: Boolean = false,
    val audioRoute: AudioRoute = AudioRoute.Speaker,
    val availableAudioRoutes: List<AudioRoute> = listOf(AudioRoute.Speaker),
    val cameraPermissionGranted: Boolean = false,
    val audioPermissionGranted: Boolean = false,
    val permissionHint: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoggingOut: Boolean = false,
    val message: String? = null,
    val loggedOut: Boolean = false,
    val screenShareRequestId: Long? = null,
)

enum class MainTab(
    val label: String,
    val iconText: String,
) {
    Home("首页", "首"),
    Meetings("会议", "会"),
    Profile("我的", "我"),
}

sealed interface MainRoute {
    data object Tabs : MainRoute
    data object CreateMeeting : MainRoute
    data object JoinMeeting : MainRoute
    data object PreJoin : MainRoute
    data object Room : MainRoute
}

data class UserSummary(
    val userId: String = "",
    val displayName: String = "会议用户",
    val phone: String = "",
    val avatar: String = "",
    val online: Boolean = false,
)

data class MeetingSummary(
    val roomId: String,
    val roomNo: String,
    val title: String,
    val hostId: String = "",
    val hostName: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "",
    val maxParticipants: Int? = null,
    val participantCount: Int? = null,
    val description: String = "",
    val sfuServerUrl: String = "",
)

data class RoomUiState(
    val meeting: MeetingSummary? = null,
    val participants: List<RoomParticipant> = emptyList(),
    val chatMessages: List<RoomChatMessage> = emptyList(),
    val activeSpeakerPeerId: String? = null,
    val networkQuality: NetworkQuality = NetworkQuality.Unknown,
    val socketConnected: Boolean = false,
    val reconnecting: Boolean = false,
    val rttMillis: Int? = null,
    val audioEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val selectedSheet: RoomSheet? = null,
    val roomNotice: String? = null,
    val participantsLoading: Boolean = false,
    val participantsTotal: Int = 0,
    val handRaised: Boolean = false,
    val screenSharing: Boolean = false,
    val screenSharePeerId: String? = null,
    val captionsEnabled: Boolean = false,
    val mediaState: RoomMediaState = RoomMediaState(),
)

data class RoomMediaState(
    val phase: SfuMediaPhase = SfuMediaPhase.Idle,
    val routerRtpCapabilitiesJson: String = "",
    val sendTransport: SfuTransportState? = null,
    val recvTransport: SfuTransportState? = null,
    val localProducers: List<SfuProducerState> = emptyList(),
    val remoteProducers: List<SfuProducerState> = emptyList(),
    val consumers: List<SfuConsumerState> = emptyList(),
    val localVideoTrack: VideoTrack? = null,
    val remoteVideoTracks: Map<String, VideoTrack?> = emptyMap(),
    val error: String? = null,
    val mediaEngineReady: Boolean = false,
)

enum class SfuMediaPhase(
    val label: String,
) {
    Idle("未开始"),
    Joining("加入房间"),
    RouterReady("路由能力已获取"),
    TransportsReady("传输已创建"),
    Consuming("订阅远端流"),
    AwaitingMediaEngine("等待本地媒体引擎"),
    Connected("媒体信令已连接"),
    Failed("媒体信令失败"),
}

data class SfuTransportState(
    val id: String,
    val direction: SfuTransportDirection,
    val connected: Boolean = false,
    val iceParametersJson: String = "",
    val iceCandidatesJson: String = "",
    val dtlsParametersJson: String = "",
    val sctpParametersJson: String = "",
)

enum class SfuTransportDirection(
    val label: String,
) {
    Send("发送"),
    Recv("接收"),
}

data class SfuProducerState(
    val id: String,
    val peerId: String,
    val userId: String = "",
    val username: String = "",
    val kind: String,
    val paused: Boolean = false,
    val local: Boolean = false,
    val source: String = "",
)

data class SfuConsumerState(
    val id: String,
    val producerId: String,
    val kind: String,
    val peerId: String = "",
    val resumed: Boolean = false,
    val producerPaused: Boolean = false,
)

data class RoomParticipant(
    val peerId: String,
    val userId: String = "",
    val username: String = "参会者",
    val avatar: String = "",
    val role: ParticipantRole = ParticipantRole.Member,
    val status: ParticipantStatus = ParticipantStatus.Online,
    val isLocal: Boolean = false,
    val audioEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val handRaised: Boolean = false,
    val speaking: Boolean = false,
    val joinedAt: String = "",
    val leftAt: String = "",
)

enum class ParticipantRole(
    val code: Int,
    val label: String,
) {
    Member(1, "成员"),
    Host(2, "主持人"),
}

enum class ParticipantStatus(
    val code: Int,
    val label: String,
) {
    Online(1, "在线"),
    Offline(2, "离线"),
}

data class RoomChatMessage(
    val id: String,
    val senderName: String,
    val content: String,
    val timestamp: String,
    val isLocal: Boolean = false,
)

enum class NetworkQuality(
    val label: String,
) {
    Excellent("网络良好"),
    Fair("网络一般"),
    Poor("网络较差"),
    Offline("未连接"),
    Unknown("检测中"),
}

enum class RoomSheet(
    val title: String,
) {
    Members("成员"),
    Chat("聊天"),
    More("更多"),
}

data class CreateMeetingForm(
    val title: String = "",
    val type: MeetingCreateType = MeetingCreateType.Instant,
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().plusMinutes(15).withSecond(0).withNano(0),
    val description: String = "",
    val audioEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
)

data class ProfileEditForm(
    val nickname: String = "",
    val phone: String = "",
    val avatar: String = "",
)

enum class MeetingCreateType(
    val label: String,
    val apiValue: Int,
) {
    Instant("立即会议", 1),
    Scheduled("预约会议", 2),
}

enum class JoinMeetingError(
    val message: String,
) {
    NotFound("会议不存在，请检查会议号"),
    Ended("会议已结束，无法加入"),
    Forbidden("当前账号无权限加入该会议"),
    Invalid("请输入正确的会议号"),
}

enum class AudioRoute(
    val label: String,
) {
    Speaker("扬声器"),
    Earpiece("听筒"),
}
