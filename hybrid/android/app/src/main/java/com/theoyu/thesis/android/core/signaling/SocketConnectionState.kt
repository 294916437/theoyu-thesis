package com.theoyu.thesis.android.core.signaling

data class SocketConnectionState(
    val connected: Boolean = false,
    val reconnecting: Boolean = false,
    val socketId: String? = null,
    val lastError: String? = null,
)
