package com.theoyu.thesis.android.core.network

import okhttp3.Interceptor
import okhttp3.Response

class UnauthorizedInterceptor(
    private val unauthorizedHandler: UnauthorizedHandler,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            unauthorizedHandler.onUnauthorized()
        }

        return response
    }
}
