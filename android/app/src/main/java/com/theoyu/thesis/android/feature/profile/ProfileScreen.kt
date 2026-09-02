package com.theoyu.thesis.android.feature.profile

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import com.theoyu.thesis.android.feature.main.component.Avatar
import com.theoyu.thesis.android.feature.main.component.InfoRow

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
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 100.dp),
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                enabled = !uiState.isLoggingOut && !uiState.isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                onClick = onLogout,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isLoggingOut) "正在退出登录..." else "退出当前账号",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(userSummary: UserSummary) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(
                name = userSummary.displayName,
                avatarUrl = userSummary.avatar,
                modifier = Modifier.size(80.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = userSummary.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (userSummary.phone.isNotBlank()) userSummary.phone else "用户 ID: ${userSummary.userId.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (userSummary.online) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = if (userSummary.online) "账号状态 · 在线" else "账号状态 · 离线",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (userSummary.online) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailCard(
    userSummary: UserSummary,
    onEditProfile: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "账户基本信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("编辑资料")
                }
            }
            HorizontalDivider()
            InfoRow("用户 ID", userSummary.userId.ifBlank { "未分配" })
            InfoRow("用户昵称", userSummary.displayName.ifBlank { "未设置" })
            InfoRow("绑定手机", userSummary.phone.ifBlank { "未绑定" })
            InfoRow("头像链接", userSummary.avatar.ifBlank { "默认头像" })
            InfoRow("在线状态", if (userSummary.online) "在线中" else "离线")
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
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "编辑个人资料",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.profileEditForm.nickname,
                onValueChange = onNicknameChanged,
                label = { Text("昵称") },
                leadingIcon = {
                    Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                supportingText = { Text("${uiState.profileEditForm.nickname.length}/50") },
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.profileEditForm.phone,
                onValueChange = onPhoneChanged,
                label = { Text("手机号") },
                leadingIcon = {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.profileEditForm.avatar,
                onValueChange = onAvatarChanged,
                label = { Text("头像 URL") },
                placeholder = { Text("https://...") },
                leadingIcon = {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSubmitting,
                    shape = RoundedCornerShape(10.dp),
                    onClick = onDismissEditor,
                ) {
                    Text("取消")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSubmitting,
                    shape = RoundedCornerShape(10.dp),
                    onClick = onSaveProfile,
                ) {
                    Text(if (uiState.isSubmitting) "保存中..." else "保存更改")
                }
            }
        }
    }
}
