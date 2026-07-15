package com.theoyu.thesis.android.data.meeting

import com.google.gson.JsonElement
import com.theoyu.thesis.android.core.network.ApiResult
import com.theoyu.thesis.android.core.network.safeApiCall

class RoomRepository(
    private val apiService: RoomApiService,
) {
    suspend fun validateMeetingNo(meetingNo: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.validateMeetingNo(ValidateMeetingNoRequest(meetingNo.trim()))
        }

    suspend fun fetchMeetingInfo(roomId: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.fetchMeetingInfo(roomId)
        }

    suspend fun fetchMeetingDetail(roomIdOrNo: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.fetchMeetingDetail(roomIdOrNo)
        }

    suspend fun closeMeeting(roomId: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.closeMeeting(RoomIdRequest(roomId))
        }

    suspend fun createMeeting(meetingData: Map<String, Any?>): ApiResult<JsonElement> =
        safeApiCall {
            apiService.createMeeting(meetingData)
        }

    suspend fun joinMeeting(joinData: Map<String, Any?>): ApiResult<JsonElement> =
        safeApiCall {
            apiService.joinMeeting(joinData)
        }

    suspend fun fetchUpcomingMeetings(page: Int = 1, size: Int = 5): ApiResult<JsonElement> =
        safeApiCall {
            apiService.fetchUpcomingMeetings(page, size)
        }

    suspend fun fetchRecentMeetings(page: Int = 1, size: Int = 10): ApiResult<JsonElement> =
        safeApiCall {
            apiService.fetchRecentMeetings(page, size)
        }

    suspend fun updateMeeting(
        roomId: String,
        meetingData: Map<String, Any?>,
    ): ApiResult<JsonElement> =
        safeApiCall {
            apiService.updateMeeting(roomId, meetingData)
        }

    suspend fun deleteMeeting(roomId: String): ApiResult<JsonElement> =
        safeApiCall {
            apiService.deleteMeeting(roomId)
        }

    suspend fun fetchParticipantsList(
        roomId: String,
        status: String,
        page: Int,
        size: Int,
    ): ApiResult<JsonElement> =
        safeApiCall {
            apiService.fetchParticipantsList(roomId, status, page, size)
        }
}
