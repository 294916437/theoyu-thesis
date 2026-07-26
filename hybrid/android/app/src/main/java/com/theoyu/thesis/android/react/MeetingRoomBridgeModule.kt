package com.theoyu.thesis.android.react

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap

class MeetingRoomBridgeModule(
    reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String = NAME

    @ReactMethod
    fun perform(action: String, payload: ReadableMap?) {
        MeetingRoomActionDispatcher.dispatch(action, payload)
    }

    @ReactMethod
    fun setKeepScreenOn(enabled: Boolean) {
        currentActivity?.runOnUiThread {
            val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            if (enabled) {
                currentActivity?.window?.addFlags(flag)
            } else {
                currentActivity?.window?.clearFlags(flag)
            }
        }
    }

    @ReactMethod
    fun enterPictureInPicture() {
        val activity = currentActivity ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        activity.runOnUiThread {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }

    companion object {
        const val NAME = "MeetingRoomBridge"
    }
}
