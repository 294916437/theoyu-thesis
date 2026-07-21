package com.theoyu.thesis.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theoyu.thesis.android.R
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.theoyu.thesis.android.core.network.ApiResult
import com.theoyu.thesis.android.core.session.SessionStore
import com.theoyu.thesis.android.data.auth.AuthRepository
import com.theoyu.thesis.android.data.user.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private var countdownJob: Job? = null

    fun onPhoneChanged(phone: String) {
        _uiState.update {
            it.copy(
                phone = phone.filter(Char::isDigit).take(PHONE_MAX_LENGTH),
                phoneErrorResId = null,
                message = null,
                messageResId = null,
            )
        }
    }

    fun onCodeChanged(code: String) {
        _uiState.update {
            it.copy(
                code = code.filter(Char::isDigit).take(CODE_LENGTH),
                codeErrorResId = null,
                message = null,
                messageResId = null,
            )
        }
    }

    fun onAgreeTermsChanged(agreed: Boolean) {
        _uiState.update { it.copy(agreeTerms = agreed, message = null, messageResId = null) }
    }

    fun sendVerificationCode() {
        val state = _uiState.value
        if (!state.isPhoneValid) {
            _uiState.update {
                it.copy(
                    phoneErrorResId = R.string.auth_error_invalid_phone,
                    messageResId = R.string.auth_error_invalid_phone,
                )
            }
            return
        }

        if (state.countdownSeconds > 0) {
            _uiState.update { it.copy(messageResId = R.string.auth_error_retry_later) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(sendingCode = true, message = null, messageResId = null) }

            when (val result = authRepository.getVerificationCode(state.phone)) {
                is ApiResult.Success -> {
                    if (result.data.isSuccessfulResponse()) {
                        _uiState.update { it.copy(messageResId = R.string.auth_message_code_sent) }
                        startCountdown()
                    } else {
                        _uiState.update {
                            it.copy(
                                message = result.data.responseMessage(),
                                messageResId = if (result.data.responseMessage() == null) {
                                    R.string.auth_error_send_code_failed
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }

                is ApiResult.Failure -> {
                    _uiState.update { it.copy(message = result.error.message) }
                }
            }

            _uiState.update { it.copy(sendingCode = false) }
        }
    }

    fun submit() {
        val state = _uiState.value
        val validationError = validateForSubmit(state)
        if (validationError != null) {
            _uiState.update { validationError.applyTo(it) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, message = null, messageResId = null) }

            val loginRequest = mapOf(
                "phone" to state.phone,
                "code" to state.code,
                "type" to VERIFICATION_LOGIN_TYPE,
            )

            when (val result = authRepository.login(loginRequest)) {
                is ApiResult.Success -> handleLoginResponse(result.data)
                is ApiResult.Failure -> _uiState.update { it.copy(message = result.error.message) }
            }

            _uiState.update { it.copy(loading = false) }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null, messageResId = null) }
    }

    private suspend fun handleLoginResponse(response: JsonElement) {
        if (!response.isSuccessfulResponse()) {
            _uiState.update {
                it.copy(
                    codeErrorResId = R.string.auth_error_wrong_code,
                    messageResId = R.string.auth_error_wrong_code,
                )
            }
            return
        }

        val data = response.asJsonObjectOrNull()?.getAsJsonObject("data")
        val token = data?.get("token")?.asStringOrNull()
        val userId = data?.get("userId")?.asStringOrNull()

        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            _uiState.update { it.copy(messageResId = R.string.auth_error_missing_login_payload) }
            return
        }

        sessionStore.saveSession(token, userId)
        userRepository.getUserProfile(userId)
        _uiState.update { it.copy(authenticated = true, messageResId = R.string.auth_message_login_success) }
    }

    private fun validateForSubmit(state: AuthUiState): ValidationError? =
        when {
            !state.agreeTerms -> ValidationError(messageResId = R.string.auth_error_agree_terms)
            !state.isPhoneValid -> ValidationError(
                phoneErrorResId = R.string.auth_error_invalid_phone,
                messageResId = R.string.auth_error_invalid_phone,
            )
            state.code.length != CODE_LENGTH -> ValidationError(
                codeErrorResId = R.string.auth_error_invalid_code_length,
                messageResId = R.string.auth_error_invalid_code,
            )
            else -> null
        }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in CODE_COUNTDOWN_SECONDS downTo 0) {
                _uiState.update { it.copy(countdownSeconds = remaining) }
                delay(ONE_SECOND_MILLIS)
            }
        }
    }

    private fun JsonElement.isSuccessfulResponse(): Boolean =
        asJsonObjectOrNull()?.get("success")?.asBooleanOrNull() == true

    private fun JsonElement.responseMessage(): String? {
        val body = asJsonObjectOrNull() ?: return null
        return body.get("notification")?.asStringOrNull()
            ?: body.get("message")?.asStringOrNull()
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        if (isJsonObject) asJsonObject else null

    private fun JsonElement.asStringOrNull(): String? =
        if (isJsonNull) null else runCatching { asString }.getOrNull()

    private fun JsonElement.asBooleanOrNull(): Boolean? =
        if (isJsonNull) null else runCatching { asBoolean }.getOrNull()

    private data class ValidationError(
        val phoneErrorResId: Int? = null,
        val codeErrorResId: Int? = null,
        val messageResId: Int,
    ) {
        fun applyTo(state: AuthUiState): AuthUiState =
            state.copy(
                phoneErrorResId = phoneErrorResId,
                codeErrorResId = codeErrorResId,
                messageResId = messageResId,
            )
    }

    private companion object {
        const val PHONE_MAX_LENGTH = 11
        const val CODE_LENGTH = 6
        const val CODE_COUNTDOWN_SECONDS = 180
        const val ONE_SECOND_MILLIS = 1_000L
        const val VERIFICATION_LOGIN_TYPE = 1
    }
}
