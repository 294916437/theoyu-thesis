package com.theoyu.thesis.android.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.theoyu.thesis.android.core.network.ApiResult
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.data.meeting.RoomRepository
import com.theoyu.thesis.android.data.user.UserRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            loadUserSummary()
            loadMeetingLists()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab, route = MainRoute.Tabs, message = null) }
    }

    fun openCreateMeeting(type: MeetingCreateType = MeetingCreateType.Instant) {
        val defaultTitle = "${_uiState.value.userSummary.displayName}的会议"
        _uiState.update {
            it.copy(
                route = MainRoute.CreateMeeting,
                createForm = CreateMeetingForm(title = defaultTitle, type = type),
                message = null,
            )
        }
    }

    fun openJoinMeeting(initialMeetingNo: String = "") {
        _uiState.update {
            it.copy(
                route = MainRoute.JoinMeeting,
                joinMeetingNo = initialMeetingNo.filter(Char::isDigit).take(MEETING_NO_MAX_LENGTH),
                joinError = null,
                validatedMeeting = null,
                message = null,
            )
        }
    }

    fun backToTabs() {
        _uiState.update { it.copy(route = MainRoute.Tabs, message = null) }
    }

    fun updateHomeMeetingNo(value: String) {
        _uiState.update { it.copy(homeMeetingNo = cleanMeetingNo(value)) }
    }

    fun updateJoinMeetingNo(value: String) {
        _uiState.update {
            it.copy(
                joinMeetingNo = cleanMeetingNo(value),
                joinError = null,
                validatedMeeting = null,
                message = null,
            )
        }
    }

    fun updateCreateTitle(value: String) {
        _uiState.update { it.copy(createForm = it.createForm.copy(title = value.take(TITLE_MAX_LENGTH))) }
    }

    fun updateCreateType(type: MeetingCreateType) {
        _uiState.update { it.copy(createForm = it.createForm.copy(type = type)) }
    }

    fun updateCreateDate(date: LocalDate) {
        _uiState.update { it.copy(createForm = it.createForm.copy(startDate = date)) }
    }

    fun updateCreateTime(time: LocalTime) {
        _uiState.update { it.copy(createForm = it.createForm.copy(startTime = time.withSecond(0).withNano(0))) }
    }

    fun updateCreateDescription(value: String) {
        _uiState.update { it.copy(createForm = it.createForm.copy(description = value.take(DESCRIPTION_MAX_LENGTH))) }
    }

    fun updateCreateAudio(enabled: Boolean) {
        _uiState.update { it.copy(createForm = it.createForm.copy(audioEnabled = enabled)) }
    }

    fun updateCreateVideo(enabled: Boolean) {
        _uiState.update { it.copy(createForm = it.createForm.copy(videoEnabled = enabled)) }
    }

    fun createMeeting() {
        val form = _uiState.value.createForm
        val title = form.title.trim()
        if (title.length < 2) {
            _uiState.update { it.copy(message = "会议标题至少需要 2 个字符") }
            return
        }

        val startDateTime = LocalDateTime.of(form.startDate, form.startTime)
        if (form.type == MeetingCreateType.Scheduled && !startDateTime.isAfter(LocalDateTime.now())) {
            _uiState.update { it.copy(message = "预约会议开始时间必须晚于当前时间") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            val request = mapOf(
                "title" to title,
                "type" to form.type.apiValue,
                "maxParticipants" to DEFAULT_MAX_PARTICIPANTS,
                "startTime" to startDateTime.format(API_DATE_TIME_FORMATTER),
                "settings" to buildSettingsJson(form),
            )

            when (val result = roomRepository.createMeeting(request)) {
                is ApiResult.Success -> {
                    val meeting = parseMeeting(result.data)
                    _uiState.update {
                        it.copy(
                            route = MainRoute.PreJoin,
                            preJoinMeeting = meeting ?: MeetingSummary(
                                roomId = "",
                                roomNo = "",
                                title = title,
                                startTime = startDateTime.format(DISPLAY_DATE_TIME_FORMATTER),
                                description = form.description,
                            ),
                            selectedTab = MainTab.Meetings,
                            message = "会议创建成功",
                        )
                    }
                    loadMeetingLists()
                }

                is ApiResult.Failure -> _uiState.update { it.copy(message = result.error.message) }
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun validateJoinMeeting() {
        val meetingNo = _uiState.value.joinMeetingNo.trim()
        if (meetingNo.length < MEETING_NO_MIN_LENGTH) {
            _uiState.update { it.copy(joinError = JoinMeetingError.Invalid) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, joinError = null, validatedMeeting = null, message = null) }
            when (val result = roomRepository.fetchMeetingDetail(meetingNo)) {
                is ApiResult.Success -> {
                    val meeting = parseMeeting(result.data)
                    val status = meeting?.status.orEmpty()
                    _uiState.update {
                        when {
                            meeting == null -> it.copy(joinError = JoinMeetingError.NotFound)
                            status == "已结束" || status.equals("ended", ignoreCase = true) || status == "3" ->
                                it.copy(joinError = JoinMeetingError.Ended)
                            else -> it.copy(validatedMeeting = meeting)
                        }
                    }
                }

                is ApiResult.Failure -> _uiState.update {
                    it.copy(joinError = mapJoinFailure(result.error.statusCode, result.error.message))
                }
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun joinValidatedMeeting() {
        val meeting = _uiState.value.validatedMeeting ?: run {
            validateJoinMeeting()
            return
        }
        val roomId = meeting.roomId.ifBlank { meeting.roomNo }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            when (val result = roomRepository.joinMeeting(mapOf("roomId" to roomId))) {
                is ApiResult.Success -> {
                    val joinedMeeting = parseMeeting(result.data) ?: meeting
                    _uiState.update {
                        it.copy(
                            route = MainRoute.PreJoin,
                            preJoinMeeting = joinedMeeting.copy(
                                title = joinedMeeting.title.ifBlank { meeting.title },
                                roomNo = joinedMeeting.roomNo.ifBlank { meeting.roomNo },
                            ),
                            message = null,
                        )
                    }
                }

                is ApiResult.Failure -> _uiState.update {
                    it.copy(joinError = mapJoinFailure(result.error.statusCode, result.error.message))
                }
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun loadUserSummary() {
        val session = sessionStore.currentSession()
        val userId = session.userId.orEmpty()
        if (userId.isBlank()) {
            _uiState.update { it.copy(userSummary = UserSummary()) }
            return
        }

        when (val result = userRepository.getUserProfile(userId)) {
            is ApiResult.Success -> {
                val data = result.data.responseDataObject()
                _uiState.update {
                    it.copy(
                        userSummary = UserSummary(
                            userId = userId,
                            displayName = data?.firstString("nickname", "username", "name", "displayName")
                                ?: "用户 $userId",
                            phone = data?.firstString("phone", "mobile").orEmpty(),
                        ),
                    )
                }
            }

            is ApiResult.Failure -> _uiState.update {
                it.copy(userSummary = UserSummary(userId = userId, displayName = "用户 $userId"))
            }
        }
    }

    private suspend fun loadMeetingLists() {
        val upcoming = when (val result = roomRepository.fetchUpcomingMeetings()) {
            is ApiResult.Success -> parseMeetingList(result.data)
            is ApiResult.Failure -> emptyList()
        }
        val recent = when (val result = roomRepository.fetchRecentMeetings()) {
            is ApiResult.Success -> parseMeetingList(result.data)
            is ApiResult.Failure -> emptyList()
        }
        _uiState.update { it.copy(upcomingMeetings = upcoming, recentMeetings = recent) }
    }

    private fun cleanMeetingNo(value: String): String =
        value.filter(Char::isDigit).take(MEETING_NO_MAX_LENGTH)

    private fun parseMeetingList(element: JsonElement): List<MeetingSummary> {
        val body = element.asJsonObjectOrNull()
        val candidates = listOfNotNull(
            body?.get("data"),
            body?.get("list"),
            body?.get("records"),
            body?.get("rows"),
            body?.get("items"),
        )
        val array = candidates.firstNotNullOfOrNull { it.asJsonArrayOrNull() }
            ?: body?.getAsJsonObjectOrNull("data")?.let { data ->
                listOfNotNull(
                    data.get("list"),
                    data.get("records"),
                    data.get("rows"),
                    data.get("items"),
                ).firstNotNullOfOrNull { it.asJsonArrayOrNull() }
            }
            ?: element.asJsonArrayOrNull()
            ?: JsonArray()
        return array.mapNotNull(::parseMeetingFromObject)
    }

    private fun parseMeeting(element: JsonElement): MeetingSummary? {
        val data = element.responseDataObject()
            ?: element.asJsonObjectOrNull()?.getAsJsonObjectOrNull("meeting")
            ?: element.asJsonObjectOrNull()
        return data?.let(::parseMeetingFromObject)
    }

    private fun parseMeetingFromObject(element: JsonElement): MeetingSummary? =
        element.asJsonObjectOrNull()?.let { body ->
            val host = body.getAsJsonObjectOrNull("host")
            val roomId = body.firstString("roomId", "id").orEmpty()
            val roomNo = body.firstString("roomNo", "meetingNo").orEmpty()
            val title = body.firstString("title", "name") ?: "未命名会议"
            MeetingSummary(
                roomId = roomId,
                roomNo = roomNo,
                title = title,
                hostName = body.firstString("hostName") ?: host?.firstString("nickname", "username", "name").orEmpty(),
                startTime = body.firstString("startTime", "createdTime").orEmpty(),
                endTime = body.firstString("endTime").orEmpty(),
                status = body.get("status")?.asStringOrNull().orEmpty(),
                maxParticipants = body.get("maxParticipants")?.asIntOrNull(),
                participantCount = body.get("participantCount")?.asIntOrNull(),
                description = body.firstString("description", "settings").orEmpty(),
            )
        }

    private fun mapJoinFailure(statusCode: Int?, message: String): JoinMeetingError =
        when {
            statusCode == 403 || message.contains("权限") -> JoinMeetingError.Forbidden
            message.contains("结束") || message.contains("关闭") -> JoinMeetingError.Ended
            statusCode == 404 || message.contains("不存在") || message.contains("not found", ignoreCase = true) ->
                JoinMeetingError.NotFound
            else -> JoinMeetingError.Invalid
        }

    private fun buildSettingsJson(form: CreateMeetingForm): String =
        """{"enableRecording":false,"allowedCodecs":["opus","VP8"],"enableWaitingRoom":false,"disableCamera":${!form.videoEnabled},"muteAudio":${!form.audioEnabled},"description":"${form.description.escapeJson()}"}"""

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun JsonElement.responseDataObject(): JsonObject? =
        asJsonObjectOrNull()?.getAsJsonObjectOrNull("data")

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        if (isJsonObject) asJsonObject else null

    private fun JsonElement.asJsonArrayOrNull(): JsonArray? =
        if (isJsonArray) asJsonArray else null

    private fun JsonObject.getAsJsonObjectOrNull(memberName: String): JsonObject? =
        get(memberName)?.asJsonObjectOrNull()

    private fun JsonObject.firstString(vararg names: String): String? =
        names.firstNotNullOfOrNull { name -> get(name)?.asStringOrNull()?.takeIf(String::isNotBlank) }

    private fun JsonElement.asStringOrNull(): String? =
        if (isJsonNull) null else runCatching { asString }.getOrNull()

    private fun JsonElement.asIntOrNull(): Int? =
        if (isJsonNull) null else runCatching { asInt }.getOrNull()

    private companion object {
        const val DEFAULT_MAX_PARTICIPANTS = 15
        const val DESCRIPTION_MAX_LENGTH = 500
        const val MEETING_NO_MAX_LENGTH = 12
        const val MEETING_NO_MIN_LENGTH = 4
        const val TITLE_MAX_LENGTH = 50
        val API_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val DISPLAY_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
