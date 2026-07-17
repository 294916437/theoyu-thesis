package com.theoyu.thesis.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun selectMode(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, message = null) }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update {
            it.copy(
                phone = phone.filter(Char::isDigit).take(PHONE_MAX_LENGTH),
                phoneError = null,
                message = null,
            )
        }
    }

    fun onCodeChanged(code: String) {
        _uiState.update {
            it.copy(
                code = code.filter(Char::isDigit).take(CODE_LENGTH),
                codeError = null,
                message = null,
            )
        }
    }

    fun onAgreeTermsChanged(agreed: Boolean) {
        _uiState.update { it.copy(agreeTerms = agreed, message = null) }
    }

    fun sendVerificationCode() {
        val state = _uiState.value
        if (!state.isPhoneValid) {
            _uiState.update { it.copy(phoneError = "请输入正确的手机号", message = "请输入正确的手机号") }
            return
        }

        if (state.countdownSeconds > 0) {
            _uiState.update { it.copy(message = "请稍后再试") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(sendingCode = true, message = null) }

            when (val result = authRepository.getVerificationCode(state.phone)) {
                is ApiResult.Success -> {
                    if (result.data.isSuccessfulResponse()) {
                        _uiState.update { it.copy(message = "验证码已发送") }
                        startCountdown()
                    } else {
                        _uiState.update {
                            it.copy(message = result.data.responseMessage() ?: "发送验证码失败")
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
            _uiState.update { it.copy(loading = true, message = null) }

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
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun handleLoginResponse(response: JsonElement) {
        if (!response.isSuccessfulResponse()) {
            _uiState.update { it.copy(codeError = "验证码错误", message = "验证码错误") }
            return
        }

        val data = response.asJsonObjectOrNull()?.getAsJsonObject("data")
        val token = data?.get("token")?.asStringOrNull()
        val userId = data?.get("userId")?.asStringOrNull()

        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            _uiState.update { it.copy(message = "登录响应缺少 token 或 userId") }
            return
        }

        sessionStore.saveSession(token, userId)
        userRepository.getUserProfile(userId)
        _uiState.update { it.copy(authenticated = true, message = "登录成功") }
    }

    private fun validateForSubmit(state: AuthUiState): ValidationError? =
        when {
            !state.agreeTerms -> ValidationError(message = "请先同意用户协议和隐私政策")
            !state.isPhoneValid -> ValidationError(phoneError = "请输入正确的手机号", message = "请输入正确的手机号")
            state.code.length != CODE_LENGTH -> ValidationError(codeError = "请输入6位验证码", message = "请输入正确的验证码")
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
        val phoneError: String? = null,
        val codeError: String? = null,
        val message: String,
    ) {
        fun applyTo(state: AuthUiState): AuthUiState =
            state.copy(
                phoneError = phoneError,
                codeError = codeError,
                message = message,
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
