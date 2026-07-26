package com.theoyu.thesis.android.feature.meeting.room

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.react.bridge.ReadableMap
import com.theoyu.thesis.android.BlueSkyApplication
import com.theoyu.thesis.android.feature.main.AudioRoute
import com.theoyu.thesis.android.feature.main.MeetingSummary
import com.theoyu.thesis.android.feature.main.RoomChatMessage
import com.theoyu.thesis.android.feature.main.RoomParticipant
import com.theoyu.thesis.android.feature.main.RoomSheet
import com.theoyu.thesis.android.feature.main.RoomUiState
import com.theoyu.thesis.android.feature.main.SfuConsumerState
import com.theoyu.thesis.android.feature.main.SfuProducerState
import com.theoyu.thesis.android.feature.main.SfuTransportState
import com.theoyu.thesis.android.react.MeetingRoomActionDispatcher
import com.theoyu.thesis.android.react.MeetingRoomEvents
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun RoomScreen(
    roomState: RoomUiState,
    onToggleAudio: () -> Unit,
    onToggleVideo: () -> Unit,
    onLeaveRoom: () -> Unit,
    onOpenSheet: (RoomSheet) -> Unit,
    onCloseSheet: () -> Unit,
    onRefreshParticipants: () -> Unit,
    onHostToggleParticipantAudio: (RoomParticipant) -> Unit,
    onHostToggleParticipantVideo: (RoomParticipant) -> Unit,
    onRemoveParticipant: (RoomParticipant) -> Unit,
    onToggleScreenShare: () -> Unit,
    onToggleHandRaised: () -> Unit,
    onSwitchCamera: () -> Unit,
    availableAudioRoutes: List<AudioRoute>,
    selectedAudioRoute: AudioRoute,
    onAudioRouteSelected: (AudioRoute) -> Unit,
    onToggleCaptions: () -> Unit,
    onOpenMeetingSettings: () -> Unit,
    onCloseMeeting: () -> Unit,
    onSendMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as BlueSkyApplication
    val reactHost = application.reactHost
    val authToken = remember { application.appContainer.sessionStore.currentToken().orEmpty() }
    val roomStateJson = remember(roomState, availableAudioRoutes, selectedAudioRoute) {
        roomState.toReactJson(
            availableAudioRoutes = availableAudioRoutes,
            selectedAudioRoute = selectedAudioRoute,
            authToken = authToken,
        )
    }
    val surface = remember {
        reactHost.createSurface(
            context = context,
            moduleName = "MeetingRoom",
            initialProps = android.os.Bundle().apply {
                putString("roomStateJson", roomStateJson)
            },
        )
    }

    DisposableEffect(surface) {
        surface.start()
        onDispose {
            surface.stop()
            surface.clear()
            surface.detach()
        }
    }

    DisposableEffect(
        roomState.participants,
        onToggleAudio,
        onToggleVideo,
        onLeaveRoom,
        onOpenSheet,
        onCloseSheet,
        onRefreshParticipants,
        onHostToggleParticipantAudio,
        onHostToggleParticipantVideo,
        onRemoveParticipant,
        onToggleScreenShare,
        onToggleHandRaised,
        onSwitchCamera,
        onAudioRouteSelected,
        onToggleCaptions,
        onOpenMeetingSettings,
        onCloseMeeting,
        onSendMessage,
    ) {
        MeetingRoomActionDispatcher.setHandler { action, payload ->
            when (action) {
                "toggleAudio" -> onToggleAudio()
                "toggleVideo" -> onToggleVideo()
                "leaveRoom" -> onLeaveRoom()
                "closeSheet" -> onCloseSheet()
                "refreshParticipants" -> onRefreshParticipants()
                "toggleScreenShare" -> onToggleScreenShare()
                "toggleHandRaised" -> onToggleHandRaised()
                "switchCamera" -> onSwitchCamera()
                "toggleCaptions" -> onToggleCaptions()
                "openMeetingSettings" -> onOpenMeetingSettings()
                "closeMeeting" -> onCloseMeeting()
                "openSheet" -> payload.getStringOrNull("sheet")?.toRoomSheet()?.let(onOpenSheet)
                "selectAudioRoute" -> payload.getStringOrNull("route")?.toAudioRoute()?.let(onAudioRouteSelected)
                "sendMessage" -> payload.getStringOrNull("content")?.let(onSendMessage)
                "hostToggleParticipantAudio" -> payload.findParticipant(roomState.participants)?.let(onHostToggleParticipantAudio)
                "hostToggleParticipantVideo" -> payload.findParticipant(roomState.participants)?.let(onHostToggleParticipantVideo)
                "removeParticipant" -> payload.findParticipant(roomState.participants)?.let(onRemoveParticipant)
            }
        }
        onDispose {
            MeetingRoomActionDispatcher.setHandler(null)
        }
    }

    LaunchedEffect(roomStateJson) {
        MeetingRoomEvents.emitState(reactHost, roomStateJson)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            surface.view ?: FrameLayout(it)
        },
    )
}

private fun ReadableMap?.getStringOrNull(key: String): String? =
    if (this != null && hasKey(key) && !isNull(key)) getString(key) else null

private fun ReadableMap?.findParticipant(participants: List<RoomParticipant>): RoomParticipant? {
    val peerId = getStringOrNull("peerId").orEmpty()
    val userId = getStringOrNull("userId").orEmpty()
    return participants.firstOrNull { participant ->
        (peerId.isNotBlank() && participant.peerId == peerId) ||
            (userId.isNotBlank() && participant.userId == userId)
    }
}

private fun String.toRoomSheet(): RoomSheet? =
    RoomSheet.entries.firstOrNull { it.name == this }

private fun String.toAudioRoute(): AudioRoute? =
    AudioRoute.entries.firstOrNull { it.name == this }

private fun RoomUiState.toReactJson(
    availableAudioRoutes: List<AudioRoute>,
    selectedAudioRoute: AudioRoute,
    authToken: String,
): String =
    participants.firstOrNull { it.isLocal }.let { localParticipant ->
    JSONObject()
        .put("meeting", meeting?.toJson())
        .put("currentUserId", localParticipant?.userId.orEmpty())
        .put("currentPeerId", localParticipant?.peerId.orEmpty())
        .put("currentUsername", localParticipant?.username.orEmpty())
        .put("authToken", authToken)
        .put("participants", participants.map(RoomParticipant::toJson).toJsonArray())
        .put("chatMessages", chatMessages.map(RoomChatMessage::toJson).toJsonArray())
        .put("activeSpeakerPeerId", activeSpeakerPeerId)
        .put("networkQuality", networkQuality.name)
        .put("networkQualityLabel", networkQuality.label)
        .put("socketConnected", socketConnected)
        .put("reconnecting", reconnecting)
        .put("rttMillis", rttMillis)
        .put("audioEnabled", audioEnabled)
        .put("videoEnabled", videoEnabled)
        .put("selectedSheet", selectedSheet?.name)
        .put("roomNotice", roomNotice)
        .put("participantsLoading", participantsLoading)
        .put("participantsTotal", participantsTotal)
        .put("handRaised", handRaised)
        .put("screenSharing", screenSharing)
        .put("captionsEnabled", captionsEnabled)
        .put("mediaState", mediaState.toJson())
        .put("availableAudioRoutes", availableAudioRoutes.map { it.name }.toJsonArray())
        .put("selectedAudioRoute", selectedAudioRoute.name)
        .toString()
    }

private fun MeetingSummary.toJson(): JSONObject =
    JSONObject()
        .put("roomId", roomId)
        .put("roomNo", roomNo)
        .put("title", title)
        .put("hostId", hostId)
        .put("hostName", hostName)
        .put("startTime", startTime)
        .put("endTime", endTime)
        .put("status", status)
        .put("maxParticipants", maxParticipants)
        .put("participantCount", participantCount)
        .put("description", description)
        .put("sfuServerUrl", sfuServerUrl)

private fun RoomParticipant.toJson(): JSONObject =
    JSONObject()
        .put("peerId", peerId)
        .put("userId", userId)
        .put("username", username)
        .put("avatar", avatar)
        .put("role", role.name)
        .put("roleLabel", role.label)
        .put("status", status.name)
        .put("statusLabel", status.label)
        .put("isLocal", isLocal)
        .put("audioEnabled", audioEnabled)
        .put("videoEnabled", videoEnabled)
        .put("handRaised", handRaised)
        .put("speaking", speaking)
        .put("joinedAt", joinedAt)
        .put("leftAt", leftAt)

private fun RoomChatMessage.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("senderName", senderName)
        .put("content", content)
        .put("timestamp", timestamp)
        .put("isLocal", isLocal)

private fun com.theoyu.thesis.android.feature.main.RoomMediaState.toJson(): JSONObject =
    JSONObject()
        .put("phase", phase.name)
        .put("phaseLabel", phase.label)
        .put("routerRtpCapabilitiesJson", routerRtpCapabilitiesJson)
        .put("sendTransport", sendTransport?.toJson())
        .put("recvTransport", recvTransport?.toJson())
        .put("localProducers", localProducers.map(SfuProducerState::toJson).toJsonArray())
        .put("remoteProducers", remoteProducers.map(SfuProducerState::toJson).toJsonArray())
        .put("consumers", consumers.map(SfuConsumerState::toJson).toJsonArray())
        .put("error", error)
        .put("mediaEngineReady", mediaEngineReady)

private fun SfuTransportState.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("direction", direction.name)
        .put("connected", connected)
        .put("iceParametersJson", iceParametersJson)
        .put("iceCandidatesJson", iceCandidatesJson)
        .put("dtlsParametersJson", dtlsParametersJson)
        .put("sctpParametersJson", sctpParametersJson)

private fun SfuProducerState.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("peerId", peerId)
        .put("userId", userId)
        .put("username", username)
        .put("kind", kind)
        .put("paused", paused)
        .put("local", local)

private fun SfuConsumerState.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("producerId", producerId)
        .put("kind", kind)
        .put("peerId", peerId)
        .put("resumed", resumed)
        .put("producerPaused", producerPaused)

private fun List<Any>.toJsonArray(): JSONArray =
    JSONArray().also { array ->
        forEach(array::put)
    }
