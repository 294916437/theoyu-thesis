package com.theoyu.thesis.android.core.network

fun interface UnauthorizedHandler {
    fun onUnauthorized()

    object Noop : UnauthorizedHandler {
        override fun onUnauthorized() = Unit
    }
}
