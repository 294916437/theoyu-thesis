package com.theoyu.thesis.android.feature.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theoyu.thesis.android.ui.theme.BlueSkyTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFrame(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    MainFrameContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTabSelected = viewModel::selectTab,
        onHomeMeetingNoChanged = viewModel::updateHomeMeetingNo,
        onOpenJoinMeeting = viewModel::openJoinMeeting,
        onOpenCreateMeeting = viewModel::openCreateMeeting,
        onBack = viewModel::backToTabs,
        onJoinMeetingNoChanged = viewModel::updateJoinMeetingNo,
        onValidateMeeting = viewModel::validateJoinMeeting,
        onJoinValidatedMeeting = viewModel::joinValidatedMeeting,
        onCreateTitleChanged = viewModel::updateCreateTitle,
        onCreateTypeChanged = viewModel::updateCreateType,
        onCreateDateChanged = viewModel::updateCreateDate,
        onCreateTimeChanged = viewModel::updateCreateTime,
        onCreateDescriptionChanged = viewModel::updateCreateDescription,
        onCreateAudioChanged = viewModel::updateCreateAudio,
        onCreateVideoChanged = viewModel::updateCreateVideo,
        onCreateSubmit = viewModel::createMeeting,
        onPreJoinAudioChanged = viewModel::updatePreJoinAudio,
        onPreJoinVideoChanged = viewModel::updatePreJoinVideo,
        onPermissionsChanged = viewModel::updatePermissions,
        onAudioRoutesChanged = viewModel::updateAudioRoutes,
        onAudioRouteSelected = viewModel::selectAudioRoute,
        onOpenProfileEditor = viewModel::openProfileEditor,
        onDismissProfileEditor = viewModel::dismissProfileEditor,
        onProfileNicknameChanged = viewModel::updateProfileNickname,
        onSaveProfile = viewModel::saveProfile,
        onLogout = viewModel::logout,
        onEnterMeeting = viewModel::enterMeetingFromPreview,
        onLeaveRoom = viewModel::leaveRoom,
        onToggleRoomAudio = viewModel::toggleRoomAudio,
        onToggleRoomVideo = viewModel::toggleRoomVideo,
        onOpenRoomSheet = viewModel::openRoomSheet,
        onCloseRoomSheet = viewModel::closeRoomSheet,
        onSendRoomMessage = viewModel::sendRoomMessage,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFrameContent(
    uiState: MainUiState,
    snackbarHostState: SnackbarHostState,
    onTabSelected: (MainTab) -> Unit,
    onHomeMeetingNoChanged: (String) -> Unit,
    onOpenJoinMeeting: (String) -> Unit,
    onOpenCreateMeeting: (MeetingCreateType) -> Unit,
    onBack: () -> Unit,
    onJoinMeetingNoChanged: (String) -> Unit,
    onValidateMeeting: () -> Unit,
    onJoinValidatedMeeting: () -> Unit,
    onCreateTitleChanged: (String) -> Unit,
    onCreateTypeChanged: (MeetingCreateType) -> Unit,
    onCreateDateChanged: (LocalDate) -> Unit,
    onCreateTimeChanged: (LocalTime) -> Unit,
    onCreateDescriptionChanged: (String) -> Unit,
    onCreateAudioChanged: (Boolean) -> Unit,
    onCreateVideoChanged: (Boolean) -> Unit,
    onCreateSubmit: () -> Unit,
    onPreJoinAudioChanged: (Boolean) -> Unit,
    onPreJoinVideoChanged: (Boolean) -> Unit,
    onPermissionsChanged: (Boolean, Boolean) -> Unit,
    onAudioRoutesChanged: (List<AudioRoute>) -> Unit,
    onAudioRouteSelected: (AudioRoute) -> Unit,
    onOpenProfileEditor: () -> Unit,
    onDismissProfileEditor: () -> Unit,
    onProfileNicknameChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit,
    onEnterMeeting: () -> Unit,
    onLeaveRoom: () -> Unit,
    onToggleRoomAudio: () -> Unit,
    onToggleRoomVideo: () -> Unit,
    onOpenRoomSheet: (RoomSheet) -> Unit,
    onCloseRoomSheet: () -> Unit,
    onSendRoomMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState.route) {
        MainRoute.Tabs -> uiState.selectedTab.label
        MainRoute.CreateMeeting -> "创建会议"
        MainRoute.JoinMeeting -> "加入会议"
        MainRoute.PreJoin -> "会前预览"
        MainRoute.Room -> uiState.activeRoom.meeting?.title ?: "会议房间"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState.route != MainRoute.Room) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (uiState.route != MainRoute.Tabs) {
                            TextButton(onClick = onBack) { Text("返回") }
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.route == MainRoute.Tabs) {
                MainNavigationBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (uiState.route) {
                MainRoute.Tabs -> when (uiState.selectedTab) {
                    MainTab.Home -> HomeScreen(
                        uiState = uiState,
                        onMeetingNoChanged = onHomeMeetingNoChanged,
                        onJoinClick = { onOpenJoinMeeting(uiState.homeMeetingNo) },
                        onCreateInstant = { onOpenCreateMeeting(MeetingCreateType.Instant) },
                        onCreateScheduled = { onOpenCreateMeeting(MeetingCreateType.Scheduled) },
                        onRecentClick = { onTabSelected(MainTab.Meetings) },
                    )

                    MainTab.Meetings -> MeetingsScreen(
                        upcomingMeetings = uiState.upcomingMeetings,
                        recentMeetings = uiState.recentMeetings,
                    )

                    MainTab.Profile -> ProfileScreen(
                        uiState = uiState,
                        onEditProfile = onOpenProfileEditor,
                        onDismissEditor = onDismissProfileEditor,
                        onNicknameChanged = onProfileNicknameChanged,
                        onSaveProfile = onSaveProfile,
                        onLogout = onLogout,
                    )
                }

                MainRoute.CreateMeeting -> CreateMeetingScreen(
                    form = uiState.createForm,
                    isSubmitting = uiState.isSubmitting,
                    onTitleChanged = onCreateTitleChanged,
                    onTypeChanged = onCreateTypeChanged,
                    onDateChanged = onCreateDateChanged,
                    onTimeChanged = onCreateTimeChanged,
                    onDescriptionChanged = onCreateDescriptionChanged,
                    onAudioChanged = onCreateAudioChanged,
                    onVideoChanged = onCreateVideoChanged,
                    onSubmit = onCreateSubmit,
                )

                MainRoute.JoinMeeting -> JoinMeetingScreen(
                    meetingNo = uiState.joinMeetingNo,
                    error = uiState.joinError,
                    validatedMeeting = uiState.validatedMeeting,
                    isSubmitting = uiState.isSubmitting,
                    onMeetingNoChanged = onJoinMeetingNoChanged,
                    onValidate = onValidateMeeting,
                    onJoin = onJoinValidatedMeeting,
                )

                MainRoute.PreJoin -> PreJoinScreen(
                    uiState = uiState,
                    onAudioChanged = onPreJoinAudioChanged,
                    onVideoChanged = onPreJoinVideoChanged,
                    onPermissionsChanged = onPermissionsChanged,
                    onAudioRoutesChanged = onAudioRoutesChanged,
                    onAudioRouteSelected = onAudioRouteSelected,
                    onEnterMeeting = onEnterMeeting,
                )

                MainRoute.Room -> RoomScreen(
                    roomState = uiState.activeRoom,
                    onToggleAudio = onToggleRoomAudio,
                    onToggleVideo = onToggleRoomVideo,
                    onLeaveRoom = onLeaveRoom,
                    onOpenSheet = onOpenRoomSheet,
                    onCloseSheet = onCloseRoomSheet,
                    onSendMessage = onSendRoomMessage,
                )
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MainNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    NavigationBar {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Text(tab.iconText, style = MaterialTheme.typography.labelLarge) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: MainUiState,
    onMeetingNoChanged: (String) -> Unit,
    onJoinClick: () -> Unit,
    onCreateInstant: () -> Unit,
    onCreateScheduled: () -> Unit,
    onRecentClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { UserSummaryCard(userSummary = uiState.userSummary) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("加入会议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.homeMeetingNo,
                        onValueChange = onMeetingNoChanged,
                        label = { Text("会议号") },
                        placeholder = { Text("输入会议号") },
                        singleLine = true,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.homeMeetingNo.isNotBlank(),
                        onClick = onJoinClick,
                    ) {
                        Text("加入会议")
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onCreateInstant,
                ) {
                    Text("立即会议")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCreateScheduled,
                ) {
                    Text("预约会议")
                }
            }
        }
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRecentClick),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("最近会议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${uiState.recentMeetings.size} 条会议记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("查看", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Text("即将开始", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (uiState.upcomingMeetings.isEmpty()) {
            item { EmptyState(text = "暂无即将开始的会议") }
        } else {
            items(uiState.upcomingMeetings.take(3), key = { it.roomId + it.roomNo + it.title }) { meeting ->
                MeetingSummaryCard(meeting = meeting)
            }
        }
    }
}

@Composable
private fun UserSummaryCard(userSummary: UserSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userSummary.displayName.firstOrNull()?.toString() ?: "我",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userSummary.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (userSummary.phone.isNotBlank()) userSummary.phone else "用户 ID ${userSummary.userId.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MeetingsScreen(
    upcomingMeetings: List<MeetingSummary>,
    recentMeetings: List<MeetingSummary>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("即将开始") }
        if (upcomingMeetings.isEmpty()) {
            item { EmptyState("暂无预约会议") }
        } else {
            items(upcomingMeetings, key = { "upcoming-${it.roomId}-${it.roomNo}" }) { MeetingSummaryCard(it) }
        }
        item { SectionTitle("最近会议") }
        if (recentMeetings.isEmpty()) {
            item { EmptyState("暂无最近会议") }
        } else {
            items(recentMeetings, key = { "recent-${it.roomId}-${it.roomNo}" }) { MeetingSummaryCard(it) }
        }
    }
}

@Composable
private fun ProfileScreen(
    uiState: MainUiState,
    onEditProfile: () -> Unit,
    onDismissEditor: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHeader(userSummary = uiState.userSummary)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsRow("编辑资料", "修改昵称和头像信息", onEditProfile)
                        HorizontalDivider()
                        SettingsRow("在线状态", if (uiState.userSummary.online) "在线" else "离线", null)
                        HorizontalDivider()
                        SettingsRow("手机号", uiState.userSummary.phone.ifBlank { "-" }, null)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            tonalElevation = 3.dp,
        ) {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                enabled = !uiState.isLoggingOut,
                onClick = onLogout,
            ) {
                Text(if (uiState.isLoggingOut) "退出中..." else "退出登录")
            }
        }
    }

    if (uiState.profileEditOpen) {
        AlertDialog(
            onDismissRequest = onDismissEditor,
            title = { Text("编辑资料") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.profileEditForm.nickname,
                        onValueChange = onNicknameChanged,
                        label = { Text("昵称") },
                        singleLine = true,
                    )
                    Text(
                        "手机号由登录账号绑定，当前仅支持修改昵称。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSubmitting,
                    onClick = onSaveProfile,
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEditor) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ProfileHeader(userSummary: UserSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userSummary.displayName.firstOrNull()?.toString() ?: "我",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(userSummary.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = userSummary.phone.ifBlank { "用户 ID ${userSummary.userId.ifBlank { "-" }}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (userSummary.online) "在线" else "离线",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (userSummary.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Text("进入", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMeetingScreen(
    form: CreateMeetingForm,
    isSubmitting: Boolean,
    onTitleChanged: (String) -> Unit,
    onTypeChanged: (MeetingCreateType) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onTimeChanged: (LocalTime) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAudioChanged: (Boolean) -> Unit,
    onVideoChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = form.title,
                    onValueChange = onTitleChanged,
                    label = { Text("会议标题") },
                    placeholder = { Text("请输入会议标题") },
                    singleLine = true,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MeetingCreateType.entries.forEach { type ->
                        FilterChip(
                            selected = form.type == type,
                            onClick = { onTypeChanged(type) },
                            label = { Text(type.label) },
                        )
                    }
                }
            }
            item {
                DateTimePickerFields(
                    enabled = form.type == MeetingCreateType.Scheduled,
                    date = form.startDate,
                    time = form.startTime,
                    onDateChanged = onDateChanged,
                    onTimeChanged = onTimeChanged,
                )
            }
            item {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    value = form.description,
                    onValueChange = onDescriptionChanged,
                    label = { Text("会议说明") },
                    placeholder = { Text("填写会议议题或参会说明") },
                    maxLines = 4,
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("默认入会状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SwitchRow("麦克风默认开启", form.audioEnabled, onAudioChanged)
                        SwitchRow("摄像头默认开启", form.videoEnabled, onVideoChanged)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            tonalElevation = 3.dp,
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                enabled = !isSubmitting,
                onClick = onSubmit,
            ) {
                Text(if (isSubmitting) "创建中..." else "创建会议")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerFields(
    enabled: Boolean,
    date: LocalDate,
    time: LocalTime,
    onDateChanged: (LocalDate) -> Unit,
    onTimeChanged: (LocalTime) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (enabled) "开始时间" else "立即会议将使用当前时间",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { showDatePicker = true },
            ) {
                Text(date.format(DATE_FORMATTER), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { showTimePicker = true },
            ) {
                Text(time.format(TIME_FORMATTER), maxLines = 1)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChanged(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

@Composable
private fun JoinMeetingScreen(
    meetingNo: String,
    error: JoinMeetingError?,
    validatedMeeting: MeetingSummary?,
    isSubmitting: Boolean,
    onMeetingNoChanged: (String) -> Unit,
    onValidate: () -> Unit,
    onJoin: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = meetingNo,
                onValueChange = onMeetingNoChanged,
                label = { Text("会议号") },
                placeholder = { Text("请输入会议号") },
                isError = error != null,
                supportingText = error?.let { { Text(it.message) } },
                singleLine = true,
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && meetingNo.isNotBlank(),
                onClick = if (validatedMeeting == null) onValidate else onJoin,
            ) {
                Text(
                    when {
                        isSubmitting -> "处理中..."
                        validatedMeeting == null -> "验证会议"
                        else -> "加入会议"
                    },
                )
            }
        }
        validatedMeeting?.let { meeting ->
            item {
                Text("会议摘要", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item { MeetingSummaryCard(meeting = meeting) }
        }
    }
}

@Composable
private fun PreJoinScreen(
    uiState: MainUiState,
    onAudioChanged: (Boolean) -> Unit,
    onVideoChanged: (Boolean) -> Unit,
    onPermissionsChanged: (Boolean, Boolean) -> Unit,
    onAudioRoutesChanged: (List<AudioRoute>) -> Unit,
    onAudioRouteSelected: (AudioRoute) -> Unit,
    onEnterMeeting: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        onPermissionsChanged(
            result[Manifest.permission.CAMERA] == true,
            result[Manifest.permission.RECORD_AUDIO] == true,
        )
    }

    LaunchedEffect(Unit) {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        onPermissionsChanged(cameraGranted, audioGranted)
        if (!cameraGranted || !audioGranted) {
            permissionLauncher.launch(permissions)
        }
        onAudioRoutesChanged(context.availableCommunicationRoutes())
    }

    LaunchedEffect(uiState.audioRoute) {
        context.selectCommunicationRoute(uiState.audioRoute)
    }

    val canJoin = uiState.cameraPermissionGranted &&
        uiState.audioPermissionGranted &&
        (uiState.createForm.audioEnabled || uiState.createForm.videoEnabled)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreJoinVideoPreview(
                    enabled = uiState.createForm.videoEnabled,
                    cameraPermissionGranted = uiState.cameraPermissionGranted,
                    lifecycleOwner = lifecycleOwner,
                )
            }
            uiState.permissionHint?.let { hint ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("入会身份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(uiState.userSummary.displayName.firstOrNull()?.toString() ?: "我")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uiState.userSummary.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    uiState.userSummary.phone.ifBlank { "用户 ID ${uiState.userSummary.userId.ifBlank { "-" }}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                        InfoRow("会议", uiState.preJoinMeeting?.title ?: "-")
                        InfoRow("会议号", uiState.preJoinMeeting?.roomNo?.ifBlank { "-" } ?: "-")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("设备检查", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SwitchRow("麦克风", uiState.createForm.audioEnabled, onAudioChanged)
                        SwitchRow("摄像头", uiState.createForm.videoEnabled, onVideoChanged)
                        Text("音频输出", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.availableAudioRoutes.forEach { route ->
                                FilterChip(
                                    selected = uiState.audioRoute == route,
                                    onClick = { onAudioRouteSelected(route) },
                                    label = { Text(route.label) },
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            tonalElevation = 3.dp,
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                enabled = canJoin,
                onClick = onEnterMeeting,
            ) {
                Text(
                    when {
                        uiState.permissionHint != null -> "完成设备权限检查"
                        !uiState.createForm.audioEnabled && !uiState.createForm.videoEnabled -> "至少开启一个设备"
                        else -> "加入会议"
                    },
                )
            }
        }
    }
}

@Composable
private fun PreJoinVideoPreview(
    enabled: Boolean,
    cameraPermissionGranted: Boolean,
    lifecycleOwner: LifecycleOwner,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (enabled && cameraPermissionGranted) {
                CameraPreview(lifecycleOwner = lifecycleOwner)
            } else {
                Text(
                    text = if (!cameraPermissionGranted) "等待相机权限" else "摄像头已关闭",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(lifecycleOwner: LifecycleOwner) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = CameraPreviewUseCase.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                    )
                },
                ContextCompat.getMainExecutor(context),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomScreen(
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
            gridItems(participants.filterNot { it.peerId == activeSpeaker.peerId }.take(6), key = { it.peerId }) { participant ->
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
        gridItems(participants, key = { it.peerId }) { participant ->
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

@Composable
private fun MeetingSummaryCard(meeting: MeetingSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meeting.roomNo.isNotBlank()) {
                Text("会议号 ${meeting.roomNo}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = meeting.startTime.ifBlank { "时间待定" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                meeting.hostName.takeIf(String::isNotBlank)?.let {
                    Text("主持人 $it", style = MaterialTheme.typography.bodySmall)
                }
                meeting.participantCount?.let {
                    Text("$it 人", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyState(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(88.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}

private fun Context.availableCommunicationRoutes(): List<AudioRoute> {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val routes = mutableListOf(AudioRoute.Speaker)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val hasEarpiece = audioManager.availableCommunicationDevices.any {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        }
        if (hasEarpiece) {
            routes += AudioRoute.Earpiece
        }
    } else {
        @Suppress("DEPRECATION")
        if (!audioManager.isSpeakerphoneOn) {
            routes += AudioRoute.Earpiece
        } else {
            routes += AudioRoute.Earpiece
        }
    }
    return routes.distinct()
}

private fun Context.selectCommunicationRoute(route: AudioRoute) {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val type = when (route) {
            AudioRoute.Speaker -> AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            AudioRoute.Earpiece -> AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        }
        audioManager.availableCommunicationDevices
            .firstOrNull { it.type == type }
            ?.let(audioManager::setCommunicationDevice)
    } else {
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = route == AudioRoute.Speaker
    }
}

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@ComposePreview(showBackground = true)
@Composable
private fun MainFramePreview() {
    BlueSkyTheme {
        MainFrameContent(
            uiState = MainUiState(
                userSummary = UserSummary(userId = "1", displayName = "Theo", phone = "13800000000"),
                upcomingMeetings = listOf(
                    MeetingSummary(roomId = "1", roomNo = "123456", title = "项目进度会议", startTime = "2026-07-21 15:00"),
                ),
                recentMeetings = listOf(
                    MeetingSummary(roomId = "2", roomNo = "654321", title = "团队周会", startTime = "2026-07-20 10:00"),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onTabSelected = {},
            onHomeMeetingNoChanged = {},
            onOpenJoinMeeting = {},
            onOpenCreateMeeting = {},
            onBack = {},
            onJoinMeetingNoChanged = {},
            onValidateMeeting = {},
            onJoinValidatedMeeting = {},
            onCreateTitleChanged = {},
            onCreateTypeChanged = {},
            onCreateDateChanged = {},
            onCreateTimeChanged = {},
            onCreateDescriptionChanged = {},
            onCreateAudioChanged = {},
            onCreateVideoChanged = {},
            onCreateSubmit = {},
            onPreJoinAudioChanged = {},
            onPreJoinVideoChanged = {},
            onPermissionsChanged = { _, _ -> },
            onAudioRoutesChanged = {},
            onAudioRouteSelected = {},
            onOpenProfileEditor = {},
            onDismissProfileEditor = {},
            onProfileNicknameChanged = {},
            onSaveProfile = {},
            onLogout = {},
            onEnterMeeting = {},
            onLeaveRoom = {},
            onToggleRoomAudio = {},
            onToggleRoomVideo = {},
            onOpenRoomSheet = {},
            onCloseRoomSheet = {},
            onSendRoomMessage = {},
        )
    }
}
