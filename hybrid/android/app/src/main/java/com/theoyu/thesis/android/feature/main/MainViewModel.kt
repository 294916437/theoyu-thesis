package com.theoyu.thesis.android.feature.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.theoyu.thesis.android.core.network.ApiResult
import com.theoyu.thesis.android.core.sfu.RoomMediaEngine
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.core.signaling.SocketIoClient
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
    private val appContext: Context,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val socketIoClient: SocketIoClient,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    private val roomSocketSubscriptions = mutableListOf<SocketSubscription>()
    private val roomMediaEngine = RoomMediaEngine(
        context = appContext,
        socketIoClient = socketIoClient,
        onMediaStateChanged = { mediaState ->
            _uiState.update { state -> state.copy(activeRoom = state.activeRoom.copy(mediaState = mediaState)) }
        },
        onMessageChanged = { message ->
            _uiState.update { state -> state.copy(message = message) }
        },
        onLocalPreviewChanged = {},
        onRemoteVideoTrackChanged = { _, _ -> },
    )

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
                    sessionStore.saveUserProfile(
                        userId = userId,
                        nickname = nickname,
                        phone = _uiState.value.userSummary.phone,
                        avatar = _uiState.value.userSummary.avatar,
                    )
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
                val localPeerId = session.userId.orEmpty().ifBlank { socketIoClient.connectionState.value.socketId ?: "local" }
                val localParticipant = RoomParticipant(
                    peerId = localPeerId,
                    userId = session.userId.orEmpty(),
                    username = _uiState.value.userSummary.displayName,
                    role = if (meeting.hostId == session.userId.orEmpty()) ParticipantRole.Host else ParticipantRole.Member,
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
                            mediaState = RoomMediaState(
                                phase = SfuMediaPhase.Joining,
                                localProducers = buildLocalProducerPlaceholders(
                                    peerId = session.userId.orEmpty(),
                                    username = _uiState.value.userSummary.displayName,
                                    audioEnabled = it.createForm.audioEnabled,
                                    videoEnabled = it.createForm.videoEnabled,
                                ),
                            ),
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
            roomMediaEngine.closeSession()
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
        roomMediaEngine.toggleAudio(next)
        viewModelScope.launch {
            emitRoomMediaToggle("toggleAudio", "enabled", next)
        }
    }

    fun toggleRoomVideo() {
        val next = !_uiState.value.activeRoom.videoEnabled
        updateRoomLocalMedia(audioEnabled = _uiState.value.activeRoom.audioEnabled, videoEnabled = next)
        roomMediaEngine.toggleVideo(next)
        viewModelScope.launch {
            emitRoomMediaToggle("toggleVideo", "enabled", next)
        }
    }

    fun openRoomSheet(sheet: RoomSheet) {
        _uiState.update { it.copy(activeRoom = it.activeRoom.copy(selectedSheet = sheet)) }
        if (sheet == RoomSheet.Members) {
            refreshRoomParticipants()
        }
    }

    fun closeRoomSheet() {
        _uiState.update { it.copy(activeRoom = it.activeRoom.copy(selectedSheet = null)) }
    }

    fun refreshRoomParticipants() {
        val roomId = _uiState.value.activeRoom.meeting?.roomId.orEmpty()
        if (roomId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(activeRoom = it.activeRoom.copy(participantsLoading = true)) }
            when (val result = roomRepository.fetchParticipantsList(roomId = roomId, status = "1", page = 1, size = 100)) {
                is ApiResult.Success -> {
                    val participants = parseParticipantList(result.data)
                    val total = result.data.asJsonObjectOrNull()?.get("totalCount")?.asIntOrNull() ?: participants.size
                    _uiState.update {
                        val merged = mergeParticipants(it.activeRoom.participants, participants)
                        it.copy(
                            activeRoom = it.activeRoom.copy(
                                participants = merged.sortedForRoom(),
                                participantsTotal = total,
                                participantsLoading = false,
                            ),
                        )
                    }
                }

                is ApiResult.Failure -> _uiState.update {
                    it.copy(
                        activeRoom = it.activeRoom.copy(participantsLoading = false),
                        message = result.error.message,
                    )
                }
            }
        }
    }

    fun hostToggleParticipantAudio(participant: RoomParticipant) {
        hostControlParticipant(
            participant = participant,
            event = "hostToggleAudio",
            enabled = !participant.audioEnabled,
            successMessage = if (participant.audioEnabled) "已静音 ${participant.username}" else "已允许 ${participant.username} 开麦",
        )
    }

    fun hostToggleParticipantVideo(participant: RoomParticipant) {
        hostControlParticipant(
            participant = participant,
            event = "hostToggleVideo",
            enabled = !participant.videoEnabled,
            successMessage = if (participant.videoEnabled) "已关闭 ${participant.username} 摄像头" else "已允许 ${participant.username} 开启摄像头",
        )
    }

    fun removeParticipant(participant: RoomParticipant) {
        val targetPeerId = participant.peerId.ifBlank { participant.userId }
        if (targetPeerId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                socketIoClient.emit(
                    "removeParticipant",
                    JSONObject().put("targetPeerId", targetPeerId),
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        activeRoom = it.activeRoom.copy(
                            participants = it.activeRoom.participants.filterNot { current ->
                                current.peerId == participant.peerId || current.userId == participant.userId
                            },
                        ),
                        message = "已移出 ${participant.username}",
                    )
                }
                refreshRoomParticipants()
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "移出成员失败") }
            }
        }
    }

    fun toggleScreenShare() {
        _uiState.update {
            val next = !it.activeRoom.screenSharing
            it.copy(
                activeRoom = it.activeRoom.copy(screenSharing = next),
                message = if (next) "屏幕共享已开启" else "屏幕共享已停止",
            )
        }
    }

    fun toggleHandRaised() {
        val next = !_uiState.value.activeRoom.handRaised
        viewModelScope.launch {
            runCatching {
                socketIoClient.emit("setHandRaised", JSONObject().put("raised", next))
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        activeRoom = updateHandRaised(it.activeRoom, it.userSummary.userId, next).copy(handRaised = next),
                        message = if (next) "已举手" else "已取消举手",
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "举手状态更新失败") }
            }
        }
    }

    fun switchCamera() {
        roomMediaEngine.switchCamera()
        _uiState.update { it.copy(message = "已切换摄像头") }
    }

    fun toggleCaptions() {
        _uiState.update {
            val next = !it.activeRoom.captionsEnabled
            it.copy(
                activeRoom = it.activeRoom.copy(captionsEnabled = next),
                message = if (next) "字幕已开启" else "字幕已关闭",
            )
        }
    }

    fun openMeetingSettings() {
        _uiState.update { it.copy(message = "会议设置将在后续版本提供更多控制项") }
    }

    fun closeMeeting() {
        val roomId = _uiState.value.activeRoom.meeting?.roomId.orEmpty()
        if (roomId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            val httpResult = roomRepository.closeMeeting(roomId)
            val socketResult = runCatching {
                if (socketIoClient.isConnected) {
                    socketIoClient.emit("closeRoom", JSONObject().put("roomId", roomId).put("reason", "host_closed"))
                }
            }
            when {
                httpResult is ApiResult.Failure -> _uiState.update { it.copy(message = httpResult.error.message) }
                socketResult.isFailure -> _uiState.update { it.copy(message = socketResult.exceptionOrNull()?.message ?: "关闭会议失败") }
                else -> {
                    roomMediaEngine.closeSession()
                    clearRoomSocketListeners()
                    socketIoClient.disconnect()
                    _uiState.update {
                        it.copy(
                            route = MainRoute.Tabs,
                            selectedTab = MainTab.Meetings,
                            activeRoom = RoomUiState(),
                            message = "会议已关闭",
                        )
                    }
                    loadMeetingLists()
                }
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    override fun onCleared() {
        roomMediaEngine.release()
        super.onCleared()
    }

    private suspend fun createSfuTransport(
        roomId: String,
        producing: Boolean,
        consuming: Boolean,
    ): SfuTransportState {
        val response = socketIoClient.emit(
            "createWebRtcTransport",
            JSONObject()
                .put("roomId", roomId)
                .put("producing", producing)
                .put("consuming", consuming),
        ).asJsonObject()
        val id = response?.optString("id").orEmpty()
        return SfuTransportState(
            id = id,
            direction = if (producing) SfuTransportDirection.Send else SfuTransportDirection.Recv,
            connected = false,
            iceParametersJson = response?.optJSONObject("iceParameters")?.toString().orEmpty(),
            iceCandidatesJson = response?.optJSONArray("iceCandidates")?.toString().orEmpty(),
            dtlsParametersJson = response?.optJSONObject("dtlsParameters")?.toString().orEmpty(),
            sctpParametersJson = response?.optJSONObject("sctpParameters")?.toString().orEmpty(),
        )
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
            val session = sessionStore.currentSession()
            val userId = session.userId.orEmpty()
            if (userId.isBlank()) {
                _uiState.update { it.copy(isSubmitting = false, message = "缺少用户信息，请重新登录") }
                return@launch
            }
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
                    val preJoinMeeting = if (form.type == MeetingCreateType.Instant && meeting != null) {
                        joinRoomForPreJoin(meeting) ?: run {
                            _uiState.update { it.copy(isSubmitting = false) }
                            return@launch
                        }
                    } else {
                        meeting
                    }
                    _uiState.update {
                        it.copy(
                            route = MainRoute.PreJoin,
                            preJoinMeeting = preJoinMeeting ?: MeetingSummary(
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

    private suspend fun joinRoomForPreJoin(meeting: MeetingSummary): MeetingSummary? {
        val roomId = meeting.roomId.ifBlank { meeting.roomNo }
        if (roomId.isBlank()) return meeting
        return when (val result = roomRepository.joinMeeting(mapOf("roomId" to roomId))) {
            is ApiResult.Success -> {
                val data = result.data.responseDataObject() ?: result.data.asJsonObjectOrNull()
                val allowed = data?.get("allowed")?.asBooleanOrNull() ?: true
                if (!allowed) {
                    _uiState.update { it.copy(message = data?.firstString("message") ?: "无法加入会议") }
                    null
                } else {
                    meeting.copy(
                        roomId = data?.firstString("roomId") ?: meeting.roomId,
                        sfuServerUrl = data?.firstString("sfuServerUrl", "socketUrl", "url") ?: meeting.sfuServerUrl,
                    )
                }
            }

            is ApiResult.Failure -> {
                _uiState.update { it.copy(message = result.error.message) }
                null
            }
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
                    val data = result.data.responseDataObject() ?: result.data.asJsonObjectOrNull()
                    val allowed = data?.get("allowed")?.asBooleanOrNull() ?: true
                    if (!allowed) {
                        _uiState.update {
                            it.copy(joinError = mapJoinFailure(null, data?.firstString("message") ?: "无法加入会议"))
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                route = MainRoute.PreJoin,
                                preJoinMeeting = meeting.copy(
                                    roomId = data?.firstString("roomId") ?: meeting.roomId,
                                    sfuServerUrl = data?.firstString("sfuServerUrl", "socketUrl", "url") ?: meeting.sfuServerUrl,
                                ),
                                message = null,
                            )
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

        _uiState.update {
            it.copy(
                userSummary = it.userSummary.copy(
                    userId = userId,
                    displayName = session.nickname?.takeIf(String::isNotBlank) ?: it.userSummary.displayName,
                    phone = session.phone.orEmpty(),
                    avatar = session.avatar.orEmpty(),
                ),
            )
        }

        when (val result = userRepository.getUserProfile(userId)) {
            is ApiResult.Success -> {
                val data = result.data.responseDataObject()
                val nickname = data?.firstString("nickname", "username", "name", "displayName")
                val phone = data?.firstString("phone", "mobile")
                val avatar = data?.firstString("avatar")
                sessionStore.saveUserProfile(
                    userId = userId,
                    nickname = nickname,
                    phone = phone,
                    avatar = avatar,
                )
                _uiState.update {
                    it.copy(
                        userSummary = it.userSummary.copy(
                            userId = userId,
                            displayName = nickname ?: "用户 $userId",
                            phone = phone.orEmpty(),
                            avatar = avatar.orEmpty(),
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
            val producerId = body.optString("producerId")
            if (producerId.isNotBlank()) {
                roomMediaEngine.registerRemoteProducer(body.toProducerState())
            }
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
                        mediaState = updateProducerPauseState(
                            mediaState = it.activeRoom.mediaState,
                            producerId = producerId,
                            peerId = peerId,
                            kind = kind,
                            paused = !enabled,
                        ),
                    ),
                )
            }
        }
        roomSocketSubscriptions += socketIoClient.on("handRaiseChanged") { args ->
            val body = args.firstJsonObject() ?: return@on
            val userId = body.optString("userId")
            val peerId = body.optString("peerId", userId)
            val raised = body.optBoolean("raised", false)
            _uiState.update {
                val isLocal = userId == it.userSummary.userId || peerId == it.activeRoom.participants.firstOrNull(RoomParticipant::isLocal)?.peerId
                it.copy(
                    activeRoom = updateHandRaised(
                        roomState = it.activeRoom.copy(handRaised = if (isLocal) raised else it.activeRoom.handRaised),
                        userId = userId.ifBlank { peerId },
                        raised = raised,
                    ),
                )
            }
        }
        roomSocketSubscriptions += socketIoClient.on("newProducer") { args ->
            val body = args.firstJsonObject() ?: return@on
            val producer = body.toProducerState()
            val participant = participantFromJson(body).copy(
                audioEnabled = body.optString("kind") != "video",
                videoEnabled = body.optString("kind") != "audio",
            )
            _uiState.update {
                it.copy(
                    activeRoom = it.activeRoom.copy(
                        participants = it.activeRoom.participants.upsert(participant),
                    ),
                )
            }
            roomMediaEngine.registerRemoteProducer(producer)
            roomMediaEngine.consumeRemoteProducer(producer)
        }
        roomSocketSubscriptions += socketIoClient.on("consumerClosed") { args ->
            val body = args.firstJsonObject() ?: return@on
            val consumerId = body.optString("consumerId")
            _uiState.update {
                it.copy(
                    activeRoom = it.activeRoom.copy(
                        mediaState = it.activeRoom.mediaState.copy(
                            consumers = it.activeRoom.mediaState.consumers.filterNot { consumer -> consumer.id == consumerId },
                        ),
                    ),
                )
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

    private fun hostControlParticipant(
        participant: RoomParticipant,
        event: String,
        enabled: Boolean,
        successMessage: String,
    ) {
        val roomId = _uiState.value.activeRoom.meeting?.roomId.orEmpty()
        val targetPeerId = participant.peerId.ifBlank { participant.userId }
        if (roomId.isBlank() || targetPeerId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                socketIoClient.emit(
                    event,
                    JSONObject()
                        .put("roomId", roomId)
                        .put("targetPeerId", targetPeerId)
                        .put("enabled", enabled),
                )
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        activeRoom = state.activeRoom.copy(
                            participants = state.activeRoom.participants.map { current ->
                                if (current.peerId == participant.peerId || current.userId == participant.userId) {
                                    when (event) {
                                        "hostToggleAudio" -> current.copy(audioEnabled = enabled)
                                        "hostToggleVideo" -> current.copy(videoEnabled = enabled)
                                        else -> current
                                    }
                                } else {
                                    current
                                }
                            },
                        ),
                        message = successMessage,
                    )
                }
                refreshRoomParticipants()
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "成员状态控制失败") }
            }
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

    private fun updateHandRaised(
        roomState: RoomUiState,
        userId: String,
        raised: Boolean,
    ): RoomUiState =
        roomState.copy(
            participants = roomState.participants.map { participant ->
                if (participant.userId == userId || participant.peerId == userId) {
                    participant.copy(handRaised = raised)
                } else {
                    participant
                }
            },
        )

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
                hostId = body.firstString("hostId") ?: host?.firstString("id", "userId").orEmpty(),
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

    private fun parseParticipantList(element: JsonElement): List<RoomParticipant> {
        val body = element.asJsonObjectOrNull()
        val array = body?.get("data")?.asJsonArrayOrNull()
            ?: body?.getAsJsonObjectOrNull("data")?.let { data ->
                listOfNotNull(data.get("list"), data.get("records"), data.get("items"))
                    .firstNotNullOfOrNull { it.asJsonArrayOrNull() }
            }
            ?: element.asJsonArrayOrNull()
            ?: JsonArray()
        return array.mapNotNull(::parseParticipantFromObject)
    }

    private fun parseParticipantFromObject(element: JsonElement): RoomParticipant? =
        element.asJsonObjectOrNull()?.let { body ->
            val userId = body.firstString("userId", "id").orEmpty()
            val roleCode = body.get("role")?.asIntOrNull() ?: PARTICIPANT_ROLE_MEMBER
            val statusCode = body.get("status")?.asIntOrNull() ?: PARTICIPANT_STATUS_ONLINE
            val localUserId = _uiState.value.userSummary.userId
            RoomParticipant(
                peerId = body.firstString("peerId", "socketId").orEmpty().ifBlank { userId },
                userId = userId,
                username = body.firstString("userName", "username", "name", "nickname") ?: "参会者",
                avatar = body.firstString("avatar").orEmpty(),
                role = if (roleCode == PARTICIPANT_ROLE_HOST) ParticipantRole.Host else ParticipantRole.Member,
                status = if (statusCode == PARTICIPANT_STATUS_ONLINE) ParticipantStatus.Online else ParticipantStatus.Offline,
                isLocal = userId.isNotBlank() && userId == localUserId,
                audioEnabled = body.get("audioMuted")?.asBooleanOrNull() != true,
                videoEnabled = body.get("videoMuted")?.asBooleanOrNull() != true,
                joinedAt = body.firstString("joinedAt").orEmpty(),
                leftAt = body.firstString("leftAt").orEmpty(),
            )
        }

    private fun parseJoinRoomProducers(response: Any?): List<SfuProducerState> {
        val body = response.asJsonObject() ?: return emptyList()
        val peers = body.optJSONArray("peers") ?: return emptyList()
        return buildList {
            for (peerIndex in 0 until peers.length()) {
                val peer = peers.optJSONObject(peerIndex) ?: continue
                val producers = peer.optJSONArray("producers") ?: continue
                for (producerIndex in 0 until producers.length()) {
                    val producer = producers.optJSONObject(producerIndex) ?: continue
                    add(
                        SfuProducerState(
                            id = producer.optString("id"),
                            peerId = peer.optString("peerId"),
                            userId = peer.optString("userId"),
                            username = peer.optString("username"),
                            kind = producer.optString("kind"),
                            paused = producer.optBoolean("paused", false),
                        ),
                    )
                }
            }
        }
    }

    private fun buildLocalProducerPlaceholders(
        peerId: String,
        username: String,
        audioEnabled: Boolean,
        videoEnabled: Boolean,
    ): List<SfuProducerState> =
        buildList {
            if (audioEnabled) {
                add(
                    SfuProducerState(
                        id = "",
                        peerId = peerId,
                        userId = peerId,
                        username = username,
                        kind = "audio",
                        local = true,
                    ),
                )
            }
            if (videoEnabled) {
                add(
                    SfuProducerState(
                        id = "",
                        peerId = peerId,
                        userId = peerId,
                        username = username,
                        kind = "video",
                        local = true,
                    ),
                )
            }
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

    private fun JSONObject.toProducerState(): SfuProducerState =
        SfuProducerState(
            id = optString("producerId", optString("id")),
            peerId = optString("peerId"),
            userId = optString("userId"),
            username = optString("username"),
            kind = optString("kind"),
            paused = optBoolean("paused", false),
        )

    private fun mergeParticipants(
        current: List<RoomParticipant>,
        fetched: List<RoomParticipant>,
    ): List<RoomParticipant> {
        val fetchedByUserId = fetched.associateBy { it.userId }
        val mergedCurrent = current.map { participant ->
            val fetchedParticipant = fetchedByUserId[participant.userId]
            if (fetchedParticipant == null) {
                participant
            } else {
                fetchedParticipant.copy(
                    peerId = participant.peerId.ifBlank { fetchedParticipant.peerId },
                    isLocal = participant.isLocal || fetchedParticipant.isLocal,
                    speaking = participant.speaking,
                    handRaised = participant.handRaised,
                )
            }
        }
        val currentUserIds = mergedCurrent.mapNotNull { it.userId.takeIf(String::isNotBlank) }.toSet()
        return mergedCurrent + fetched.filterNot { it.userId in currentUserIds }
    }

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

    private fun List<RoomParticipant>.sortedForRoom(): List<RoomParticipant> =
        sortedWith(
            compareByDescending<RoomParticipant> { it.role == ParticipantRole.Host }
                .thenByDescending { it.status == ParticipantStatus.Online }
                .thenByDescending { it.isLocal }
                .thenBy { it.username },
        )

    private fun List<SfuProducerState>.upsertProducer(producer: SfuProducerState): List<SfuProducerState> =
        if (producer.id.isBlank()) {
            this
        } else if (any { it.id == producer.id }) {
            map { current -> if (current.id == producer.id) producer else current }
        } else {
            this + producer
        }

    private fun List<SfuConsumerState>.upsertConsumer(consumer: SfuConsumerState): List<SfuConsumerState> =
        if (consumer.id.isBlank()) {
            this
        } else if (any { it.id == consumer.id }) {
            map { current -> if (current.id == consumer.id) consumer else current }
        } else {
            this + consumer
        }

    private fun updateProducerPauseState(
        mediaState: RoomMediaState,
        producerId: String,
        peerId: String,
        kind: String,
        paused: Boolean,
    ): RoomMediaState =
        mediaState.copy(
            localProducers = mediaState.localProducers.mapProducerPause(producerId, peerId, kind, paused),
            remoteProducers = mediaState.remoteProducers.mapProducerPause(producerId, peerId, kind, paused),
            consumers = mediaState.consumers.map { consumer ->
                if (consumer.producerId == producerId) consumer.copy(producerPaused = paused) else consumer
            },
        )

    private fun List<SfuProducerState>.mapProducerPause(
        producerId: String,
        peerId: String,
        kind: String,
        paused: Boolean,
    ): List<SfuProducerState> =
        map { producer ->
            if (producer.id == producerId || (producer.peerId == peerId && producer.kind == kind)) {
                producer.copy(paused = paused)
            } else {
                producer
            }
        }

    private fun Array<Any>.firstJsonObject(): JSONObject? =
        firstOrNull()?.let { value ->
            when (value) {
                is JSONObject -> value
                is Map<*, *> -> JSONObject(value)
                else -> runCatching { JSONObject(value.toString()) }.getOrNull()
            }
        }

    private fun Any?.asJsonObject(): JSONObject? =
        when (this) {
            is JSONObject -> this
            is Map<*, *> -> JSONObject(this)
            null -> null
            else -> runCatching { JSONObject(toString()) }.getOrNull()
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
        const val PARTICIPANT_ROLE_MEMBER = 1
        const val PARTICIPANT_ROLE_HOST = 2
        const val PARTICIPANT_STATUS_ONLINE = 1
        val API_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val DISPLAY_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
