package com.theoyu.thesis.android

import android.app.Application
import com.theoyu.thesis.android.core.di.AppContainer

class BlueSkyApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
