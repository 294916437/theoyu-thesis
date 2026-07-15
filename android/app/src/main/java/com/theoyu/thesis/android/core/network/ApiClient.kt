package com.theoyu.thesis.android.core.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val REQUEST_TIMEOUT_SECONDS = 15L

    fun createRetrofit(
        config: ApiClientConfig,
        tokenProvider: AuthTokenProvider,
        unauthorizedHandler: UnauthorizedHandler = UnauthorizedHandler.Noop,
    ): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (config.enableHttpLogging) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthHeaderInterceptor(tokenProvider))
            .addInterceptor(UnauthorizedInterceptor(unauthorizedHandler))
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(config.normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

data class ApiClientConfig(
    val baseUrl: String,
    val enableHttpLogging: Boolean = false,
) {
    val normalizedBaseUrl: String =
        if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
}
