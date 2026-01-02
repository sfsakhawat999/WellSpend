package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.LoanType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanInputScreen(
    initialLoan: Loan? = null,
    accounts: List<Account>,
    currency: String,
    onSave: (String, Double, LoanType, String?, String?, Double, LocalDate) -> Unit, // name, amount, type, desc, accId, fee, date
    onCancel: () -> Unit
) {
    // State initialization
    var name by remember { mutableStateOf(initialLoan?.name ?: "") }
    var amount by remember { mutableStateOf(initialLoan?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(initialLoan?.type ?: LoanType.LEND) }
    var description by remember { mutableStateOf(initialLoan?.description ?: "") }
    // ...
    // ...
    var selectedAccountId by remember { mutableStateOf<String?>(null) } 
    var doNotTrack by remember { mutableStateOf(false) }
    var feeAmount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(if (initialLoan != null) Instant.ofEpochMilli(initialLoan.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() else LocalDate.now()) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (initialLoan != null) "Edit Loan" else "New Loan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(48.dp)) // Balance the close button
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Amount Input - Big & Center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                     Text(
                        text = currency,
                        style = TextStyle(fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    // Using basic TextField for cleaner look
                    TextField(
                        value = amount,
                        onValueChange = { amount = it },
                        textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                        placeholder = { Text("0", style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))) },
                        singleLine = true,
                        enabled = initialLoan == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            disabledIndicatorColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
                if (initialLoan != null) {
                    Text("Initial amount cannot be changed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Name
            Text("Person / Entity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Who is this loan for?") }
            )

            // Type Selector (Disabled if editing)
            Text("Loan Type", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val types = listOf(LoanType.LEND, LoanType.BORROW)
                types.forEach { type ->
                    val isSelected = selectedType == type
                    // If initialLoan exists, disable changing type interaction visually or logically
                    val isEnabled = initialLoan == null
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable(enabled = isEnabled) { selectedType = type }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (type == LoanType.LEND) "Lend (I give)" else "Borrow (I take)",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isEnabled) 1f else 0.5f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (initialLoan != null) {
                Text("Loan type cannot be changed after creation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
            }

            // Description
            Text("Description", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = description, 
                onValueChange = { description = it }, 
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Optional description") }
            )

            // Date
            Text("Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp).then(if (initialLoan == null) Modifier.clickable { showDatePicker = true } else Modifier),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { Icon(Icons.Default.DateRange, "Select Date", tint = if (initialLoan == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f)) },
                enabled = false, 
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            // Need to actually trigger the click
            // A overlay box is easiest
            // See AddExpenseForm impl

            // Account Section (Only show for New Loan? Or allow editing context implied? 
            // Usually editing a loan doesn't change the source account of the initial transaction easily. 
            // For now, let's HIDE account/fee for EDIT mode to simplify, as requested "edit loans" usually means rename/amount.
            // If user wants to change account, they edit the specific transaction.)
            
            if (initialLoan == null) {
                // Account Selector
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().clickable { doNotTrack = !doNotTrack }.padding(vertical = 8.dp)
                ) {
                    Checkbox(checked = doNotTrack, onCheckedChange = { doNotTrack = it })
                    Text("Do not track as transaction")
                }

                if (!doNotTrack) {
                    val accountLabel = if (selectedType == LoanType.LEND) "Pay From Account" else "Deposit To Account"
                    Text(accountLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                     if (selectedAccountId == null && accounts.isNotEmpty()) {
                        LaunchedEffect(Unit) { selectedAccountId = accounts.first().id }
                     }
                } else {
                    LaunchedEffect(Unit) { selectedAccountId = null }
                }

                // Fee (Only if Lending AND not tracking? No, Fee applies to transaction.)
                if (selectedType == LoanType.LEND && !doNotTrack) {
                    Spacer(Modifier.height(16.dp))
                     Text("Transaction Fee (Optional)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                     OutlinedTextField(
                        value = feeAmount, 
                        onValueChange = { feeAmount = it }, 
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
        
        // Save Button
        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    val fee = feeAmount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt != null) {
                        onSave(name, amt, selectedType, description.ifBlank { null }, if (doNotTrack) null else selectedAccountId, fee, date)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank() && amount.toDoubleOrNull() != null
            ) {
                 Icon(Icons.Default.Check, contentDescription = null)
                 Spacer(Modifier.width(8.dp))
                 Text("Save Loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
