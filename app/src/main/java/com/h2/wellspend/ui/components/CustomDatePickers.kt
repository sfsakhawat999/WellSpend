package com.h2.wellspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.h2.wellspend.data.TimeRange
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedDatePickerDialog(
    initialDate: LocalDate,
    initialRangeType: TimeRange,
    initialCustomRange: Pair<LocalDate, LocalDate>?,
    onDateSelected: (LocalDate, TimeRange, Pair<LocalDate, LocalDate>?) -> Unit,
    onDismiss: () -> Unit,
    startOfWeek: DayOfWeek = DayOfWeek.MONDAY
) {
    var selectedRangeType by remember { mutableStateOf(initialRangeType) }
    
    // Independent states for each type to preserve selection when switching tabs
    var dailyDate by remember { mutableStateOf(initialDate) }
    var weeklyDate by remember { mutableStateOf(initialDate) }
    var monthlyDate by remember { mutableStateOf(initialDate) }
    var yearlyDate by remember { mutableStateOf(initialDate) }
    var customRange by remember { 
        mutableStateOf(initialCustomRange ?: (initialDate.withDayOfMonth(1) to initialDate.withDayOfMonth(initialDate.lengthOfMonth()))) 
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Range Type Selector
            // Range Type Selector
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Range Type",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Box(
                    modifier = Modifier.width(130.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { expanded = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedRangeType.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        // Use standard icon
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(130.dp) // Match width
                    ) {
                        TimeRange.values().forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedRangeType = range
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp), // Increased height for calendar fit
                contentAlignment = Alignment.TopCenter
            ) {
                when (selectedRangeType) {
                    TimeRange.DAILY -> {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = dailyDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                        )
                        LaunchedEffect(datePickerState.selectedDateMillis) {
                            datePickerState.selectedDateMillis?.let {
                                dailyDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            }
                        }
                        DatePicker(
                            state = datePickerState,
                            title = null,
                            headline = null,
                            showModeToggle = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    TimeRange.WEEKLY -> {
                        WeekPickerContent(
                            currentDate = weeklyDate,
                            onDateChanged = { weeklyDate = it },
                            startOfWeek = startOfWeek
                        )
                    }
                    TimeRange.MONTHLY -> {
                        MonthPickerContent(
                            currentDate = monthlyDate,
                            onDateChanged = { monthlyDate = it }
                        )
                    }
                    TimeRange.YEARLY -> {
                        YearPickerContent(
                            currentDate = yearlyDate,
                            onDateChanged = { yearlyDate = it }
                        )
                    }
                    TimeRange.CUSTOM -> {
                        CustomRangePickerContent(
                            initialStart = customRange.first,
                            initialEnd = customRange.second,
                            onRangeChanged = { start, end -> customRange = start to end }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val resultDate = when (selectedRangeType) {
                            TimeRange.DAILY -> dailyDate
                            TimeRange.WEEKLY -> weeklyDate
                            TimeRange.MONTHLY -> monthlyDate
                            TimeRange.YEARLY -> yearlyDate
                            TimeRange.CUSTOM -> customRange.first
                        }
                        onDateSelected(resultDate, selectedRangeType, customRange)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            }
        }
    }
}




@Composable
fun MonthPickerContent(
    currentDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit
) {
    var selectedMonth by remember(currentDate) { mutableStateOf(currentDate.month) }
    var selectedYear by remember(currentDate) { mutableIntStateOf(currentDate.year) }
    val months = Month.entries
    val years = (2000..2100).toList()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WheelPicker(
                    items = months,
                    initialItem = selectedMonth,
                    itemLabel = { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) },
                    onItemSelected = { 
                        selectedMonth = it
                        onDateChanged(LocalDate.of(selectedYear, selectedMonth, 1))
                    }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                WheelPicker(
                    items = years,
                    initialItem = selectedYear,
                    itemLabel = { it.toString() },
                    onItemSelected = { 
                        selectedYear = it
                        onDateChanged(LocalDate.of(selectedYear, selectedMonth, 1))
                    }
                )
            }
        }
    }
}

@Composable
fun YearPickerContent(
    currentDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit
) {
    var selectedYear by remember(currentDate) { mutableIntStateOf(currentDate.year) }
    val years = (2000..2100).toList()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WheelPicker(
                    items = years,
                    initialItem = selectedYear,
                    itemLabel = { it.toString() },
                    onItemSelected = { 
                        selectedYear = it
                        onDateChanged(LocalDate.of(selectedYear, 1, 1))
                    }
                )
            }
        }
    }
}

@Composable
fun WeekPickerContent(
    currentDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    startOfWeek: DayOfWeek
) {
    var selectedYear by remember(currentDate) { mutableIntStateOf(currentDate.year) }
    val weekFields = WeekFields.of(startOfWeek, 1)
    val currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())
    var selectedWeek by remember(currentDate) { mutableIntStateOf(currentWeek) }
    val years = (2000..2100).toList()
    val weeks = (1..53).toList()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(0.4f)) {
                WheelPicker(
                    items = years,
                    initialItem = selectedYear,
                    itemLabel = { it.toString() },
                    onItemSelected = { 
                        selectedYear = it
                        // Update date
                         val newDate = LocalDate.now()
                            .withYear(selectedYear)
                            .with(weekFields.weekOfWeekBasedYear(), selectedWeek.toLong())
                            .with(weekFields.dayOfWeek(), 1)
                        onDateChanged(newDate)
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(0.6f)) {
                WheelPicker(
                    items = weeks,
                    initialItem = selectedWeek,
                    itemLabel = { weekNum ->
                        try {
                            val weekDate = LocalDate.now()
                                .withYear(selectedYear)
                                .with(weekFields.weekOfWeekBasedYear(), weekNum.toLong())
                                .with(weekFields.dayOfWeek(), 1)
                            "W$weekNum (${weekDate.format(DateTimeFormatter.ofPattern("MMM dd"))})"
                        } catch (e: Exception) {
                            "Week $weekNum"
                        }
                    },
                    onItemSelected = { 
                        selectedWeek = it
                         val newDate = LocalDate.now()
                            .withYear(selectedYear)
                            .with(weekFields.weekOfWeekBasedYear(), selectedWeek.toLong())
                            .with(weekFields.dayOfWeek(), 1)
                        onDateChanged(newDate)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangePickerContent(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onRangeChanged: (LocalDate, LocalDate) -> Unit
) {
    val startMillis = initialStart.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val endMillis = initialEnd.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startMillis)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endMillis)

    var isSelectingStart by remember { mutableStateOf(true) }

    LaunchedEffect(startDatePickerState) {
        snapshotFlow { startDatePickerState.selectedDateMillis }
            .drop(1)
            .collect { millis ->
                millis?.let {
                    // If manual change and we were selecting start
                    if (isSelectingStart) {
                        isSelectingStart = false
                    }
                    val newStart = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    val currentEnd = Instant.ofEpochMilli(endDatePickerState.selectedDateMillis ?: endMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    onRangeChanged(newStart, currentEnd)
                }
            }
    }
    
    LaunchedEffect(endDatePickerState) {
        snapshotFlow { endDatePickerState.selectedDateMillis }
            .drop(1)
            .collect { millis ->
                millis?.let {
                    val currentStart = Instant.ofEpochMilli(startDatePickerState.selectedDateMillis ?: startMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    val newEnd = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    onRangeChanged(currentStart, newEnd)
                }
            }
    }

    // Tab Animation
    val tabBias by animateFloatAsState(
        targetValue = if (isSelectingStart) -1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bias"
    )

    fun formatMillis(millis: Long?): String {
        return millis?.let {
             Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } ?: "Select"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Range Tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .align(BiasAlignment(tabBias, 0f))
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // Start
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                             interactionSource = remember { MutableInteractionSource() },
                             indication = null
                        ) { isSelectingStart = true },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val textColor by animateColorAsState(if (isSelectingStart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Start Date", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.8f))
                    Text(formatMillis(startDatePickerState.selectedDateMillis), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
                }
                
                // End
                Column(
                    modifier = Modifier
                        .weight(1f)
                         .fillMaxHeight()
                        .clickable(
                             interactionSource = remember { MutableInteractionSource() },
                             indication = null
                        ) { isSelectingStart = false },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val textColor by animateColorAsState(if (!isSelectingStart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("End Date", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.8f))
                    Text(formatMillis(endDatePickerState.selectedDateMillis), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }
        
        // Picker
        if (isSelectingStart) {
            DatePicker(
                state = startDatePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            DatePicker(
                state = endDatePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Keep the interfaces/wrappers for compatibility if needed, OR remove them if we are replacing usage everywhere.
// Since the prompts implied replacing the logic in DateSelector, let's keep only necessary parts. 
// I will keep the original Dialog wrappers BUT make them use the new Content composables to reduce duplication if I needed to keep them, 
// but since we are doing a redesign, I'll rely on UnifiedDatePickerDialog. 
// However, to avoid compile errors if other files use MonthPickerDialog etc (though likely only DateSelector uses them), 
// I will keep the old functions but implement them via the new contents or just leave them as wrappers around simple Dialogs ?
// Let's safe-guard by keeping them but using the new content logic.

@Composable
fun MonthPickerDialog(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    disabledDate: LocalDate? = null
) {
    var selectedDate by remember { mutableStateOf(currentDate) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Month",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            MonthPickerContent(
                currentDate = currentDate,
                onDateChanged = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val isDisabled = disabledDate != null && 
                    selectedDate.month == disabledDate.month && 
                    selectedDate.year == disabledDate.year
                
                Button(
                    onClick = {
                        onDateSelected(selectedDate)
                        onDismiss()
                    },
                    enabled = !isDisabled
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun YearPickerDialog(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    disabledDate: LocalDate? = null
) {
    var selectedDate by remember { mutableStateOf(currentDate) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Year",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            YearPickerContent(
                currentDate = currentDate,
                onDateChanged = { selectedDate = it } // YearPickerContent sets month/day to 1/1
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val isDisabled = disabledDate != null && selectedDate.year == disabledDate.year
                
                Button(
                    onClick = {
                        onDateSelected(selectedDate)
                        onDismiss()
                    },
                    enabled = !isDisabled
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun WeekPickerDialog(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    startOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    disabledDate: LocalDate? = null
) {
    var selectedDate by remember { mutableStateOf(currentDate) }
    val weekFields = WeekFields.of(startOfWeek, 1)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Week",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            WeekPickerContent(
                currentDate = currentDate,
                onDateChanged = { selectedDate = it },
                startOfWeek = startOfWeek
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                // Compare logic for week
                val isDisabled = disabledDate != null && 
                    selectedDate.get(weekFields.weekOfWeekBasedYear()) == disabledDate.get(weekFields.weekOfWeekBasedYear()) &&
                    selectedDate.year == disabledDate.year
                
                Button(
                    onClick = {
                        onDateSelected(selectedDate)
                        onDismiss()
                    },
                    enabled = !isDisabled
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangePickerDialog(
    initialDate: LocalDate,
    initialRange: Pair<LocalDate, LocalDate>?,
    onRangeSelected: (Pair<LocalDate, LocalDate>) -> Unit,
    onDismiss: () -> Unit
) {
    val initialStart = initialRange?.first ?: initialDate
    val initialEnd = initialRange?.second ?: initialDate
    
    var currentStart by remember { mutableStateOf(initialStart) }
    var currentEnd by remember { mutableStateOf(initialEnd) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // No strict title needed as the content has the pill header
            
            // Content
            Box(modifier = Modifier.height(350.dp)) {
                CustomRangePickerContent(
                    initialStart = initialStart,
                    initialEnd = initialEnd,
                    onRangeChanged = { s, e ->
                        currentStart = s
                        currentEnd = e
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        onRangeSelected(currentStart to currentEnd)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            }
        }
    }
}

// Actually, let's just stick to the plan: UnifiedDatePickerDialog is the key.
// I will keep WheelPicker helper.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialItem: T,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    visibleItemsCount: Int = 3
) {
    val initialIndex = items.indexOf(initialItem).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val itemHeight = 40.dp
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val viewportCenter = layoutInfo.viewportEndOffset / 2
                val closestItem = layoutInfo.visibleItemsInfo.minByOrNull { 
                    kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
                }
                closestItem?.index?.let { items.getOrNull(it) }
            }
            .distinctUntilChanged()
            .collect { item ->
                item?.let { onItemSelected(it) }
            }
    }

    val centerItemIndex by remember {
        derivedStateOf {
             val layoutInfo = listState.layoutInfo
             val viewportCenter = layoutInfo.viewportEndOffset / 2
             layoutInfo.visibleItemsInfo.minByOrNull { 
                 kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
             }?.index ?: -1
        }
    }

    Box(
        modifier = Modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2))
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = index == centerItemIndex
                val opacity by remember(isSelected) { derivedStateOf { if (isSelected) 1f else 0.3f } }

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .alpha(opacity),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
