package com.theoyu.thesis.android.core.network

import com.theoyu.thesis.android.data.auth.AuthApiService
import com.theoyu.thesis.android.data.auth.AuthRepository
import com.theoyu.thesis.android.data.meeting.RoomApiService
import com.theoyu.thesis.android.data.meeting.RoomRepository
import com.theoyu.thesis.android.data.user.UserApiService
import com.theoyu.thesis.android.data.user.UserRepository
import retrofit2.Retrofit

class ApiServiceFactory(
    private val retrofit: Retrofit,
) {
    fun authRepository(): AuthRepository =
        AuthRepository(retrofit.create(AuthApiService::class.java))

    fun userRepository(): UserRepository =
        UserRepository(retrofit.create(UserApiService::class.java))

    fun roomRepository(): RoomRepository =
        RoomRepository(retrofit.create(RoomApiService::class.java))
}
