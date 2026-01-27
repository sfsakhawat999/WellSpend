package com.h2.wellspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.h2.wellspend.data.RecurringConfig
import com.h2.wellspend.data.RecurringFrequency
import com.h2.wellspend.data.TransactionType
import com.h2.wellspend.data.Account
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RecurringConfigScreen(
    configs: List<RecurringConfig>,
    accounts: List<Account>,
    currency: String,
    onUpdate: (RecurringConfig) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<RecurringConfig?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deletingConfigId by remember { mutableStateOf<String?>(null) }

    // Group configs by transaction type
    val expenseConfigs = configs.filter { it.transactionType == TransactionType.EXPENSE }
    val incomeConfigs = configs.filter { it.transactionType == TransactionType.INCOME }
    val transferConfigs = configs.filter { it.transactionType == TransactionType.TRANSFER }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (configs.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No recurring transactions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Add a recurring transaction to see it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expense Configs
                if (expenseConfigs.isNotEmpty()) {
                    item {
                        SectionHeader("EXPENSES")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(expenseConfigs, key = { it.id }) { config ->
                        RecurringConfigItem(
                            config = config,
                            accounts = accounts,
                            currency = currency,
                            onEdit = {
                                editingConfig = config
                                showEditDialog = true
                            },
                            onDelete = {
                                deletingConfigId = config.id
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }

                // Income Configs
                if (incomeConfigs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("INCOME")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(incomeConfigs, key = { it.id }) { config ->
                        RecurringConfigItem(
                            config = config,
                            accounts = accounts,
                            currency = currency,
                            onEdit = {
                                editingConfig = config
                                showEditDialog = true
                            },
                            onDelete = {
                                deletingConfigId = config.id
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }

                // Transfer Configs
                if (transferConfigs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("TRANSFERS")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(transferConfigs, key = { it.id }) { config ->
                        RecurringConfigItem(
                            config = config,
                            accounts = accounts,
                            currency = currency,
                            onEdit = {
                                editingConfig = config
                                showEditDialog = true
                            },
                            onDelete = {
                                deletingConfigId = config.id
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && editingConfig != null) {
        EditRecurringConfigDialog(
            config = editingConfig!!,
            accounts = accounts,
            currency = currency,
            onDismiss = { 
                showEditDialog = false
                editingConfig = null
            },
            onConfirm = { updatedConfig ->
                onUpdate(updatedConfig)
                showEditDialog = false
                editingConfig = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && deletingConfigId != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                deletingConfigId = null
            },
            title = { Text("Delete Recurring Transaction") },
            text = { Text("Are you sure you want to delete this recurring transaction? This will not affect existing transactions.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(deletingConfigId!!)
                        showDeleteConfirmation = false
                        deletingConfigId = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirmation = false
                    deletingConfigId = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecurringConfigItem(
    config: RecurringConfig,
    accounts: List<Account>,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accountName = accounts.find { it.id == config.accountId }?.name ?: "No Account"
    val nextDue = try {
        LocalDate.parse(config.nextDueDate.take(10))
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        config.nextDueDate
    }
    
    val typeColor = when (config.transactionType) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> Color(0xFF4CAF50)
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.title.ifEmpty { config.category },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = config.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                     Text(
                        text = config.transactionType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = typeColor
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Next: $nextDue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (!config.note.isNullOrEmpty()) {
                    Text(
                        text = config.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currency${String.format("%.2f", config.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecurringConfigDialog(
    config: RecurringConfig,
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (RecurringConfig) -> Unit
) {
    var amount by remember { mutableStateOf(String.format("%.2f", config.amount).trimEnd('0').trimEnd('.')) }
    var title by remember { mutableStateOf(config.title) }
    var note by remember { mutableStateOf(config.note ?: "") }
    var frequency by remember { mutableStateOf(config.frequency) }
    var nextDueDate by remember { mutableStateOf(config.nextDueDate.take(10)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        nextDueDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Recurring Transaction") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text(currency) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                // Category (read-only)
                OutlinedTextField(
                    value = config.category,
                    onValueChange = {},
                    label = { Text("Category") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Frequency
                Column {
                    Text(
                        "Frequency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurringFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = frequency == freq,
                                onClick = { frequency = freq },
                                label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                    }
                }
            }

                // Next Due Date
                OutlinedTextField(
                    value = try {
                         LocalDate.parse(nextDueDate).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                     } catch (e: Exception) {
                         nextDueDate
                     },
                    onValueChange = {},
                    label = { Text("Next Due Date") },
                    readOnly = true,
                    enabled = false, 
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Select Date")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null) {
                        onConfirm(config.copy(
                            amount = amt,
                            title = title,
                            note = note,
                            frequency = frequency,
                            nextDueDate = nextDueDate
                        ))
                    }
                },
                enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
