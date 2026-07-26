package com.theoyu.thesis.android.core.sfu

import android.content.Context

object WebRtcEnvironment {
    @Volatile
    private var initialized = false

    fun initialize(appContext: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext.applicationContext
            initialized = true
        }
    }
}
