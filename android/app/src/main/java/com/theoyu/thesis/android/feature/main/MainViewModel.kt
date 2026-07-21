package com.theoyu.thesis.android.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.theoyu.thesis.android.core.network.ApiResult
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.core.signaling.SocketIoClient
import com.theoyu.thesis.android.core.signaling.SocketIoConfig
import com.theoyu.thesis.android.core.signaling.SocketSubscription
import com.theoyu.thesis.android.data.auth.AuthRepository
import com.theoyu.thesis.android.data.meeting.RoomRepository
import com.theoyu.thesis.android.data.user.UserRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val socketIoClient: SocketIoClient,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    private val roomSocketSubscriptions = mutableListOf<SocketSubscription>()

    init {
        refresh()
        observeSocketConnection()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val userId = sessionStore.currentSession().userId.orEmpty()
            if (userId.isNotBlank()) {
                userRepository.setUserOnlineStatus(userId)
            }
            loadUserSummary()
            loadOnlineStatus()
            loadMeetingLists()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab, route = MainRoute.Tabs, message = null) }
        if (tab == MainTab.Profile) {
            viewModelScope.launch {
                loadUserSummary()
                loadOnlineStatus()
            }
        }
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

    fun updatePreJoinVideo(enabled: Boolean) {
        _uiState.update { it.copy(createForm = it.createForm.copy(videoEnabled = enabled)) }
    }

    fun updatePreJoinAudio(enabled: Boolean) {
        _uiState.update { it.copy(createForm = it.createForm.copy(audioEnabled = enabled)) }
    }

    fun updatePermissions(cameraGranted: Boolean, audioGranted: Boolean) {
        _uiState.update {
            it.copy(
                cameraPermissionGranted = cameraGranted,
                audioPermissionGranted = audioGranted,
                permissionHint = when {
                    !cameraGranted && !audioGranted -> "需要相机和麦克风权限后才能进入会议"
                    !cameraGranted -> "需要相机权限以完成本地视频预览"
                    !audioGranted -> "需要麦克风权限以完成本地音频检查"
                    else -> null
                },
            )
        }
    }

    fun updateAudioRoutes(routes: List<AudioRoute>) {
        val normalized = routes.ifEmpty { listOf(AudioRoute.Speaker) }.distinct()
        _uiState.update {
            it.copy(
                availableAudioRoutes = normalized,
                audioRoute = if (it.audioRoute in normalized) it.audioRoute else normalized.first(),
            )
        }
    }

    fun selectAudioRoute(route: AudioRoute) {
        _uiState.update { it.copy(audioRoute = route) }
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

    fun openProfileEditor() {
        _uiState.update {
            it.copy(
                profileEditOpen = true,
                profileEditForm = ProfileEditForm(nickname = it.userSummary.displayName),
                message = null,
            )
        }
    }

    fun dismissProfileEditor() {
        _uiState.update { it.copy(profileEditOpen = false, message = null) }
    }

    fun updateProfileNickname(value: String) {
        _uiState.update { it.copy(profileEditForm = it.profileEditForm.copy(nickname = value.take(TITLE_MAX_LENGTH))) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val userId = state.userSummary.userId
        val nickname = state.profileEditForm.nickname.trim()
        if (userId.isBlank()) {
            _uiState.update { it.copy(message = "缺少用户信息，请重新登录") }
            return
        }
        if (nickname.length < 2) {
            _uiState.update { it.copy(message = "昵称至少需要 2 个字符") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            when (val result = userRepository.updateUserProfile(mapOf("userId" to userId, "nickname" to nickname))) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            userSummary = it.userSummary.copy(displayName = nickname),
                            profileEditOpen = false,
                            message = "资料已更新",
                        )
                    }
                    loadUserSummary()
                }

                is ApiResult.Failure -> _uiState.update { it.copy(message = result.error.message) }
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun logout() {
        val userId = _uiState.value.userSummary.userId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, message = null) }
            if (userId.isNotBlank()) {
                userRepository.setUserOfflineStatus(userId)
            }
            authRepository.logout()
            sessionStore.clearSession()
            _uiState.update { it.copy(isLoggingOut = false, loggedOut = true) }
        }
    }

    fun enterMeetingFromPreview() {
        val meeting = _uiState.value.preJoinMeeting
        if (meeting == null) {
            _uiState.update { it.copy(message = "缺少会议信息，无法进入房间") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            runCatching {
                val session = sessionStore.currentSession()
                val roomId = meeting.roomId.ifBlank { meeting.roomNo }
                if (!socketIoClient.isConnected) {
                    socketIoClient.connect(
                        url = meeting.sfuServerUrl.ifBlank { DEFAULT_SFU_SOCKET_URL },
                        config = SocketIoConfig(),
                    )
                }
                setupRoomSocketListeners()
                val joinPayload = JSONObject()
                    .put("roomId", roomId)
                    .put("userId", session.userId.orEmpty())
                    .put("username", _uiState.value.userSummary.displayName)
                    .put("token", session.token.orEmpty())
                    .put("withMedia", true)
                socketIoClient.emit("joinRoom", joinPayload)
                val localPeerId = socketIoClient.connectionState.value.socketId ?: "local"
                val localParticipant = RoomParticipant(
                    peerId = localPeerId,
                    userId = session.userId.orEmpty(),
                    username = _uiState.value.userSummary.displayName,
                    isLocal = true,
                    audioEnabled = _uiState.value.createForm.audioEnabled,
                    videoEnabled = _uiState.value.createForm.videoEnabled,
                )
                _uiState.update {
                    it.copy(
                        route = MainRoute.Room,
                        activeRoom = RoomUiState(
                            meeting = meeting,
                            participants = listOf(localParticipant),
                            activeSpeakerPeerId = localPeerId,
                            socketConnected = socketIoClient.isConnected,
                            audioEnabled = it.createForm.audioEnabled,
                            videoEnabled = it.createForm.videoEnabled,
                            networkQuality = NetworkQuality.Excellent,
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "进入会议失败") }
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            val roomId = _uiState.value.activeRoom.meeting?.roomId.orEmpty()
            if (socketIoClient.isConnected && roomId.isNotBlank()) {
                runCatching {
                    socketIoClient.emit("leaveRoom", JSONObject().put("roomId", roomId))
                }
            }
            clearRoomSocketListeners()
            socketIoClient.disconnect()
            _uiState.update {
                it.copy(
                    route = MainRoute.Tabs,
                    selectedTab = MainTab.Meetings,
                    activeRoom = RoomUiState(),
                    message = "已离开会议",
                )
            }
        }
    }

    fun toggleRoomAudio() {
        val next = !_uiState.value.activeRoom.audioEnabled
        updateRoomLocalMedia(audioEnabled = next, videoEnabled = _uiState.value.activeRoom.videoEnabled)
        viewModelScope.launch {
            emitRoomMediaToggle("toggleAudio", "enabled", next)
        }
    }

    fun toggleRoomVideo() {
        val next = !_uiState.value.activeRoom.videoEnabled
        updateRoomLocalMedia(audioEnabled = _uiState.value.activeRoom.audioEnabled, videoEnabled = next)
        viewModelScope.launch {
            emitRoomMediaToggle("toggleVideo", "enabled", next)
        }
    }

    fun openRoomSheet(sheet: RoomSheet) {
        _uiState.update { it.copy(activeRoom = it.activeRoom.copy(selectedSheet = sheet)) }
    }

    fun closeRoomSheet() {
        _uiState.update { it.copy(activeRoom = it.activeRoom.copy(selectedSheet = null)) }
    }

    fun sendRoomMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return
        val state = _uiState.value
        val message = RoomChatMessage(
            id = "${System.currentTimeMillis()}",
            senderName = state.userSummary.displayName,
            content = trimmed,
            timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
            isLocal = true,
        )
        _uiState.update { it.copy(activeRoom = it.activeRoom.copy(chatMessages = it.activeRoom.chatMessages + message)) }
        viewModelScope.launch {
            runCatching {
                socketIoClient.emit(
                    "roomMessage",
                    JSONObject()
                        .put("roomId", state.activeRoom.meeting?.roomId.orEmpty())
                        .put("content", trimmed),
                )
            }
        }
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
                        userSummary = it.userSummary.copy(
                            userId = userId,
                            displayName = data?.firstString("nickname", "username", "name", "displayName")
                                ?: "用户 $userId",
                            phone = data?.firstString("phone", "mobile").orEmpty(),
                            avatar = data?.firstString("avatar").orEmpty(),
                        ),
                    )
                }
            }

            is ApiResult.Failure -> _uiState.update {
                it.copy(userSummary = it.userSummary.copy(userId = userId, displayName = "用户 $userId"))
            }
        }
    }

    private suspend fun loadOnlineStatus() {
        val userId = _uiState.value.userSummary.userId
        if (userId.isBlank()) return
        when (val result = userRepository.getUserOnlineStatus(userId)) {
            is ApiResult.Success -> {
                val online = result.data.responseDataObject()?.get("online")?.asBooleanOrNull() == true
                _uiState.update { it.copy(userSummary = it.userSummary.copy(online = online)) }
            }

            is ApiResult.Failure -> Unit
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

    private fun observeSocketConnection() {
        viewModelScope.launch {
            socketIoClient.connectionState.collectLatest { state ->
                _uiState.update {
                    it.copy(
                        activeRoom = it.activeRoom.copy(
                            socketConnected = state.connected,
                            reconnecting = state.reconnecting,
                            networkQuality = when {
                                !state.connected -> NetworkQuality.Offline
                                state.reconnecting -> NetworkQuality.Poor
                                else -> it.activeRoom.networkQuality
                            },
                            roomNotice = state.lastError ?: it.activeRoom.roomNotice,
                        ),
                    )
                }
            }
        }
    }

    private fun setupRoomSocketListeners() {
        clearRoomSocketListeners()
        roomSocketSubscriptions += socketIoClient.on("rtt") { args ->
            val rtt = args.firstJsonObject()?.optInt("rtt") ?: return@on
            _uiState.update {
                it.copy(
                    activeRoom = it.activeRoom.copy(
                        rttMillis = rtt,
                        networkQuality = when {
                            rtt < 120 -> NetworkQuality.Excellent
                            rtt < 300 -> NetworkQuality.Fair
                            else -> NetworkQuality.Poor
                        },
                    ),
                )
            }
        }
        roomSocketSubscriptions += socketIoClient.on("newPeer") { args ->
            val body = args.firstJsonObject() ?: return@on
            val participant = participantFromJson(body)
            _uiState.update {
                it.copy(activeRoom = it.activeRoom.copy(participants = it.activeRoom.participants.upsert(participant)))
            }
        }
        roomSocketSubscriptions += socketIoClient.on("peerLeft") { args ->
            val peerId = args.firstJsonObject()?.optString("peerId").orEmpty()
            _uiState.update {
                it.copy(activeRoom = it.activeRoom.copy(participants = it.activeRoom.participants.filterNot { p -> p.peerId == peerId }))
            }
        }
        roomSocketSubscriptions += socketIoClient.on("producerStateChanged") { args ->
            val body = args.firstJsonObject() ?: return@on
            val peerId = body.optString("peerId")
            val kind = body.optString("kind")
            val enabled = !body.optBoolean("paused", false)
            _uiState.update {
                it.copy(
                    activeRoom = it.activeRoom.copy(
                        participants = it.activeRoom.participants.map { participant ->
                            if (participant.peerId != peerId) {
                                participant
                            } else if (kind == "audio") {
                                participant.copy(audioEnabled = enabled, speaking = enabled)
                            } else {
                                participant.copy(videoEnabled = enabled)
                            }
                        },
                        activeSpeakerPeerId = if (kind == "audio" && enabled) peerId else it.activeRoom.activeSpeakerPeerId,
                    ),
                )
            }
        }
        roomSocketSubscriptions += socketIoClient.on("newProducer") { args ->
            val body = args.firstJsonObject() ?: return@on
            val participant = participantFromJson(body).copy(
                audioEnabled = body.optString("kind") != "video",
                videoEnabled = body.optString("kind") != "audio",
            )
            _uiState.update {
                it.copy(activeRoom = it.activeRoom.copy(participants = it.activeRoom.participants.upsert(participant)))
            }
        }
        roomSocketSubscriptions += socketIoClient.on("roomClosed") {
            _uiState.update { it.copy(activeRoom = it.activeRoom.copy(roomNotice = "会议已结束")) }
            leaveRoom()
        }
        roomSocketSubscriptions += socketIoClient.on("removedFromRoom") {
            _uiState.update { it.copy(activeRoom = it.activeRoom.copy(roomNotice = "已被移出会议")) }
            leaveRoom()
        }
    }

    private fun clearRoomSocketListeners() {
        roomSocketSubscriptions.forEach { it.dispose() }
        roomSocketSubscriptions.clear()
    }

    private suspend fun emitRoomMediaToggle(event: String, key: String, enabled: Boolean) {
        if (!socketIoClient.isConnected) return
        runCatching {
            socketIoClient.emit(
                event,
                JSONObject()
                    .put("roomId", _uiState.value.activeRoom.meeting?.roomId.orEmpty())
                    .put(key, enabled),
            )
        }
    }

    private fun updateRoomLocalMedia(audioEnabled: Boolean, videoEnabled: Boolean) {
        _uiState.update {
            val localPeerId = it.activeRoom.participants.firstOrNull(RoomParticipant::isLocal)?.peerId
            it.copy(
                activeRoom = it.activeRoom.copy(
                    audioEnabled = audioEnabled,
                    videoEnabled = videoEnabled,
                    participants = it.activeRoom.participants.map { participant ->
                        if (participant.peerId == localPeerId) {
                            participant.copy(audioEnabled = audioEnabled, videoEnabled = videoEnabled)
                        } else {
                            participant
                        }
                    },
                ),
            )
        }
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
                sfuServerUrl = body.firstString("sfuServerUrl", "socketUrl", "url").orEmpty(),
            )
        }

    private fun participantFromJson(body: JSONObject): RoomParticipant =
        RoomParticipant(
            peerId = body.optString("peerId", body.optString("socketId", "${System.currentTimeMillis()}")),
            userId = body.optString("userId"),
            username = body.optString("username", body.optString("name", "参会者")),
            audioEnabled = !body.optBoolean("audioPaused", false),
            videoEnabled = !body.optBoolean("videoPaused", false),
            speaking = body.optBoolean("speaking", false),
        )

    private fun List<RoomParticipant>.upsert(participant: RoomParticipant): List<RoomParticipant> =
        if (any { it.peerId == participant.peerId }) {
            map { current ->
                if (current.peerId == participant.peerId) {
                    current.copy(
                        userId = participant.userId.ifBlank { current.userId },
                        username = participant.username.ifBlank { current.username },
                        audioEnabled = participant.audioEnabled,
                        videoEnabled = participant.videoEnabled,
                        speaking = participant.speaking,
                    )
                } else {
                    current
                }
            }
        } else {
            this + participant
        }

    private fun Array<Any>.firstJsonObject(): JSONObject? =
        firstOrNull()?.let { value ->
            when (value) {
                is JSONObject -> value
                is Map<*, *> -> JSONObject(value)
                else -> runCatching { JSONObject(value.toString()) }.getOrNull()
            }
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

    private fun JsonElement.asBooleanOrNull(): Boolean? =
        if (isJsonNull) null else runCatching { asBoolean }.getOrNull()

    private companion object {
        const val DEFAULT_MAX_PARTICIPANTS = 15
        const val DESCRIPTION_MAX_LENGTH = 500
        const val MEETING_NO_MAX_LENGTH = 12
        const val MEETING_NO_MIN_LENGTH = 4
        const val TITLE_MAX_LENGTH = 50
        const val DEFAULT_SFU_SOCKET_URL = "http://10.0.2.2:3000"
        val API_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val DISPLAY_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
