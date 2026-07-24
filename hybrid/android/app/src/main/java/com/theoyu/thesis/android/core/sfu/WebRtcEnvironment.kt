package com.theoyu.thesis.android.core.sfu

import android.content.Context
import org.mediasoup.droid.MediasoupClient
import org.webrtc.EglBase

object WebRtcEnvironment {
    val eglBase: EglBase by lazy { EglBase.create() }

    @Volatile
    private var initialized = false

    fun initialize(appContext: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            MediasoupClient.initialize(appContext.applicationContext)
            initialized = true
        }
    }
}
