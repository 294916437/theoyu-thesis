package com.theoyu.thesis.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.theoyu.thesis.android.core.network.ApiClient
import com.theoyu.thesis.android.core.network.ApiClientConfig
import com.theoyu.thesis.android.core.network.ApiServiceFactory
import com.theoyu.thesis.android.core.network.UnauthorizedHandler
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.feature.auth.AuthScreen
import com.theoyu.thesis.android.feature.auth.AuthViewModel
import com.theoyu.thesis.android.feature.auth.AuthViewModelFactory
import com.theoyu.thesis.android.feature.main.MainFrame
import com.theoyu.thesis.android.feature.splash.SplashScreen
import com.theoyu.thesis.android.ui.theme.BlueSkyTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val sessionStore: SessionStore by lazy {
        SessionStore(applicationContext)
    }

    private val apiServiceFactory: ApiServiceFactory by lazy {
        ApiServiceFactory(
            ApiClient.createRetrofit(
                config = ApiClientConfig(
                    baseUrl = getString(R.string.api_base_url),
                    enableHttpLogging = false,
                ),
                tokenProvider = sessionStore,
                unauthorizedHandler = UnauthorizedHandler {
                    sessionStore.clearSession()
                },
            ),
        )
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            authRepository = apiServiceFactory.authRepository(),
            userRepository = apiServiceFactory.userRepository(),
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
) {
    var destination by remember { mutableStateOf(AppDestination.Splash) }
    val authState by authViewModel.uiState.collectAsState()

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

    when (destination) {
        AppDestination.Splash -> SplashScreen()
        AppDestination.Auth -> AuthScreen(
            uiState = authState,
            onModeSelected = authViewModel::selectMode,
            onPhoneChanged = authViewModel::onPhoneChanged,
            onCodeChanged = authViewModel::onCodeChanged,
            onAgreeTermsChanged = authViewModel::onAgreeTermsChanged,
            onSendCode = authViewModel::sendVerificationCode,
            onSubmit = authViewModel::submit,
            onMessageShown = authViewModel::consumeMessage,
        )

        AppDestination.Main -> MainFrame()
    }
}

private const val SPLASH_DURATION_MILLIS = 900L
