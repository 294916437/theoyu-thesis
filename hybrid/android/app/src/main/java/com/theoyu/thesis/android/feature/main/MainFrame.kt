package com.theoyu.thesis.android.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theoyu.thesis.android.feature.meeting.create.CreateMeetingScreen
import com.theoyu.thesis.android.feature.meeting.home.HomeScreen
import com.theoyu.thesis.android.feature.meeting.join.JoinMeetingScreen
import com.theoyu.thesis.android.feature.meeting.list.MeetingsScreen
import com.theoyu.thesis.android.feature.meeting.prejoin.PreJoinScreen
import com.theoyu.thesis.android.feature.meeting.room.RoomScreen
import com.theoyu.thesis.android.feature.profile.ProfileScreen
import com.theoyu.thesis.android.ui.theme.BlueSkyTheme

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
        onRefreshParticipants = viewModel::refreshRoomParticipants,
        onHostToggleParticipantAudio = viewModel::hostToggleParticipantAudio,
        onHostToggleParticipantVideo = viewModel::hostToggleParticipantVideo,
        onRemoveParticipant = viewModel::removeParticipant,
        onToggleScreenShare = viewModel::toggleScreenShare,
        onToggleHandRaised = viewModel::toggleHandRaised,
        onSwitchCamera = viewModel::switchCamera,
        onToggleCaptions = viewModel::toggleCaptions,
        onOpenMeetingSettings = viewModel::openMeetingSettings,
        onCloseMeeting = viewModel::closeMeeting,
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
    onCreateDateChanged: (java.time.LocalDate) -> Unit,
    onCreateTimeChanged: (java.time.LocalTime) -> Unit,
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
    onRefreshParticipants: () -> Unit,
    onHostToggleParticipantAudio: (RoomParticipant) -> Unit,
    onHostToggleParticipantVideo: (RoomParticipant) -> Unit,
    onRemoveParticipant: (RoomParticipant) -> Unit,
    onToggleScreenShare: () -> Unit,
    onToggleHandRaised: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleCaptions: () -> Unit,
    onOpenMeetingSettings: () -> Unit,
    onCloseMeeting: () -> Unit,
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
                    onRefreshParticipants = onRefreshParticipants,
                    onHostToggleParticipantAudio = onHostToggleParticipantAudio,
                    onHostToggleParticipantVideo = onHostToggleParticipantVideo,
                    onRemoveParticipant = onRemoveParticipant,
                    onToggleScreenShare = onToggleScreenShare,
                    onToggleHandRaised = onToggleHandRaised,
                    onSwitchCamera = onSwitchCamera,
                    availableAudioRoutes = uiState.availableAudioRoutes,
                    selectedAudioRoute = uiState.audioRoute,
                    onAudioRouteSelected = onAudioRouteSelected,
                    onToggleCaptions = onToggleCaptions,
                    onOpenMeetingSettings = onOpenMeetingSettings,
                    onCloseMeeting = onCloseMeeting,
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
            onRefreshParticipants = {},
            onHostToggleParticipantAudio = {},
            onHostToggleParticipantVideo = {},
            onRemoveParticipant = {},
            onToggleScreenShare = {},
            onToggleHandRaised = {},
            onSwitchCamera = {},
            onToggleCaptions = {},
            onOpenMeetingSettings = {},
            onCloseMeeting = {},
            onSendRoomMessage = {},
        )
    }
}
