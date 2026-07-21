package com.theoyu.thesis.android.feature.main

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomScreen(
    roomState: RoomUiState,
    onToggleAudio: () -> Unit,
    onToggleVideo: () -> Unit,
    onLeaveRoom: () -> Unit,
    onOpenSheet: (RoomSheet) -> Unit,
    onCloseSheet: () -> Unit,
    onSendMessage: (String) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val participants = roomState.participants.ifEmpty {
        listOf(RoomParticipant(peerId = "local", username = "我", isLocal = true))
    }
    val activeSpeaker = participants.firstOrNull { it.peerId == roomState.activeSpeakerPeerId }
        ?: participants.first()

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
            )
        } else {
            PortraitVideoLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 104.dp),
                activeSpeaker = activeSpeaker,
                participants = participants,
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
            RoomSheetContent(
                sheet = sheet,
                roomState = roomState,
                onSendMessage = onSendMessage,
            )
        }
    }
}

@Composable
private fun PortraitVideoLayout(
    modifier: Modifier,
    activeSpeaker: RoomParticipant,
    participants: List<RoomParticipant>,
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
                prominent = false,
            )
        }
    }
}

@Composable
private fun VideoTile(
    modifier: Modifier,
    participant: RoomParticipant,
    prominent: Boolean,
) {
    Card(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (participant.isLocal && participant.videoEnabled) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "本地视频",
                    style = if (prominent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (prominent) 88.dp else 48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(participant.username.firstOrNull()?.toString() ?: "会")
                    }
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
                Text(if (participant.audioEnabled) "麦开" else "静音", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
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
                    text = "当前发言人：${activeSpeaker.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onToggleAudio) {
                Text(if (roomState.audioEnabled) "静音" else "开麦")
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onToggleVideo) {
                Text(if (roomState.videoEnabled) "关视频" else "开视频")
            }
            Button(modifier = Modifier.weight(1f), onClick = onLeaveRoom) {
                Text("挂断")
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { onOpenSheet(RoomSheet.Members) }) {
                Text("成员")
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { onOpenSheet(RoomSheet.Chat) }) {
                Text("聊天")
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { onOpenSheet(RoomSheet.More) }) {
                Text("更多")
            }
        }
    }
}

@Composable
private fun RoomSheetContent(
    sheet: RoomSheet,
    roomState: RoomUiState,
    onSendMessage: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(sheet.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        when (sheet) {
            RoomSheet.Members -> {
                roomState.participants.forEach { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(modifier = Modifier.weight(1f), text = participant.username)
                        Text(
                            text = listOf(
                                if (participant.audioEnabled) "麦克风开" else "静音",
                                if (participant.videoEnabled) "视频开" else "视频关",
                            ).joinToString(" / "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            RoomSheet.Chat -> {
                var draft by remember { mutableStateOf("") }
                if (roomState.chatMessages.isEmpty()) {
                    EmptyState("暂无聊天消息")
                } else {
                    roomState.chatMessages.takeLast(8).forEach { message ->
                        Text("${message.senderName}: ${message.content}", style = MaterialTheme.typography.bodyMedium)
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
            }

            RoomSheet.More -> {
                InfoRow("会议号", roomState.meeting?.roomNo?.ifBlank { "-" } ?: "-")
                InfoRow("连接", if (roomState.socketConnected) "Socket 已连接" else "Socket 未连接")
                InfoRow("网络", roomState.networkQuality.label)
                InfoRow("布局", "竖屏主讲人优先，横屏紧凑宫格")
            }
        }
    }
}
