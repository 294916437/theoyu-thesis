package com.theoyu.thesis.android.core.session

import android.content.Context
import com.theoyu.thesis.android.core.network.AuthTokenProvider

class SessionStore(
    context: Context,
) : AuthTokenProvider {
    private val preferences = context.applicationContext.getSharedPreferences(
        SESSION_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun currentToken(): String? =
        preferences.getString(KEY_TOKEN, null)

    fun currentUserId(): String? =
        preferences.getString(KEY_USER_ID, null)

    fun hasSession(): Boolean =
        !currentToken().isNullOrBlank()

    fun saveSession(token: String, userId: String) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun clearSession() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val SESSION_PREFERENCES_NAME = "blue_sky_session"
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
    }
}
