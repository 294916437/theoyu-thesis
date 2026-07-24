package com.theoyu.thesis.android.core.network

fun interface AuthTokenProvider {
    fun currentToken(): String?

    object Empty : AuthTokenProvider {
        override fun currentToken(): String? = null
    }
}
