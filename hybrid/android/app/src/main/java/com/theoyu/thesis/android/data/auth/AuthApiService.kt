package com.theoyu.thesis.android.data.auth

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/verification/code/send")
    suspend fun getVerificationCode(@Body request: VerificationCodeRequest): JsonElement

    @POST("auth/login")
    suspend fun login(@Body request: Map<String, @JvmSuppressWildcards Any?>): JsonElement

    @POST("auth/logout")
    suspend fun logout(): JsonElement
}

data class VerificationCodeRequest(
    val phone: String,
)
