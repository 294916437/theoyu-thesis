package com.theoyu.thesis.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.data.auth.AuthRepository
import com.theoyu.thesis.android.data.user.UserRepository

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionStore: SessionStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository, userRepository, sessionStore) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
