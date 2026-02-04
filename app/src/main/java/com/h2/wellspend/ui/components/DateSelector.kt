package com.h2.wellspend.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.h2.wellspend.data.TimeRange
import com.h2.wellspend.utils.DateUtils
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    timeRange: TimeRange,
    onTimeRangeChange: (TimeRange) -> Unit,
    startOfWeek: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY,
    customDateRange: Pair<LocalDate, LocalDate>? = null,
    onCustomDateRangeChange: (Pair<LocalDate, LocalDate>) -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showRangeMenu by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time Range Selector
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showRangeMenu = true }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                 Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeRange.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            DropdownMenu(
                expanded = showRangeMenu,
                onDismissRequest = { showRangeMenu = false }
            ) {
                TimeRange.values().forEach { range ->
                    DropdownMenuItem(
                        text = { Text(range.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            if (range == TimeRange.CUSTOM && customDateRange == null) {
                                // Default to current month if selecting custom for the first time
                                val start = currentDate.withDayOfMonth(1)
                                val end = currentDate.withDayOfMonth(currentDate.lengthOfMonth())
                                onCustomDateRangeChange(start to end)
                            }
                            onTimeRangeChange(range)
                            showRangeMenu = false
                        }
                    )
                }
            }
        }

        // Date Navigator
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                if (timeRange == TimeRange.CUSTOM) {
                     if (customDateRange != null) {
                        val days = java.time.temporal.ChronoUnit.DAYS.between(customDateRange.first, customDateRange.second) + 1
                        val newStart = customDateRange.first.minusDays(days)
                        val newEnd = customDateRange.second.minusDays(days)
                        onCustomDateRangeChange(newStart to newEnd)
                        onDateChange(newStart)
                     }
                } else {
                     onDateChange(DateUtils.adjustDate(currentDate, timeRange, false))
                }
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = MaterialTheme.colorScheme.onSurface)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text(
                    text = DateUtils.formatDateForRange(currentDate, timeRange, startOfWeek, customDateRange),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = { 
                if (timeRange == TimeRange.CUSTOM) {
                     if (customDateRange != null) {
                        val days = java.time.temporal.ChronoUnit.DAYS.between(customDateRange.first, customDateRange.second) + 1
                        val newStart = customDateRange.first.plusDays(days)
                        val newEnd = customDateRange.second.plusDays(days)
                        onCustomDateRangeChange(newStart to newEnd)
                        onDateChange(newStart)
                     } 
                } else {
                     onDateChange(DateUtils.adjustDate(currentDate, timeRange, true))
                }
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    
    if (showDatePicker) {
        when (timeRange) {
            TimeRange.DAILY -> {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                                onDateChange(date)
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            TimeRange.WEEKLY -> {
                WeekPickerDialog(
                    currentDate = currentDate,
                    onDateSelected = { 
                        onDateChange(it)
                        showDatePicker = false 
                    },
                    onDismiss = { showDatePicker = false },
                    startOfWeek = startOfWeek
                )
            }
            TimeRange.MONTHLY -> {
                MonthPickerDialog(
                    currentDate = currentDate,
                    onDateSelected = { 
                        onDateChange(it)
                        showDatePicker = false 
                    },
                    onDismiss = { showDatePicker = false }
                )
            }
            TimeRange.YEARLY -> {
                YearPickerDialog(
                    currentDate = currentDate,
                    onDateSelected = { 
                        onDateChange(it)
                        showDatePicker = false 
                    },
                    onDismiss = { showDatePicker = false }
                )
            }
            TimeRange.CUSTOM -> {
                CustomRangePickerDialog(
                    initialDate = currentDate,
                    initialRange = customDateRange,
                    onRangeSelected = { 
                        onCustomDateRangeChange(it)
                        onDateChange(it.first) // Update anchor date too?
                        showDatePicker = false
                    },
                    onDismiss = { showDatePicker = false }
                )
            }
        }
    }
}
