package com.theoyu.thesis.android.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.currentToken()
        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
