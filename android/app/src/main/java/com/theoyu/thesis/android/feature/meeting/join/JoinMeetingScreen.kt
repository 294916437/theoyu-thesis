package com.theoyu.thesis.android.feature.meeting.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.feature.main.JoinMeetingError
import com.theoyu.thesis.android.feature.main.MeetingSummary
import com.theoyu.thesis.android.feature.main.component.MeetingSummaryCard

@Composable
fun JoinMeetingScreen(
    meetingNo: String,
    error: JoinMeetingError?,
    validatedMeeting: MeetingSummary?,
    isSubmitting: Boolean,
    onMeetingNoChanged: (String) -> Unit,
    onValidate: () -> Unit,
    onJoin: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = meetingNo,
                onValueChange = onMeetingNoChanged,
                label = { Text("会议号") },
                placeholder = { Text("请输入会议号或邀请码") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.MeetingRoom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = {
                    if (meetingNo.isNotBlank()) {
                        IconButton(onClick = { onMeetingNoChanged("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "清除",
                            )
                        }
                    }
                },
                isError = error != null,
                supportingText = error?.let { { Text(it.message) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = if (validatedMeeting == null) ImeAction.Search else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { if (meetingNo.isNotBlank()) onValidate() },
                    onDone = { if (validatedMeeting != null) onJoin() else if (meetingNo.isNotBlank()) onValidate() },
                ),
                shape = RoundedCornerShape(12.dp),
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && meetingNo.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                onClick = if (validatedMeeting == null) onValidate else onJoin,
            ) {
                Text(
                    text = when {
                        isSubmitting -> "处理中..."
                        validatedMeeting == null -> "验证会议信息"
                        else -> "确认加入会议"
                    },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        validatedMeeting?.let { meeting ->
            item {
                Text(
                    text = "会议详情",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                MeetingSummaryCard(meeting = meeting)
            }
        }
    }
}
