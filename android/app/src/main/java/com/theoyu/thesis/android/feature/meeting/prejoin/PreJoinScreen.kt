package com.theoyu.thesis.android.feature.meeting.prejoin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.theoyu.thesis.android.feature.main.AudioRoute
import com.theoyu.thesis.android.feature.main.MainUiState
import com.theoyu.thesis.android.feature.main.component.Avatar
import com.theoyu.thesis.android.feature.main.component.InfoRow
import com.theoyu.thesis.android.feature.main.component.SwitchRow

@Composable
fun PreJoinScreen(
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
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 100.dp),
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("参会信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(
                                name = uiState.userSummary.displayName,
                                avatarUrl = uiState.userSummary.avatar,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.userSummary.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (uiState.userSummary.phone.isNotBlank()) uiState.userSummary.phone else "用户 ID: ${uiState.userSummary.userId.ifBlank { "未绑定" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                        InfoRow("会议标题", uiState.preJoinMeeting?.title ?: "-")
                        InfoRow("会议号码", uiState.preJoinMeeting?.roomNo?.ifBlank { "-" } ?: "-")
                    }
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("设备检查与预设", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SwitchRow(
                            label = "麦克风",
                            description = "入会后保持麦克风开启",
                            checked = uiState.createForm.audioEnabled,
                            onCheckedChange = onAudioChanged,
                        )
                        SwitchRow(
                            label = "摄像头",
                            description = "入会后保持摄像头开启",
                            checked = uiState.createForm.videoEnabled,
                            onCheckedChange = onVideoChanged,
                        )
                        HorizontalDivider()
                        Text(
                            text = "音频输出设备",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.availableAudioRoutes.forEach { route ->
                                val icon = when (route) {
                                    AudioRoute.Speaker -> Icons.AutoMirrored.Filled.VolumeUp
                                    AudioRoute.Earpiece -> Icons.Filled.PhoneIphone
                                }
                                FilterChip(
                                    selected = uiState.audioRoute == route,
                                    onClick = { onAudioRouteSelected(route) },
                                    label = { Text(route.label) },
                                    leadingIcon = {
                                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    shape = RoundedCornerShape(10.dp),
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                enabled = canJoin,
                shape = RoundedCornerShape(12.dp),
                onClick = onEnterMeeting,
            ) {
                Text(
                    text = when {
                        uiState.permissionHint != null -> "完成设备权限检查"
                        !uiState.createForm.audioEnabled && !uiState.createForm.videoEnabled -> "至少开启一个设备"
                        else -> "确认加入会议"
                    },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (enabled && cameraPermissionGranted) {
                CameraPreview(lifecycleOwner = lifecycleOwner)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "前置摄像头 · 实时预览",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideocamOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp),
                    )
                    Text(
                        text = if (!cameraPermissionGranted) "等待相机与麦克风权限" else "摄像头已关闭",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
