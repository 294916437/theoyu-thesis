package com.theoyu.thesis.android.core.network

import java.io.IOException
import retrofit2.HttpException

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: NetworkError) : ApiResult<Nothing>
}

data class NetworkError(
    val statusCode: Int? = null,
    val message: String,
    val cause: Throwable? = null,
)

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (error: HttpException) {
        ApiResult.Failure(
            NetworkError(
                statusCode = error.code(),
                message = error.response()?.errorBody()?.string()?.ifBlank { null }
                    ?: error.message(),
                cause = error,
            ),
        )
    } catch (error: IOException) {
        ApiResult.Failure(
            NetworkError(
                message = "Network request failed,${error}",
                cause = error,
            ),
        )
    } catch (error: Throwable) {
        ApiResult.Failure(
            NetworkError(
                message = error.message ?: "Unexpected request failure",
                cause = error,
            ),
        )
    }
