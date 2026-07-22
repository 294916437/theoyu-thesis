package com.theoyu.thesis.android.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.feature.main.MainUiState
import com.theoyu.thesis.android.feature.main.UserSummary

@Composable
fun ProfileScreen(
    uiState: MainUiState,
    onEditProfile: () -> Unit,
    onDismissEditor: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHeader(userSummary = uiState.userSummary)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsRow("编辑资料", "修改昵称和头像信息", onEditProfile)
                        HorizontalDivider()
                        SettingsRow("在线状态", if (uiState.userSummary.online) "在线" else "离线", null)
                        HorizontalDivider()
                        SettingsRow("手机号", uiState.userSummary.phone.ifBlank { "-" }, null)
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
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                enabled = !uiState.isLoggingOut,
                onClick = onLogout,
            ) {
                Text(if (uiState.isLoggingOut) "退出中..." else "退出登录")
            }
        }
    }

    if (uiState.profileEditOpen) {
        AlertDialog(
            onDismissRequest = onDismissEditor,
            title = { Text("编辑资料") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.profileEditForm.nickname,
                        onValueChange = onNicknameChanged,
                        label = { Text("昵称") },
                        singleLine = true,
                    )
                    Text(
                        "手机号由登录账号绑定，当前仅支持修改昵称。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSubmitting,
                    onClick = onSaveProfile,
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEditor) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ProfileHeader(userSummary: UserSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userSummary.displayName.firstOrNull()?.toString() ?: "我",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(userSummary.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = userSummary.phone.ifBlank { "用户 ID ${userSummary.userId.ifBlank { "-" }}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (userSummary.online) "在线" else "离线",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (userSummary.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Text("进入", color = MaterialTheme.colorScheme.primary)
        }
    }
}
