package com.h2.wellspend.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthYearPickerDialog(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // Current Selection
    var selectedMonth by remember { mutableStateOf(currentDate.month) }
    var selectedYear by remember { mutableIntStateOf(currentDate.year) }

    // Data Sources
    val months = Month.entries
    val years = (1900..2100).toList()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Date",
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
                Button(
                    onClick = {
                        onDateSelected(LocalDate.of(selectedYear, selectedMonth, 1))
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
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
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

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

    // Track centered item index
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
