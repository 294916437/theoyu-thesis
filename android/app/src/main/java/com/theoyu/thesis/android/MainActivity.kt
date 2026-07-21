package com.theoyu.thesis.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.feature.auth.AuthScreen
import com.theoyu.thesis.android.feature.auth.AuthViewModel
import com.theoyu.thesis.android.feature.auth.AuthViewModelFactory
import com.theoyu.thesis.android.feature.main.MainFrame
import com.theoyu.thesis.android.feature.main.MainViewModel
import com.theoyu.thesis.android.feature.main.MainViewModelFactory
import com.theoyu.thesis.android.feature.splash.SplashScreen
import com.theoyu.thesis.android.ui.theme.BlueSkyTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val appContainer by lazy {
        (application as BlueSkyApplication).appContainer
    }

    private val sessionStore: SessionStore by lazy {
        appContainer.sessionStore
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            authRepository = appContainer.authRepository,
            userRepository = appContainer.userRepository,
            sessionStore = sessionStore,
        )
    }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            roomRepository = appContainer.roomRepository,
            userRepository = appContainer.userRepository,
            authRepository = appContainer.authRepository,
            sessionStore = sessionStore,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlueSkyTheme {
                BlueSkyApp(
                    sessionStore = sessionStore,
                    authViewModel = authViewModel,
                    mainViewModel = mainViewModel,
                )
            }
        }
    }
}

private enum class AppDestination {
    Splash,
    Auth,
    Main,
}

@Composable
private fun BlueSkyApp(
    sessionStore: SessionStore,
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
) {
    var destination by remember { mutableStateOf(AppDestination.Splash) }
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MILLIS)
        destination = if (sessionStore.hasSession()) {
            AppDestination.Main
        } else {
            AppDestination.Auth
        }
    }

    LaunchedEffect(authState.authenticated) {
        if (authState.authenticated) {
            destination = AppDestination.Main
        }
    }

    LaunchedEffect(mainState.loggedOut) {
        if (mainState.loggedOut) {
            authViewModel.resetAuthenticationState()
            destination = AppDestination.Auth
        }
    }

    when (destination) {
        AppDestination.Splash -> SplashScreen()
        AppDestination.Auth -> AuthScreen(
            uiState = authState,
            onPhoneChanged = authViewModel::onPhoneChanged,
            onCodeChanged = authViewModel::onCodeChanged,
            onAgreeTermsChanged = authViewModel::onAgreeTermsChanged,
            onSendCode = authViewModel::sendVerificationCode,
            onSubmit = authViewModel::submit,
            onMessageShown = authViewModel::consumeMessage,
        )

        AppDestination.Main -> MainFrame(viewModel = mainViewModel)
    }
}

private const val SPLASH_DURATION_MILLIS = 900L
