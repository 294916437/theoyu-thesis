package com.theoyu.thesis.android.feature.meeting.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
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
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = meetingNo,
                onValueChange = onMeetingNoChanged,
                label = { Text("会议号") },
                placeholder = { Text("请输入会议号") },
                isError = error != null,
                supportingText = error?.let { { Text(it.message) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && meetingNo.isNotBlank(),
                onClick = if (validatedMeeting == null) onValidate else onJoin,
            ) {
                Text(
                    when {
                        isSubmitting -> "处理中..."
                        validatedMeeting == null -> "验证会议"
                        else -> "加入会议"
                    },
                )
            }
        }
        validatedMeeting?.let { meeting ->
            item {
                Text("会议摘要", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item { MeetingSummaryCard(meeting = meeting) }
        }
    }
}
