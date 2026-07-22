package com.theoyu.thesis.android.feature.meeting.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.feature.main.CreateMeetingForm
import com.theoyu.thesis.android.feature.main.MeetingCreateType
import com.theoyu.thesis.android.feature.main.component.SwitchRow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingScreen(
    form: CreateMeetingForm,
    isSubmitting: Boolean,
    onTitleChanged: (String) -> Unit,
    onTypeChanged: (MeetingCreateType) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onTimeChanged: (LocalTime) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAudioChanged: (Boolean) -> Unit,
    onVideoChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = form.title,
                    onValueChange = onTitleChanged,
                    label = { Text("会议标题") },
                    placeholder = { Text("请输入会议标题") },
                    singleLine = true,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MeetingCreateType.entries.forEach { type ->
                        FilterChip(
                            selected = form.type == type,
                            onClick = { onTypeChanged(type) },
                            label = { Text(type.label) },
                        )
                    }
                }
            }
            item {
                DateTimePickerFields(
                    enabled = form.type == MeetingCreateType.Scheduled,
                    date = form.startDate,
                    time = form.startTime,
                    onDateChanged = onDateChanged,
                    onTimeChanged = onTimeChanged,
                )
            }
            item {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    value = form.description,
                    onValueChange = onDescriptionChanged,
                    label = { Text("会议说明") },
                    placeholder = { Text("填写会议议题或参会说明") },
                    maxLines = 4,
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("默认入会状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SwitchRow("麦克风默认开启", form.audioEnabled, onAudioChanged)
                        SwitchRow("摄像头默认开启", form.videoEnabled, onVideoChanged)
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
                    .padding(20.dp),
                enabled = !isSubmitting,
                onClick = onSubmit,
            ) {
                Text(if (isSubmitting) "创建中..." else "创建会议")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerFields(
    enabled: Boolean,
    date: LocalDate,
    time: LocalTime,
    onDateChanged: (LocalDate) -> Unit,
    onTimeChanged: (LocalTime) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (enabled) "开始时间" else "立即会议将使用当前时间",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { showDatePicker = true },
            ) {
                Text(date.format(DATE_FORMATTER), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { showTimePicker = true },
            ) {
                Text(time.format(TIME_FORMATTER), maxLines = 1)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChanged(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
