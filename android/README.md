# Android Client

This directory is reserved for the native Android client of the thesis video conferencing system.

Create the Android project manually with Android Studio so the generated Gradle wrapper, Android Gradle Plugin version, SDK metadata, and IDE files match the local official tooling.

Recommended Android Studio setup:

- Template: Empty Activity
- Language: Kotlin
- UI: Jetpack Compose
- Minimum SDK: API 26 or higher
- Package name: `com.theoyu.thesis.mobile`
- Project location: `android/`

Architecture direction:

- UI layer: Jetpack Compose + Material 3
- State layer: ViewModel + StateFlow
- Backend APIs: reuse the existing Spring Cloud gateway REST APIs
- Meeting signaling: connect to the existing Node.js SFU Socket.io signaling service
- Media layer: isolate native WebRTC and mediasoup integration behind a dedicated gateway/service layer

More details: [Android initialization guide](../docs/android-initialization.md).
