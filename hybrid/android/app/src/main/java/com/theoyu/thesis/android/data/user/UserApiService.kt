package com.theoyu.thesis.android.data.user

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface UserApiService {
    @POST("user/user/profile")
    suspend fun getUserProfile(@Body request: UserIdRequest): JsonElement

    @Multipart
    @POST("user/user/update")
    suspend fun updateUserProfile(
        @PartMap formFields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>,
    ): JsonElement

    @POST("user/user/online/check")
    suspend fun getUserOnlineStatus(@Body request: UserIdRequest): JsonElement

    @POST("user/user/online/set")
    suspend fun setUserOnlineStatus(@Body request: UserIdRequest): JsonElement

    @POST("user/user/offline/set")
    suspend fun setUserOfflineStatus(@Body request: UserIdRequest): JsonElement
}

data class UserIdRequest(
    val userId: String,
)
