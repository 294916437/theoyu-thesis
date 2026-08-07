# React Native Android Target

This directory is the Android target generated and maintained for the React Native app in `hybrid/`.

It is required for:

- Gradle build configuration and wrapper
- Android manifest, app resources, launcher icons, and permissions
- React Native host bootstrap (`MainActivity`, `BlueSkyApplication`)
- Minimal Native Modules for platform capabilities such as session persistence, keep-screen-on, and picture-in-picture

It is not an independent native Android client. Do not add Compose screens, Android ViewModels, Retrofit repositories, native Socket.IO business logic, or `libmediasoupclient`/JNI media pipelines here. Implement product UI and meeting business logic in React Native under `src/`.
