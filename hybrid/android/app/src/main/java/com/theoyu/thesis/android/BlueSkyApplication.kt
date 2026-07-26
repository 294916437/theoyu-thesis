package com.theoyu.thesis.android

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.theoyu.thesis.android.core.di.AppContainer
import com.theoyu.thesis.android.react.BlueSkyReactPackage

class BlueSkyApplication : Application(), ReactApplication {
    lateinit var appContainer: AppContainer
        private set

    private val reactPackages: List<ReactPackage> by lazy {
        PackageList(this).packages.apply {
            add(BlueSkyReactPackage())
        }
    }

    override fun onCreate() {
        super.onCreate()
        loadReactNative(this)
        appContainer = AppContainer(this)
    }

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> =
                reactPackages

            override fun getJSMainModuleName(): String = "index"
        }

    override val reactHost: ReactHost by lazy {
        getDefaultReactHost(
            context = applicationContext,
            packageList = reactPackages,
            jsMainModulePath = "index",
        )
    }
}
