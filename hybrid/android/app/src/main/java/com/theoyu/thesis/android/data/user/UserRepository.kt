package com.theoyu.thesis.android.data.user

import com.google.gson.JsonElement
import com.theoyu.thesis.android.core.network.ApiResult
import com.theoyu.thesis.android.core.network.safeApiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class UserRepository(
    private val apiService: UserApiService,
) {
    suspend fun getUserProfile(userId: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.getUserProfile(UserIdRequest(userId))
        }

    suspend fun updateUserProfile(
        formFields: Map<String, String>,
        files: List<MultipartBody.Part> = emptyList(),
    ): ApiResult<JsonElement> =
        safeApiCall {
            apiService.updateUserProfile(formFields.toRequestBodyMap(), files)
        }

    suspend fun getUserOnlineStatus(userId: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.getUserOnlineStatus(UserIdRequest(userId))
        }

    suspend fun setUserOnlineStatus(userId: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.setUserOnlineStatus(UserIdRequest(userId))
        }

    suspend fun setUserOfflineStatus(userId: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.setUserOfflineStatus(UserIdRequest(userId))
        }

    private fun Map<String, String>.toRequestBodyMap(): Map<String, RequestBody> {
        val textPlain = "text/plain".toMediaType()
        return mapValues { (_, value) -> value.toRequestBody(textPlain) }
    }
}
