package com.theoyu.thesis.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MeetingBlueDark,
    onPrimary = Color(0xFF003062),
    primaryContainer = MeetingBlueContainerDark,
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = MeetingTealDark,
    onSecondary = Color(0xFF003737),
    tertiary = MeetingGreenDark,
    onTertiary = Color(0xFF0E3900),
    error = MeetingErrorDark,
    onError = Color(0xFF690005),
    background = MeetingBackgroundDark,
    onBackground = MeetingTextDark,
    surface = MeetingSurfaceDark,
    onSurface = MeetingTextDark,
    surfaceVariant = MeetingSurfaceVariantDark,
    onSurfaceVariant = MeetingTextMutedDark,
    outline = MeetingOutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = MeetingBlue,
    onPrimary = Color.White,
    primaryContainer = MeetingBlueContainer,
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = MeetingTeal,
    onSecondary = Color.White,
    tertiary = MeetingGreen,
    onTertiary = Color.White,
    error = MeetingError,
    onError = Color.White,
    background = MeetingBackground,
    onBackground = MeetingText,
    surface = MeetingSurface,
    onSurface = MeetingText,
    surfaceVariant = MeetingSurfaceVariant,
    onSurfaceVariant = MeetingTextMuted,
    outline = MeetingOutline,
)

@Composable
fun BlueSkyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val meetingSemanticColors =
        if (darkTheme) DarkMeetingSemanticColors else LightMeetingSemanticColors

    CompositionLocalProvider(LocalMeetingSemanticColors provides meetingSemanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

@Composable
fun TheoyuMeetingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    BlueSkyTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content,
    )
}
