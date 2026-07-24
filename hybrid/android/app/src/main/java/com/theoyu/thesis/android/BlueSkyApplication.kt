package com.theoyu.thesis.android

import android.app.Application
import com.theoyu.thesis.android.core.di.AppContainer
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost

class BlueSkyApplication : Application(), ReactApplication {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        loadReactNative(this)
        appContainer = AppContainer(this)
    }
    override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          // Packages that cannot be autolinked yet can be added manually here, for example:
          // add(MyReactNativePackage())
        },
    )
  }
}
