package com.theoyu.thesis.android.feature.auth

data class AuthUiState(
    val phone: String = "",
    val code: String = "",
    val agreeTerms: Boolean = false,
    val loading: Boolean = false,
    val sendingCode: Boolean = false,
    val countdownSeconds: Int = 0,
    val phoneErrorResId: Int? = null,
    val codeErrorResId: Int? = null,
    val messageResId: Int? = null,
    val message: String? = null,
    val authenticated: Boolean = false,
) {
    val isPhoneValid: Boolean = PHONE_REGEX.matches(phone)
    val canSendCode: Boolean = isPhoneValid && countdownSeconds == 0 && !sendingCode
    val canSubmit: Boolean =
        isPhoneValid && code.length == VERIFICATION_CODE_LENGTH && agreeTerms && !loading

    companion object {
        private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")
        private const val VERIFICATION_CODE_LENGTH = 6
    }
}
