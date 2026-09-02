package com.theoyu.thesis.android.feature.meeting.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.feature.main.MeetingSummary
import com.theoyu.thesis.android.feature.main.component.EmptyState
import com.theoyu.thesis.android.feature.main.component.MeetingSummaryCard
import com.theoyu.thesis.android.feature.main.component.SectionTitle

@Composable
fun MeetingsScreen(
    upcomingMeetings: List<MeetingSummary>,
    recentMeetings: List<MeetingSummary>,
    onMeetingClick: (MeetingSummary) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle(
                text = "即将开始",
                trailingText = if (upcomingMeetings.isNotEmpty()) "${upcomingMeetings.size} 场" else null,
            )
        }
        if (upcomingMeetings.isEmpty()) {
            item {
                EmptyState(
                    text = "暂无预约会议",
                    description = "您发起的或受邀参加的未来会议将展示在此处",
                    icon = Icons.Outlined.EventBusy,
                )
            }
        } else {
            items(upcomingMeetings, key = { "upcoming-${it.roomId}-${it.roomNo}" }) { meeting ->
                MeetingSummaryCard(
                    meeting = meeting,
                    onClick = { onMeetingClick(meeting) },
                )
            }
        }

        item {
            SectionTitle(
                text = "最近会议",
                trailingText = if (recentMeetings.isNotEmpty()) "${recentMeetings.size} 场" else null,
            )
        }
        if (recentMeetings.isEmpty()) {
            item {
                EmptyState(
                    text = "暂无历史会议记录",
                    description = "已结束或退出的历史会议将保存在此处",
                    icon = Icons.Outlined.History,
                )
            }
        } else {
            items(recentMeetings, key = { "recent-${it.roomId}-${it.roomNo}" }) { meeting ->
                MeetingSummaryCard(
                    meeting = meeting,
                    onClick = { onMeetingClick(meeting) },
                )
            }
        }
    }
}
