package com.theoyu.thesis.android.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.theoyu.thesis.android.core.network.AuthTokenProvider
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "blue_sky_session",
)

class SessionStore(
    context: Context,
) : AuthTokenProvider {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedToken: String? = null

    val sessionFlow: Flow<AuthSession> = appContext.sessionDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AuthSession(
                token = preferences[KEY_TOKEN],
                userId = preferences[KEY_USER_ID],
            )
        }

    init {
        scope.launch {
            sessionFlow.collect { session ->
                cachedToken = session.token
            }
        }
    }

    override fun currentToken(): String? = cachedToken

    suspend fun currentSession(): AuthSession =
        sessionFlow.first().also { cachedToken = it.token }

    suspend fun hasSession(): Boolean =
        !currentSession().token.isNullOrBlank()

    suspend fun saveSession(token: String, userId: String) {
        appContext.sessionDataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
            preferences[KEY_USER_ID] = userId
        }
        cachedToken = token
    }

    fun clearSession() {
        cachedToken = null
        scope.launch {
            appContext.sessionDataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
    }
}

data class AuthSession(
    val token: String?,
    val userId: String?,
)
