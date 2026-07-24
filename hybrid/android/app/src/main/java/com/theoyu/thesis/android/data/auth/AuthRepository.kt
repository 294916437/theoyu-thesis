package com.theoyu.thesis.android.data.auth

import com.google.gson.JsonElement
import com.theoyu.thesis.android.core.network.ApiResult
import com.theoyu.thesis.android.core.network.safeApiCall

class AuthRepository(
    private val apiService: AuthApiService,
) {
    suspend fun getVerificationCode(phone: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.getVerificationCode(VerificationCodeRequest(phone))
        }

    suspend fun login(loginRequest: Map<String, Any?>): ApiResult<JsonElement> =
        safeApiCall {
            apiService.login(loginRequest)
        }

    suspend fun logout(): ApiResult<JsonElement> =
        safeApiCall {
            apiService.logout()
        }
}
