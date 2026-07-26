package com.theoyu.thesis.android.react

import com.facebook.react.bridge.ReadableMap

object MeetingRoomActionDispatcher {
    @Volatile
    private var handler: ((String, ReadableMap?) -> Unit)? = null

    fun setHandler(nextHandler: ((String, ReadableMap?) -> Unit)?) {
        handler = nextHandler
    }

    fun dispatch(action: String, payload: ReadableMap?) {
        handler?.invoke(action, payload)
    }
}
