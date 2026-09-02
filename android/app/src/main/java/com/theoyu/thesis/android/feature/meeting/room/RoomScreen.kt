package com.theoyu.thesis.android.feature.meeting.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.theoyu.thesis.android.core.sfu.WebRtcEnvironment
import com.theoyu.thesis.android.feature.main.AudioRoute
import com.theoyu.thesis.android.feature.main.ParticipantRole
import com.theoyu.thesis.android.feature.main.ParticipantStatus
import com.theoyu.thesis.android.feature.main.RoomChatMessage
import com.theoyu.thesis.android.feature.main.RoomParticipant
import com.theoyu.thesis.android.feature.main.RoomSheet
import com.theoyu.thesis.android.feature.main.RoomUiState
import com.theoyu.thesis.android.feature.main.component.Avatar
import com.theoyu.thesis.android.feature.main.component.EmptyState
import com.theoyu.thesis.android.feature.main.component.InfoRow
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@OptIn(ExperimentalMaterial3Api::class)
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
    onMuteAllParticipants: () -> Unit,
    onDisableAllParticipantVideo: () -> Unit,
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val participants = roomState.participants.ifEmpty {
        listOf(RoomParticipant(peerId = "local", username = "我", isLocal = true))
    }
    val activeSpeaker = participants.firstOrNull { it.peerId == roomState.screenSharePeerId }
        ?: participants.firstOrNull { it.peerId == roomState.activeSpeakerPeerId }
        ?: participants.first()
    val isHost = participants.any { it.isLocal && it.role == ParticipantRole.Host }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (isLandscape) {
            CompactVideoGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 64.dp, end = 12.dp, bottom = 96.dp),
                participants = participants,
                localVideoTrack = roomState.mediaState.localVideoTrack,
                remoteVideoTracks = roomState.mediaState.remoteVideoTracks,
                activeSpeakerPeerId = activeSpeaker.peerId,
            )
        } else {
            PortraitVideoLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 72.dp, end = 12.dp, bottom = 100.dp),
                activeSpeaker = activeSpeaker,
                participants = participants,
                localVideoTrack = roomState.mediaState.localVideoTrack,
                remoteVideoTracks = roomState.mediaState.remoteVideoTracks,
            )
        }

        RoomTopOverlay(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            roomState = roomState,
            activeSpeaker = activeSpeaker,
        )

        RoomControlBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            roomState = roomState,
            onToggleAudio = onToggleAudio,
            onToggleVideo = onToggleVideo,
            onLeaveRoom = onLeaveRoom,
            onOpenSheet = onOpenSheet,
        )
    }

    roomState.selectedSheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = onCloseSheet) {
            when (sheet) {
                RoomSheet.Members -> ParticipantSheet(
                    roomState = roomState,
                    isHost = isHost,
                    onRefresh = onRefreshParticipants,
                    onToggleAudio = onHostToggleParticipantAudio,
                    onToggleVideo = onHostToggleParticipantVideo,
                    onRemoveParticipant = onRemoveParticipant,
                    onMuteAll = onMuteAllParticipants,
                    onDisableAllVideo = onDisableAllParticipantVideo,
                )

                RoomSheet.Chat -> ChatSheet(
                    roomState = roomState,
                    onSendMessage = onSendMessage,
                )

                RoomSheet.More -> MoreActionsSheet(
                    roomState = roomState,
                    isHost = isHost,
                    availableAudioRoutes = availableAudioRoutes,
                    selectedAudioRoute = selectedAudioRoute,
                    onToggleScreenShare = onToggleScreenShare,
                    onToggleHandRaised = onToggleHandRaised,
                    onSwitchCamera = onSwitchCamera,
                    onAudioRouteSelected = onAudioRouteSelected,
                    onOpenMeetingSettings = onOpenMeetingSettings,
                    onToggleCaptions = onToggleCaptions,
                    onCloseMeeting = onCloseMeeting,
                )
            }
        }
    }
}

@Composable
private fun PortraitVideoLayout(
    modifier: Modifier,
    activeSpeaker: RoomParticipant,
    participants: List<RoomParticipant>,
    localVideoTrack: VideoTrack?,
    remoteVideoTracks: Map<String, VideoTrack?>,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VideoTile(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            participant = activeSpeaker,
            videoTrack = activeSpeaker.resolveVideoTrack(localVideoTrack, remoteVideoTracks),
            prominent = true,
            isActiveSpeaker = true,
        )
        val thumbnails = participants.filterNot { it.peerId == activeSpeaker.peerId }.take(6)
        if (thumbnails.isNotEmpty()) {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp),
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(thumbnails, key = { it.peerId }) { participant ->
                    VideoTile(
                        modifier = Modifier.height(116.dp),
                        participant = participant,
                        videoTrack = participant.resolveVideoTrack(localVideoTrack, remoteVideoTracks),
                        prominent = false,
                        isActiveSpeaker = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactVideoGrid(
    modifier: Modifier,
    participants: List<RoomParticipant>,
    localVideoTrack: VideoTrack?,
    remoteVideoTracks: Map<String, VideoTrack?>,
    activeSpeakerPeerId: String,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(180.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(participants, key = { it.peerId }) { participant ->
            VideoTile(
                modifier = Modifier.height(150.dp),
                participant = participant,
                videoTrack = participant.resolveVideoTrack(localVideoTrack, remoteVideoTracks),
                prominent = false,
                isActiveSpeaker = participant.peerId == activeSpeakerPeerId,
            )
        }
    }
}

@Composable
private fun VideoTile(
    modifier: Modifier,
    participant: RoomParticipant,
    videoTrack: VideoTrack?,
    prominent: Boolean,
    isActiveSpeaker: Boolean = false,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = if (isActiveSpeaker) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
        ) {
            if (participant.videoEnabled && videoTrack != null) {
                VideoTrackRenderer(track = videoTrack)
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Avatar(
                        name = participant.username,
                        modifier = Modifier.size(if (prominent) 76.dp else 40.dp),
                    )
                    Text(
                        text = if (participant.videoEnabled) "等待视频流..." else "摄像头已关闭",
                        style = if (prominent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (participant.isLocal) "${participant.username} (我)" else participant.username,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (participant.handRaised) {
                        Icon(
                            imageVector = Icons.Filled.PanTool,
                            contentDescription = "举手",
                            modifier = Modifier.size(13.dp),
                            tint = Color.Yellow,
                        )
                    }
                    Icon(
                        imageVector = if (participant.audioEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = if (participant.audioEnabled) "麦克风已开" else "麦克风已关",
                        modifier = Modifier.size(13.dp),
                        tint = if (participant.audioEnabled) Color.Green else Color.Red,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoTrackRenderer(track: VideoTrack) {
    val context = LocalContext.current
    val renderer = remember {
        SurfaceViewRenderer(context).apply {
            init(WebRtcEnvironment.eglBase.eglBaseContext, null)
            setEnableHardwareScaler(true)
        }
    }
    DisposableEffect(track, renderer) {
        track.addSink(renderer)
        onDispose {
            track.removeSink(renderer)
        }
    }
    DisposableEffect(renderer) {
        onDispose {
            renderer.release()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { renderer },
    )
}

private fun RoomParticipant.resolveVideoTrack(
    localVideoTrack: VideoTrack?,
    remoteVideoTracks: Map<String, VideoTrack?>,
): VideoTrack? =
    if (isLocal) {
        localVideoTrack
    } else {
        remoteVideoTracks[peerId] ?: remoteVideoTracks[userId]
    }

@Composable
private fun RoomTopOverlay(
    modifier: Modifier,
    roomState: RoomUiState,
    activeSpeaker: RoomParticipant,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roomState.meeting?.title ?: "会议房间",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (activeSpeaker.peerId == roomState.screenSharePeerId) {
                        "${activeSpeaker.username} 正在共享屏幕"
                    } else {
                        "主讲：${activeSpeaker.username} · ${roomState.mediaState.phase.label}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val dotColor = when {
                        !roomState.socketConnected -> Color.Gray
                        (roomState.rttMillis ?: 0) > 300 -> MaterialTheme.colorScheme.error
                        (roomState.rttMillis ?: 0) > 120 -> Color(0xFFE6A23C)
                        else -> Color(0xFF67C23A)
                    }
                    Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        contentDescription = null,
                        tint = dotColor,
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        text = roomState.rttMillis?.let { "${it}ms" } ?: roomState.networkQuality.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomControlBar(
    modifier: Modifier,
    roomState: RoomUiState,
    onToggleAudio: () -> Unit,
    onToggleVideo: () -> Unit,
    onLeaveRoom: () -> Unit,
    onOpenSheet: (RoomSheet) -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlActionButton(
                icon = if (roomState.audioEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                label = if (roomState.audioEnabled) "静音" else "开麦",
                active = roomState.audioEnabled,
                onClick = onToggleAudio,
            )
            ControlActionButton(
                icon = if (roomState.videoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                label = if (roomState.videoEnabled) "关视频" else "开视频",
                active = roomState.videoEnabled,
                onClick = onToggleVideo,
            )
            ControlActionButton(
                icon = Icons.Filled.CallEnd,
                label = "离开",
                isDanger = true,
                onClick = onLeaveRoom,
            )
            ControlActionButton(
                icon = Icons.Filled.Group,
                label = "成员",
                badgeText = "${roomState.participants.size.coerceAtLeast(1)}",
                onClick = { onOpenSheet(RoomSheet.Members) },
            )
            ControlActionButton(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "聊天",
                badgeText = if (roomState.chatMessages.isNotEmpty()) "${roomState.chatMessages.size}" else null,
                onClick = { onOpenSheet(RoomSheet.Chat) },
            )
            ControlActionButton(
                icon = Icons.Filled.MoreHoriz,
                label = "更多",
                onClick = { onOpenSheet(RoomSheet.More) },
            )
        }
    }
}

@Composable
private fun ControlActionButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    isDanger: Boolean = false,
    badgeText: String? = null,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box {
            if (isDanger) {
                FilledIconButton(
                    onClick = onClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(imageVector = icon, contentDescription = label)
                }
            } else if (active) {
                FilledIconButton(
                    onClick = onClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(imageVector = icon, contentDescription = label)
                }
            } else {
                FilledTonalIconButton(
                    onClick = onClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(imageVector = icon, contentDescription = label)
                }
            }

            if (!badgeText.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 0.dp, end = 0.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ParticipantSheet(
    roomState: RoomUiState,
    isHost: Boolean,
    onRefresh: () -> Unit,
    onToggleAudio: (RoomParticipant) -> Unit,
    onToggleVideo: (RoomParticipant) -> Unit,
    onRemoveParticipant: (RoomParticipant) -> Unit,
    onMuteAll: () -> Unit,
    onDisableAllVideo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "参会人员",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "共 ${roomState.participantsTotal.coerceAtLeast(roomState.participants.size)} 位成员在线",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新成员")
            }
        }

        if (isHost) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    onClick = onMuteAll,
                ) {
                    Icon(Icons.Filled.MicOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("全体静音")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    onClick = onDisableAllVideo,
                ) {
                    Icon(Icons.Filled.VideocamOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("关全员视频")
                }
            }
        }

        if (roomState.participantsLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (roomState.participants.isEmpty()) {
            EmptyState("暂无成员信息")
        } else {
            LazyColumn(
                modifier = Modifier.height(380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(roomState.participants, key = { it.userId.ifBlank { it.peerId } }) { participant ->
                    ParticipantRow(
                        participant = participant,
                        canModerate = isHost && !participant.isLocal && participant.role != ParticipantRole.Host,
                        onToggleAudio = onToggleAudio,
                        onToggleVideo = onToggleVideo,
                        onRemoveParticipant = onRemoveParticipant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: RoomParticipant,
    canModerate: Boolean,
    onToggleAudio: (RoomParticipant) -> Unit,
    onToggleVideo: (RoomParticipant) -> Unit,
    onRemoveParticipant: (RoomParticipant) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = participant.username, modifier = Modifier.size(42.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (participant.isLocal) "${participant.username} (我)" else participant.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (participant.role == ParticipantRole.Host) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("主持人", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
                Text(
                    text = listOf(participant.status.label, participant.joinedAt.take(16)).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (participant.audioEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (participant.audioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (participant.videoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (participant.videoEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (participant.handRaised) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.PanTool,
                        contentDescription = "举手",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (canModerate) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "管理成员")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (participant.audioEnabled) "静音成员" else "解除静音") },
                                leadingIcon = {
                                    Icon(if (participant.audioEnabled) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    onToggleAudio(participant)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (participant.videoEnabled) "关闭其摄像头" else "请求开启摄像头") },
                                leadingIcon = {
                                    Icon(if (participant.videoEnabled) Icons.Filled.VideocamOff else Icons.Filled.Videocam, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    onToggleVideo(participant)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("移出会议", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Filled.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuOpen = false
                                    onRemoveParticipant(participant)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreActionsSheet(
    roomState: RoomUiState,
    isHost: Boolean,
    availableAudioRoutes: List<AudioRoute>,
    selectedAudioRoute: AudioRoute,
    onToggleScreenShare: () -> Unit,
    onToggleHandRaised: () -> Unit,
    onSwitchCamera: () -> Unit,
    onAudioRouteSelected: (AudioRoute) -> Unit,
    onOpenMeetingSettings: () -> Unit,
    onToggleCaptions: () -> Unit,
    onCloseMeeting: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("更多功能", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyVerticalGrid(
            modifier = Modifier.height(200.dp),
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                listOf(
                    MoreAction(
                        label = if (roomState.screenSharing) "停止共享" else "屏幕共享",
                        icon = if (roomState.screenSharing) Icons.AutoMirrored.Filled.StopScreenShare else Icons.AutoMirrored.Filled.ScreenShare,
                        selected = roomState.screenSharing,
                        onClick = onToggleScreenShare,
                    ),
                    MoreAction(
                        label = if (roomState.handRaised) "放下手" else "举手发言",
                        icon = Icons.Filled.PanTool,
                        selected = roomState.handRaised,
                        onClick = onToggleHandRaised,
                    ),
                    MoreAction("翻转摄像头", Icons.Filled.CameraAlt, onClick = onSwitchCamera),
                    MoreAction("会议设置", Icons.Filled.Settings, onClick = onOpenMeetingSettings),
                    MoreAction(
                        label = if (roomState.captionsEnabled) "关闭字幕" else "实时字幕",
                        icon = Icons.Filled.ClosedCaption,
                        selected = roomState.captionsEnabled,
                        onClick = onToggleCaptions,
                    ),
                ),
                key = { it.label },
            ) { action ->
                MoreActionButton(action)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "音频输出通道",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                availableAudioRoutes.forEach { route ->
                    val icon = when (route) {
                        AudioRoute.Speaker -> Icons.AutoMirrored.Filled.VolumeUp
                        AudioRoute.Earpiece -> Icons.Filled.Settings
                    }
                    FilterChip(
                        selected = selectedAudioRoute == route,
                        onClick = { onAudioRouteSelected(route) },
                        label = { Text(route.label) },
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            }
        }
        if (isHost) {
            HorizontalDivider()
            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = onCloseMeeting,
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("全员结束会议")
            }
        }
    }
}

private data class MoreAction(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun MoreActionButton(action: MoreAction) {
    FilledTonalButton(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (action.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (action.selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        onClick = action.onClick,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChatSheet(
    roomState: RoomUiState,
    onSendMessage: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("房间聊天", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (roomState.chatMessages.isEmpty()) {
            EmptyState(
                text = "暂无聊天消息",
                description = "向所有参会成员发送即时文字消息",
                icon = Icons.AutoMirrored.Filled.Chat,
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(roomState.chatMessages.takeLast(50), key = { it.id }) { message ->
                    ChatMessageBubble(message = message)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("发送消息给所有人...") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
            FilledIconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSendMessage(draft)
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送消息")
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: RoomChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isLocal) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isLocal) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!message.isLocal) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isLocal) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
