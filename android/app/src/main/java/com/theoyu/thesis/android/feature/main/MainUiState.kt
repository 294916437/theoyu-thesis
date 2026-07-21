package com.theoyu.thesis.android.feature.main

import java.time.LocalDate
import java.time.LocalTime

data class MainUiState(
    val selectedTab: MainTab = MainTab.Home,
    val route: MainRoute = MainRoute.Tabs,
    val userSummary: UserSummary = UserSummary(),
    val upcomingMeetings: List<MeetingSummary> = emptyList(),
    val recentMeetings: List<MeetingSummary> = emptyList(),
    val homeMeetingNo: String = "",
    val joinMeetingNo: String = "",
    val joinError: JoinMeetingError? = null,
    val validatedMeeting: MeetingSummary? = null,
    val preJoinMeeting: MeetingSummary? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val createForm: CreateMeetingForm = CreateMeetingForm(),
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
}

data class UserSummary(
    val userId: String = "",
    val displayName: String = "会议用户",
    val phone: String = "",
)

data class MeetingSummary(
    val roomId: String,
    val roomNo: String,
    val title: String,
    val hostName: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "",
    val maxParticipants: Int? = null,
    val participantCount: Int? = null,
    val description: String = "",
)

data class CreateMeetingForm(
    val title: String = "",
    val type: MeetingCreateType = MeetingCreateType.Instant,
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().plusMinutes(15).withSecond(0).withNano(0),
    val description: String = "",
    val audioEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
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
