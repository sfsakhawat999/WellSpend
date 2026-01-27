package com.h2.wellspend.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.h2.wellspend.MainViewModel
import com.h2.wellspend.data.TransactionType
import androidx.compose.ui.draw.alpha
import java.time.LocalDate
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterDialog(
    currentFilter: MainViewModel.SearchFilter,
    onApply: (MainViewModel.SearchFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentFilter.type) }
    var startDate by remember { mutableStateOf(currentFilter.startDate) }
    var endDate by remember { mutableStateOf(currentFilter.endDate) }
    
    // Helper to manage date picker dialogs
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                // Removed navigationBarsPadding() here to use explicit Spacer instead
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Filter Search Results",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Transaction Type Filter
            Text("Transaction Type", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = { selectedType = TransactionType.TRANSFER },
                    label = { Text("Transfer") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Date Range Filter
            Text("Date Range", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Date Input
                OutlinedTextField(
                    value = startDate?.toString() ?: "",
                    onValueChange = {}, // Read only
                    label = { Text("Start Date") },
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartDatePicker = true },
                    enabled = false, // Disable typing, handled by click
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                )
                
                // End Date Input
                OutlinedTextField(
                    value = endDate?.toString() ?: "",
                    onValueChange = {}, // Read only
                    label = { Text("End Date") },
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                         .clickable { showEndDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            // Clear Date Button (Always occupy space to prevent layout shift)
            val hasDates = startDate != null || endDate != null
            TextButton(
                onClick = { 
                    startDate = null
                    endDate = null 
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .alpha(if (hasDates) 1f else 0f),
                enabled = hasDates
            ) {
                Text("Clear Dates")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onApply(MainViewModel.SearchFilter(selectedType, startDate, endDate))
                    onDismiss()
                }) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        endDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
