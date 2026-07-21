package com.theoyu.thesis.android.feature.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun PreJoinScreen(
    uiState: MainUiState,
    onAudioChanged: (Boolean) -> Unit,
    onVideoChanged: (Boolean) -> Unit,
    onPermissionsChanged: (Boolean, Boolean) -> Unit,
    onAudioRoutesChanged: (List<AudioRoute>) -> Unit,
    onAudioRouteSelected: (AudioRoute) -> Unit,
    onEnterMeeting: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        onPermissionsChanged(
            result[Manifest.permission.CAMERA] == true,
            result[Manifest.permission.RECORD_AUDIO] == true,
        )
    }

    LaunchedEffect(Unit) {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        onPermissionsChanged(cameraGranted, audioGranted)
        if (!cameraGranted || !audioGranted) {
            permissionLauncher.launch(permissions)
        }
        onAudioRoutesChanged(context.availableCommunicationRoutes())
    }

    LaunchedEffect(uiState.audioRoute) {
        context.selectCommunicationRoute(uiState.audioRoute)
    }

    val canJoin = uiState.cameraPermissionGranted &&
        uiState.audioPermissionGranted &&
        (uiState.createForm.audioEnabled || uiState.createForm.videoEnabled)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreJoinVideoPreview(
                    enabled = uiState.createForm.videoEnabled,
                    cameraPermissionGranted = uiState.cameraPermissionGranted,
                    lifecycleOwner = lifecycleOwner,
                )
            }
            uiState.permissionHint?.let { hint ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("入会身份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(uiState.userSummary.displayName.firstOrNull()?.toString() ?: "我")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uiState.userSummary.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    uiState.userSummary.phone.ifBlank { "用户 ID ${uiState.userSummary.userId.ifBlank { "-" }}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                        InfoRow("会议", uiState.preJoinMeeting?.title ?: "-")
                        InfoRow("会议号", uiState.preJoinMeeting?.roomNo?.ifBlank { "-" } ?: "-")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("设备检查", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SwitchRow("麦克风", uiState.createForm.audioEnabled, onAudioChanged)
                        SwitchRow("摄像头", uiState.createForm.videoEnabled, onVideoChanged)
                        Text("音频输出", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.availableAudioRoutes.forEach { route ->
                                FilterChip(
                                    selected = uiState.audioRoute == route,
                                    onClick = { onAudioRouteSelected(route) },
                                    label = { Text(route.label) },
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            tonalElevation = 3.dp,
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                enabled = canJoin,
                onClick = onEnterMeeting,
            ) {
                Text(
                    when {
                        uiState.permissionHint != null -> "完成设备权限检查"
                        !uiState.createForm.audioEnabled && !uiState.createForm.videoEnabled -> "至少开启一个设备"
                        else -> "加入会议"
                    },
                )
            }
        }
    }
}

@Composable
private fun PreJoinVideoPreview(
    enabled: Boolean,
    cameraPermissionGranted: Boolean,
    lifecycleOwner: LifecycleOwner,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (enabled && cameraPermissionGranted) {
                CameraPreview(lifecycleOwner = lifecycleOwner)
            } else {
                Text(
                    text = if (!cameraPermissionGranted) "等待相机权限" else "摄像头已关闭",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(lifecycleOwner: LifecycleOwner) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = CameraPreviewUseCase.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                    )
                },
                ContextCompat.getMainExecutor(context),
            )
        },
    )
}

private fun Context.availableCommunicationRoutes(): List<AudioRoute> {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val routes = mutableListOf(AudioRoute.Speaker)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val hasEarpiece = audioManager.availableCommunicationDevices.any {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        }
        if (hasEarpiece) {
            routes += AudioRoute.Earpiece
        }
    } else {
        routes += AudioRoute.Earpiece
    }
    return routes.distinct()
}

private fun Context.selectCommunicationRoute(route: AudioRoute) {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val type = when (route) {
            AudioRoute.Speaker -> AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            AudioRoute.Earpiece -> AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        }
        audioManager.availableCommunicationDevices
            .firstOrNull { it.type == type }
            ?.let(audioManager::setCommunicationDevice)
    } else {
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = route == AudioRoute.Speaker
    }
}
