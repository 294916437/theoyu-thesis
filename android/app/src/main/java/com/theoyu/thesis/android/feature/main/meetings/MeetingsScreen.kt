package com.theoyu.thesis.android.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MeetingsScreen(
    upcomingMeetings: List<MeetingSummary>,
    recentMeetings: List<MeetingSummary>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("即将开始") }
        if (upcomingMeetings.isEmpty()) {
            item { EmptyState("暂无预约会议") }
        } else {
            items(upcomingMeetings, key = { "upcoming-${it.roomId}-${it.roomNo}" }) { MeetingSummaryCard(it) }
        }
        item { SectionTitle("最近会议") }
        if (recentMeetings.isEmpty()) {
            item { EmptyState("暂无最近会议") }
        } else {
            items(recentMeetings, key = { "recent-${it.roomId}-${it.roomNo}" }) { MeetingSummaryCard(it) }
        }
    }
}
