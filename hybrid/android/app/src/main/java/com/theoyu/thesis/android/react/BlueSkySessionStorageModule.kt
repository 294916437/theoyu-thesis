package com.theoyu.thesis.android.react

import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class BlueSkySessionStorageModule(
    reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {
    private val prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getName(): String = NAME

    @ReactMethod
    fun getSession(promise: Promise) {
        val map = Arguments.createMap().apply {
            putString("token", prefs.getString(KEY_TOKEN, null))
            putString("userId", prefs.getString(KEY_USER_ID, null))
            putString("nickname", prefs.getString(KEY_NICKNAME, null))
            putString("phone", prefs.getString(KEY_PHONE, null))
            putString("avatar", prefs.getString(KEY_AVATAR, null))
        }
        promise.resolve(map)
    }

    @ReactMethod
    fun saveSession(token: String, userId: String, promise: Promise) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .apply()
        promise.resolve(null)
    }

    @ReactMethod
    fun saveUserProfile(
        userId: String,
        nickname: String?,
        phone: String?,
        avatar: String?,
        promise: Promise,
    ) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_PHONE, phone)
            .putString(KEY_AVATAR, avatar)
            .apply()
        promise.resolve(null)
    }

    @ReactMethod
    fun clearSession(promise: Promise) {
        prefs.edit().clear().apply()
        promise.resolve(null)
    }

    companion object {
        const val NAME = "BlueSkySessionStorage"
        private const val PREFS_NAME = "blue_sky_session"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_PHONE = "phone"
        private const val KEY_AVATAR = "avatar"
    }
}
