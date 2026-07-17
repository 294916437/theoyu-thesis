package com.theoyu.thesis.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@Immutable
data class MeetingSemanticColors(
    val speaking: Color,
    val recording: Color,
    val screenSharing: Color,
    val micMuted: Color,
    val networkPoor: Color,
    val videoTile: Color,
    val onVideoTile: Color,
)

val LightMeetingSemanticColors = MeetingSemanticColors(
    speaking = SpeakingGreen,
    recording = RecordingRed,
    screenSharing = ScreenShareAmber,
    micMuted = MeetingTextMuted,
    networkPoor = NetworkPoorOrange,
    videoTile = VideoTile,
    onVideoTile = Color.White,
)

val DarkMeetingSemanticColors = MeetingSemanticColors(
    speaking = SpeakingGreen,
    recording = MeetingErrorDark,
    screenSharing = ScreenShareAmber,
    micMuted = MeetingTextMutedDark,
    networkPoor = NetworkPoorOrange,
    videoTile = VideoTileDark,
    onVideoTile = Color.White,
)

val LocalMeetingSemanticColors = staticCompositionLocalOf {
    LightMeetingSemanticColors
}

val MaterialTheme.meetingColors: MeetingSemanticColors
    @Composable
    get() = LocalMeetingSemanticColors.current
