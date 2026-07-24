package com.theoyu.thesis.android.data.meeting

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RoomApiService {
    @POST("media/room/validate-no")
    suspend fun validateMeetingNo(@Body request: ValidateMeetingNoRequest): JsonElement

    @GET("media/room/info/{roomId}")
    suspend fun fetchMeetingInfo(@Path("roomId") roomId: String): JsonElement

    @GET("media/room/detail/{roomIdOrNo}")
    suspend fun fetchMeetingDetail(@Path("roomIdOrNo") roomIdOrNo: String): JsonElement

    @POST("media/room/close")
    suspend fun closeMeeting(@Body request: RoomIdRequest): JsonElement

    @POST("media/room/create")
    suspend fun createMeeting(@Body request: Map<String, @JvmSuppressWildcards Any?>): JsonElement

    @POST("media/room/join")
    suspend fun joinMeeting(@Body request: Map<String, @JvmSuppressWildcards Any?>): JsonElement

    @GET("media/room/upcoming")
    suspend fun fetchUpcomingMeetings(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): JsonElement

    @GET("media/room/recent")
    suspend fun fetchRecentMeetings(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): JsonElement

    @PUT("media/room/{roomId}")
    suspend fun updateMeeting(
        @Path("roomId") roomId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>,
    ): JsonElement

    @DELETE("media/room/{roomId}")
    suspend fun deleteMeeting(@Path("roomId") roomId: String): JsonElement

    @GET("media/room/participants")
    suspend fun fetchParticipantsList(
        @Query("roomId") roomId: String,
        @Query("status") status: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): JsonElement
}

data class ValidateMeetingNoRequest(
    val meetingNo: String,
)

data class RoomIdRequest(
    val roomId: String,
)
