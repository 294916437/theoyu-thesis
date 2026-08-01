package com.theoyu.thesis.android.feature.meeting.room

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.core.sfu.WebRtcEnvironment
import com.theoyu.thesis.android.feature.main.AudioRoute
import com.theoyu.thesis.android.feature.main.ParticipantRole
import com.theoyu.thesis.android.feature.main.ParticipantStatus
import com.theoyu.thesis.android.feature.main.RoomParticipant
import com.theoyu.thesis.android.feature.main.RoomSheet
import com.theoyu.thesis.android.feature.main.RoomUiState
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
    val activeSpeaker = participants.firstOrNull { it.peerId == roomState.activeSpeakerPeerId }
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
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 88.dp),
                participants = participants,
                localVideoTrack = roomState.mediaState.localVideoTrack,
                remoteVideoTracks = roomState.mediaState.remoteVideoTracks,
            )
        } else {
            PortraitVideoLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 104.dp),
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
                .padding(12.dp),
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
        )
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(participants.filterNot { it.peerId == activeSpeaker.peerId }.take(6), key = { it.peerId }) { participant ->
                VideoTile(
                    modifier = Modifier.height(120.dp),
                    participant = participant,
                    videoTrack = participant.resolveVideoTrack(localVideoTrack, remoteVideoTracks),
                    prominent = false,
                )
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
) {
    Card(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (participant.videoEnabled && videoTrack != null) {
                VideoTrackRenderer(track = videoTrack)
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Avatar(
                        name = participant.username,
                        modifier = Modifier.size(if (prominent) 88.dp else 48.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (participant.videoEnabled) "等待远端视频流" else "摄像头已关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = participant.username,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (participant.handRaised) {
                    Icon(
                        imageVector = Icons.Filled.PanTool,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    imageVector = if (participant.audioEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = roomState.meeting?.title ?: "会议房间",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "当前发言人：${activeSpeaker.username} · ${roomState.mediaState.phase.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Card {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(roomState.networkQuality.label, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = roomState.rttMillis?.let { "${it}ms" } ?: if (roomState.socketConnected) "已连接" else "离线",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    Surface(modifier = modifier, tonalElevation = 6.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(
                modifier = Modifier.weight(1f),
                icon = if (roomState.audioEnabled) Icons.Filled.MicOff else Icons.Filled.Mic,
                label = if (roomState.audioEnabled) "静音" else "开麦",
                onClick = onToggleAudio,
            )
            ControlButton(
                modifier = Modifier.weight(1f),
                icon = if (roomState.videoEnabled) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                label = if (roomState.videoEnabled) "关视频" else "开视频",
                onClick = onToggleVideo,
            )
            ControlButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CallEnd,
                label = "挂断",
                danger = true,
                onClick = onLeaveRoom,
            )
            ControlButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Group,
                label = "成员",
                onClick = { onOpenSheet(RoomSheet.Members) },
            )
            ControlButton(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "聊天",
                onClick = { onOpenSheet(RoomSheet.Chat) },
            )
            ControlButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.MoreHoriz,
                label = "更多",
                onClick = { onOpenSheet(RoomSheet.More) },
            )
        }
    }
}

@Composable
private fun ControlButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    OutlinedButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("成员", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${roomState.participantsTotal.coerceAtLeast(roomState.participants.size)} 人参会",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onRefresh) {
                Text("刷新")
            }
        }
        if (isHost) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onMuteAll) {
                    Icon(Icons.Filled.MicOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("全体静音")
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onDisableAllVideo) {
                    Icon(Icons.Filled.VideocamOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("全体关摄像头")
                }
            }
        }
        if (roomState.participantsLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (roomState.participants.isEmpty()) {
            EmptyState("暂无成员")
        } else {
            LazyColumn(
                modifier = Modifier.height(420.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = participant.username, modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (participant.isLocal) "${participant.username}（我）" else participant.username,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                if (participant.role == ParticipantRole.Host) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(onClick = {}, label = { Text("主持人") })
                }
            }
            Text(
                listOf(participant.status.label, participant.joinedAt.take(16)).filter(String::isNotBlank).joinToString(" · "),
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
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (canModerate) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "成员操作")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (participant.audioEnabled) "静音" else "允许开麦") },
                            leadingIcon = {
                                Icon(if (participant.audioEnabled) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onToggleAudio(participant)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (participant.videoEnabled) "关闭摄像头" else "允许开摄像头") },
                            leadingIcon = {
                                Icon(if (participant.videoEnabled) Icons.Filled.VideocamOff else Icons.Filled.Videocam, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onToggleVideo(participant)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("移出会议") },
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
        Text("更多操作", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyVerticalGrid(
            modifier = Modifier.height(220.dp),
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                listOf(
                    MoreAction(
                        label = if (roomState.screenSharing) "停止共享" else "屏幕共享",
                        icon = Icons.AutoMirrored.Filled.ScreenShare,
                        selected = roomState.screenSharing,
                        onClick = onToggleScreenShare,
                    ),
                    MoreAction(
                        label = if (roomState.handRaised) "取消举手" else "举手",
                        icon = Icons.Filled.PanTool,
                        selected = roomState.handRaised,
                        onClick = onToggleHandRaised,
                    ),
                    MoreAction("切换摄像头", Icons.Filled.CameraAlt, onClick = onSwitchCamera),
                    MoreAction("会议设置", Icons.Filled.Settings, onClick = onOpenMeetingSettings),
                    MoreAction(
                        label = if (roomState.captionsEnabled) "关闭字幕" else "字幕",
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("音频输出", fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                availableAudioRoutes.forEach { route ->
                    FilterChip(
                        selected = selectedAudioRoute == route,
                        onClick = { onAudioRouteSelected(route) },
                        label = { Text(route.label) },
                    )
                }
            }
        }
        if (isHost) {
            HorizontalDivider()
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCloseMeeting,
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("关闭会议")
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
    OutlinedButton(
        modifier = Modifier.fillMaxSize(),
        onClick = action.onClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = if (action.selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(RoomSheet.Chat.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (roomState.chatMessages.isEmpty()) {
            EmptyState("暂无聊天消息")
        } else {
            LazyColumn(
                modifier = Modifier.height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(roomState.chatMessages.takeLast(30), key = { it.id }) { message ->
                    Text("${message.senderName}: ${message.content}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("输入消息") },
                singleLine = true,
            )
            Button(
                onClick = {
                    onSendMessage(draft)
                    draft = ""
                },
            ) {
                Text("发送")
            }
        }
        InfoRow("连接", if (roomState.socketConnected) "Socket 已连接" else "Socket 未连接")
    }
}

@Composable
private fun Avatar(
    name: String,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.toString() ?: "会",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
