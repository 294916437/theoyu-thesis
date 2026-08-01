package com.theoyu.thesis.android.feature.profile

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.feature.main.MainUiState
import com.theoyu.thesis.android.feature.main.UserSummary

@Composable
fun ProfileScreen(
    uiState: MainUiState,
    onEditProfile: () -> Unit,
    onDismissEditor: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAvatarChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHeader(userSummary = uiState.userSummary)
            }
            item {
                if (uiState.profileEditOpen) {
                    ProfileEditCard(
                        uiState = uiState,
                        onNicknameChanged = onNicknameChanged,
                        onPhoneChanged = onPhoneChanged,
                        onAvatarChanged = onAvatarChanged,
                        onSaveProfile = onSaveProfile,
                        onDismissEditor = onDismissEditor,
                    )
                } else {
                    ProfileDetailCard(
                        userSummary = uiState.userSummary,
                        onEditProfile = onEditProfile,
                    )
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
                enabled = !uiState.isLoggingOut && !uiState.isSubmitting,
                onClick = onLogout,
            ) {
                Text(if (uiState.isLoggingOut) "退出中..." else "退出登录")
            }
        }
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
                Text(
                    text = userSummary.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = userSummary.phone.ifBlank { "用户 ID ${userSummary.userId.ifBlank { "-" }}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
private fun ProfileDetailCard(
    userSummary: UserSummary,
    onEditProfile: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("个人资料", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Button(onClick = onEditProfile) {
                    Text("编辑")
                }
            }
            HorizontalDivider()
            ProfileInfoRow("用户 ID", userSummary.userId.ifBlank { "-" })
            ProfileInfoRow("昵称", userSummary.displayName.ifBlank { "-" })
            ProfileInfoRow("手机号", userSummary.phone.ifBlank { "-" })
            ProfileInfoRow("头像地址", userSummary.avatar.ifBlank { "未设置" })
            ProfileInfoRow("在线状态", if (userSummary.online) "在线" else "离线")
        }
    }
}

@Composable
private fun ProfileEditCard(
    uiState: MainUiState,
    onNicknameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAvatarChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onDismissEditor: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("编辑个人资料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.profileEditForm.nickname,
                onValueChange = onNicknameChanged,
                label = { Text("昵称") },
                singleLine = true,
                supportingText = { Text("${uiState.profileEditForm.nickname.length}/50") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.profileEditForm.phone,
                onValueChange = onPhoneChanged,
                label = { Text("手机号") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.profileEditForm.avatar,
                onValueChange = onAvatarChanged,
                label = { Text("头像 URL") },
                placeholder = { Text("https://...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSubmitting,
                    onClick = onDismissEditor,
                ) {
                    Text("取消")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSubmitting,
                    onClick = onSaveProfile,
                ) {
                    Text(if (uiState.isSubmitting) "保存中..." else "保存")
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "头像当前以 URL 形式保存；后续可接入系统相册选择并通过 multipart 文件上传。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(84.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
