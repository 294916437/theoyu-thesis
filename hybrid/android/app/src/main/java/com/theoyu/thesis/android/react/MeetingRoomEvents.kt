package com.theoyu.thesis.android.react

import com.facebook.react.ReactHost
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule

object MeetingRoomEvents {
    fun emitState(reactHost: ReactHost, roomStateJson: String) {
        val reactContext = reactHost.currentReactContext ?: return
        val payload = Arguments.createMap().apply {
            putString("roomStateJson", roomStateJson)
        }
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("BlueSkyMeetingRoomState", payload)
    }
}
