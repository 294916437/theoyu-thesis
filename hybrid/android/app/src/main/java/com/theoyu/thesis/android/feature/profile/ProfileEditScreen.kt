package com.theoyu.thesis.android.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.theoyu.thesis.android.feature.main.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    uiState: MainUiState,
    onAvatarUriChanged: (String?) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onSexChanged: (Int) -> Unit,
    onBirthdayChanged: (String?) -> Unit,
    onIntroductionChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val form = uiState.profileEditForm
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAvatarUriChanged(uri.toString())
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val displayAvatar = form.avatarUri ?: uiState.userProfile?.avatar
                            if (!displayAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = displayAvatar,
                                    contentDescription = "头像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    text = form.nickname.firstOrNull()?.toString() ?: "我",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text("点击更换头像", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = form.nickname,
                        onValueChange = onNicknameChanged,
                        label = { Text("昵称") },
                        singleLine = true,
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("性别", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SexRadioButton("保密", 0, form.sex, onSexChanged)
                            SexRadioButton("男", 1, form.sex, onSexChanged)
                            SexRadioButton("女", 2, form.sex, onSexChanged)
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = form.birthday ?: "",
                        onValueChange = { onBirthdayChanged(it.takeIf { it.isNotBlank() }) },
                        label = { Text("生日 (YYYY-MM-DD)") },
                        placeholder = { Text("例如 2000-01-01") },
                        singleLine = true,
                    )
                }

                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = form.introduction,
                        onValueChange = onIntroductionChanged,
                        label = { Text("个人简介") },
                        minLines = 3,
                        maxLines = 5,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSave,
                        enabled = !uiState.isSubmitting && form.nickname.isNotBlank()
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("保存修改", modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SexRadioButton(
    label: String,
    value: Int,
    selectedValue: Int,
    onSelect: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSelect(value) }) {
        RadioButton(
            selected = value == selectedValue,
            onClick = { onSelect(value) }
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
