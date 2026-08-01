package com.theoyu.thesis.android.feature.meeting.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.feature.main.MainUiState
import com.theoyu.thesis.android.feature.main.component.EmptyState
import com.theoyu.thesis.android.feature.main.component.MeetingSummaryCard
import com.theoyu.thesis.android.feature.main.component.UserSummaryCard

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onMeetingNoChanged: (String) -> Unit,
    onJoinClick: () -> Unit,
    onCreateInstant: () -> Unit,
    onCreateScheduled: () -> Unit,
    onRecentClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { UserSummaryCard(userSummary = uiState.userSummary) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("加入会议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.homeMeetingNo,
                        onValueChange = onMeetingNoChanged,
                        label = { Text("会议号") },
                        placeholder = { Text("输入会议号或英文邀请码") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.homeMeetingNo.isNotBlank(),
                        onClick = onJoinClick,
                    ) {
                        Text("加入会议")
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(modifier = Modifier.weight(1f), onClick = onCreateInstant) {
                    Text("立即会议")
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onCreateScheduled) {
                    Text("预约会议")
                }
            }
        }
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRecentClick),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("最近会议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${uiState.recentMeetings.size} 条会议记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("查看", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Text("即将开始", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (uiState.upcomingMeetings.isEmpty()) {
            item { EmptyState(text = "暂无即将开始的会议") }
        } else {
            items(uiState.upcomingMeetings.take(3), key = { it.roomId + it.roomNo + it.title }) { meeting ->
                MeetingSummaryCard(meeting = meeting)
            }
        }
    }
}
