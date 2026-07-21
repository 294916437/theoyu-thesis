package com.theoyu.thesis.android.feature.main

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
) {
    val title = when (uiState.route) {
        MainRoute.Tabs -> uiState.selectedTab.label
        MainRoute.CreateMeeting -> "创建会议"
        MainRoute.JoinMeeting -> "加入会议"
        MainRoute.PreJoin -> "会前预览"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (uiState.route != MainRoute.Tabs) {
                        TextButton(onClick = onBack) { Text("返回") }
                    }
                },
            )
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

                    MainTab.Profile -> ProfileScreen(userSummary = uiState.userSummary)
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

                MainRoute.PreJoin -> PreJoinScreen(meeting = uiState.preJoinMeeting)
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
private fun ProfileScreen(userSummary: UserSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { UserSummaryCard(userSummary) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("账号信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    InfoRow("用户 ID", userSummary.userId.ifBlank { "-" })
                    InfoRow("手机号", userSummary.phone.ifBlank { "-" })
                    InfoRow("登录状态", "已登录")
                }
            }
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
private fun PreJoinScreen(meeting: MeetingSummary?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("会前预览", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "摄像头、麦克风和背景效果预览区域占位。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                    InfoRow("会议", meeting?.title ?: "-")
                    InfoRow("会议号", meeting?.roomNo?.ifBlank { "-" } ?: "-")
                    InfoRow("开始时间", meeting?.startTime?.ifBlank { "-" } ?: "-")
                }
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

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Preview(showBackground = true)
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
        )
    }
}
