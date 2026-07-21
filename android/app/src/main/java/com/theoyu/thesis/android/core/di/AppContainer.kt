package com.theoyu.thesis.android.core.di

import android.content.Context
import com.theoyu.thesis.android.R
import com.theoyu.thesis.android.core.network.ApiClient
import com.theoyu.thesis.android.core.network.ApiClientConfig
import com.theoyu.thesis.android.core.network.ApiServiceFactory
import com.theoyu.thesis.android.core.network.UnauthorizedHandler
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.core.signaling.SocketIoClient
import com.theoyu.thesis.android.data.auth.AuthRepository
import com.theoyu.thesis.android.data.meeting.RoomRepository
import com.theoyu.thesis.android.data.user.UserRepository

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val sessionStore: SessionStore = SessionStore(appContext)
    val socketIoClient: SocketIoClient by lazy {
        SocketIoClient()
    }

    private val apiServiceFactory: ApiServiceFactory by lazy {
        ApiServiceFactory(
            ApiClient.createRetrofit(
                config = ApiClientConfig(
                    baseUrl = appContext.getString(R.string.api_base_url),
                    enableHttpLogging = false,
                ),
                tokenProvider = sessionStore,
                unauthorizedHandler = UnauthorizedHandler {
                    sessionStore.clearSession()
                },
            ),
        )
    }

    val authRepository: AuthRepository by lazy {
        apiServiceFactory.authRepository()
    }

    val userRepository: UserRepository by lazy {
        apiServiceFactory.userRepository()
    }

    val roomRepository: RoomRepository by lazy {
        apiServiceFactory.roomRepository()
    }
}
