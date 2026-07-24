// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
apply plugin: "com.android.application"
apply plugin: "com.facebook.react"
dependencies {
    implementation("com.facebook.react:react-android") // 引入 React Native 核心 SDK
    implementation("com.facebook.react:hermes-android") // 推荐开启 Hermes 引擎以提升性能
}

