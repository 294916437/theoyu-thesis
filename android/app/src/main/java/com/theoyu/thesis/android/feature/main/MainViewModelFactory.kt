package com.theoyu.thesis.android.feature.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.core.signaling.SocketIoClient
import com.theoyu.thesis.android.data.auth.AuthRepository
import com.theoyu.thesis.android.data.meeting.RoomRepository
import com.theoyu.thesis.android.data.user.UserRepository

class MainViewModelFactory(
    private val appContext: Context,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val socketIoClient: SocketIoClient,
    private val sessionStore: SessionStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(appContext, roomRepository, userRepository, authRepository, socketIoClient, sessionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
