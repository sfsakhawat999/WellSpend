package com.h2.wellspend.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import androidx.compose.foundation.clickable

@Composable
fun MonthPickerDialog(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    disabledDate: LocalDate? = null
) {
    // Current Selection
    var selectedMonth by remember { mutableStateOf(currentDate.month) }
    var selectedYear by remember { mutableIntStateOf(currentDate.year) }

    // Data Sources
    val months = Month.entries
    val years = (2000..2100).toList()

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Picker
                Box(modifier = Modifier.weight(1f)) {
                    WheelPicker(
                        items = months,
                        initialItem = selectedMonth,
                        itemLabel = { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) },
                        onItemSelected = { selectedMonth = it }
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                // Year Picker
                Box(modifier = Modifier.weight(1f)) {
                    WheelPicker(
                        items = years,
                        initialItem = selectedYear,
                        itemLabel = { it.toString() },
                        onItemSelected = { selectedYear = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val selectedDate = LocalDate.of(selectedYear, selectedMonth, 1)
                val isDisabled = disabledDate != null && selectedDate.month == disabledDate.month && selectedDate.year == disabledDate.year
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
    // Current Selection
    var selectedYear by remember { mutableIntStateOf(currentDate.year) }

    // Data Sources
    val years = (2000..2100).toList()

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year Picker
                Box(modifier = Modifier.weight(1f)) {
                    WheelPicker(
                        items = years,
                        initialItem = selectedYear,
                        itemLabel = { it.toString() },
                        onItemSelected = { selectedYear = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val isDisabled = disabledDate != null && selectedYear == disabledDate.year
                Button(
                    onClick = {
                        onDateSelected(LocalDate.of(selectedYear, 1, 1))
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
    var selectedYear by remember { mutableIntStateOf(currentDate.year) }
    
    // Calculate current week index (1-52/53)
    val weekFields = WeekFields.of(startOfWeek, 1)
    val currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())
    var selectedWeek by remember { mutableIntStateOf(currentWeek) }

    val years = (2000..2100).toList()
    // Max weeks logic is simplified here; usually 52 or 53.
    // We update available weeks when year changes ideally, but static 1..53 is fine for UI picker.
    val weeks = (1..53).toList()

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year Picker
                Box(modifier = Modifier.weight(0.4f)) {
                    WheelPicker(
                        items = years,
                        initialItem = selectedYear,
                        itemLabel = { it.toString() },
                        onItemSelected = { selectedYear = it }
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                // Week Picker
                Box(modifier = Modifier.weight(0.6f)) {
                    WheelPicker(
                        items = weeks,
                        initialItem = selectedWeek,
                        itemLabel = { weekNum ->
                            // Basic logic to show date range for the week
                            try {
                                val weekDate = LocalDate.now()
                                    .withYear(selectedYear)
                                    .with(weekFields.weekOfWeekBasedYear(), weekNum.toLong())
                                    .with(weekFields.dayOfWeek(), 1) // Start of week
                                val endWeekDate = weekDate.plusDays(6)
                                "W$weekNum (${weekDate.format(DateTimeFormatter.ofPattern("MMM dd"))})"
                            } catch (e: Exception) {
                                "Week $weekNum"
                            }
                        },
                        onItemSelected = { selectedWeek = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val weekFields = WeekFields.of(startOfWeek, 1)
                val selectedDate = LocalDate.now()
                    .withYear(selectedYear)
                    .with(weekFields.weekOfWeekBasedYear(), selectedWeek.toLong())
                    .with(weekFields.dayOfWeek(), 1)
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
    // Initialize states using UTC to avoid timezone shifts
    val startMillis = (initialRange?.first ?: initialDate)
        .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    
    val endMillis = (initialRange?.second ?: initialDate)
        .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startMillis)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endMillis)

    var isSelectingStart by remember { mutableStateOf(true) }

    // Auto-switch to End Date when Start Date is selected
    LaunchedEffect(startDatePickerState) {
        snapshotFlow { startDatePickerState.selectedDateMillis }
            .distinctUntilChanged()
            .collect { millis ->
                if (millis != null && millis != startMillis && isSelectingStart) {
                     isSelectingStart = false
                }
            }
    }

    // Helper to format date
    fun formatMillis(millis: Long?): String {
        return if (millis != null) {
            java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } else "Select Date"
    }
    
    // Animate Tab Position
    val tabBias by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelectingStart) -1f else 1f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val finalStartMillis = startDatePickerState.selectedDateMillis
                    val finalEndMillis = endDatePickerState.selectedDateMillis
                    
                    if (finalStartMillis != null && finalEndMillis != null) {
                        var start = java.time.Instant.ofEpochMilli(finalStartMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        var end = java.time.Instant.ofEpochMilli(finalEndMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        
                        // Auto-swap if start > end
                        if (start.isAfter(end)) {
                            val temp = start
                            start = end
                            end = temp
                        }

                        onRangeSelected(start to end)
                        onDismiss()
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            // Custom Sliding Header
             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Background Sliding Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f) // Takes half width
                        .fillMaxHeight()
                        .align(androidx.compose.ui.BiasAlignment(tabBias, 0f))
                        .padding(4.dp) // Gap
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                
                // Foreground Text Zones
                Row(modifier = Modifier.fillMaxSize()) {
                    // Start Date Click Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { isSelectingStart = true }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val textColor by androidx.compose.animation.animateColorAsState(
                            if (isSelectingStart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Start Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatMillis(startDatePickerState.selectedDateMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    // End Date Click Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                             .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { isSelectingStart = false }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val textColor by androidx.compose.animation.animateColorAsState(
                            if (!isSelectingStart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                         Text(
                            text = "End Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatMillis(endDatePickerState.selectedDateMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // The Picker
            // We use key to force recomposition if needed, but switching state should be enough
            // DatePicker doesn't have a 'title' param in the Composable itself (it's part of the layout usually), 
            // but we are using our own header, so we set title/headline to null or empty
            if (isSelectingStart) {
                DatePicker(
                    state = startDatePickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false
                )
            } else {
                DatePicker(
                    state = endDatePickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false
                )
            }
        }
    }
}


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
    
    // Snap behavior
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Notify selection change
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

    // Track centered item index for UI highlighting
    val centerItemIndex by remember {
        derivedStateOf {
             val layoutInfo = listState.layoutInfo
             val viewportCenter = layoutInfo.viewportEndOffset / 2
             layoutInfo.visibleItemsInfo.minByOrNull { 
                 kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
             }?.index ?: -1
        }
    }

    // Centering Logic using Box
    Box(
        modifier = Modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // Selection Indicator (Overlay)
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
                
                // Opacity calc
                val opacity by remember(isSelected) {
                    derivedStateOf {
                        if (isSelected) 1f else 0.3f
                    }
                }

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
