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
import androidx.activity.compose.BackHandler
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.LoanType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanInputScreen(
    initialLoan: Loan? = null,
    accounts: List<Account>,
    accountBalances: Map<String, Double>,
    currency: String,
    selectedType: LoanType = LoanType.LEND, // Hoisted defaults
    onTypeChange: (LoanType) -> Unit = {}, 
    onSave: (String, Double, LoanType, String?, String?, Double, String?, LocalDate) -> Unit, // name, amount, type, desc, accId, fee, feeConfigName, date
    onCancel: () -> Unit
) {
    // BackHandler(onBack = onCancel) // Handled by MainScreen now
    
    // State initialization
    var name by remember { mutableStateOf(initialLoan?.name ?: "") }
    var amount by remember { mutableStateOf(initialLoan?.amount?.let { String.format("%.2f", it).trimEnd('0').trimEnd('.') } ?: "") }
    // selectedType hoisted to MainScreen
    var description by remember { mutableStateOf(initialLoan?.description ?: "") }
    var selectedAccountId by remember { mutableStateOf<String?>(null) } 
    var doNotTrack by remember { mutableStateOf(false) }
    
    // Fee State
    var selectedFeeConfigName by remember { mutableStateOf<String?>(null) }
    var feeAmount by remember { mutableStateOf("") }
    var isCustomFee by remember { mutableStateOf(false) }

    var date by remember { mutableStateOf(if (initialLoan != null) Instant.ofEpochMilli(initialLoan.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() else LocalDate.now()) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    // Calculate Fee
    val currentAccount = accounts.find { it.id == selectedAccountId }

// Fee calculation moved to FeeSelector interaction

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
        // Header Removed - Handled by MainScreen
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Amount Input - Big & Left aligned to match expense form
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = currency,
                        style = TextStyle(fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            if (initialLoan == null) {
                                // Allow only valid decimal input with max 2 decimal places
                                val filtered = newValue.filter { it.isDigit() || it == '.' }
                                val parts = filtered.split(".")
                                amount = when {
                                    parts.size == 1 -> filtered
                                    parts.size == 2 -> "${parts[0]}.${parts[1].take(2)}"
                                    else -> amount
                                }
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (initialLoan == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (amount.isEmpty()) {
                                    Text(
                                        "0",
                                        style = TextStyle(
                                            fontSize = 56.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        singleLine = true,
                        enabled = initialLoan == null,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.widthIn(min = 200.dp)
                    )
                }
                if (initialLoan != null) {
                    Text("Initial amount cannot be changed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Name
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                label = { Text("Person / Entity") },
                placeholder = { Text("Who is this loan for?") },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Type Selector removed (Hoisted to TopAppBar)
            if (initialLoan != null) {
                Text("Loan type cannot be changed after creation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
            }

            // Date
            OutlinedTextField(
                value = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).then(if (initialLoan == null) Modifier.clickable { showDatePicker = true } else Modifier),
                shape = RoundedCornerShape(12.dp),
                label = { Text("Date") },
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

            // Account Section
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
                    AccountSelector(
                        accounts = accounts,
                        accountBalances = accountBalances,
                        selectedAccountId = selectedAccountId,
                        onAccountSelected = { selectedAccountId = it },
                        currency = currency,
                        title = accountLabel
                    )
                } else {
                    LaunchedEffect(Unit) { selectedAccountId = null }
                }

                // Fee (Only if Lending AND not tracking? No, Fee applies to transaction.)
                if (selectedType == LoanType.LEND && !doNotTrack) {
                    Spacer(Modifier.height(16.dp))
                    FeeSelector(
                        account = currentAccount,
                        transactionAmount = amount.toDoubleOrNull() ?: 0.0,
                        currency = currency,
                        selectedConfigName = selectedFeeConfigName,
                        currentFeeAmount = feeAmount,
                        isCustomFee = isCustomFee,
                        onFeeChanged = { name, amt, isCustom ->
                            selectedFeeConfigName = name
                            feeAmount = amt
                            isCustomFee = isCustom
                        }
                    )
                }
            }

            // Description (Moved to bottom)
            OutlinedTextField(
                value = description, 
                onValueChange = { description = it }, 
                modifier = Modifier.fillMaxWidth().padding(top=8.dp, bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                label = { Text("Description") },
                placeholder = { Text("Optional description") },
                minLines = 3,
                maxLines = 5
            )
        }
        
        // Save Button
        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    val fee = feeAmount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt != null) {
                        onSave(name, amt, selectedType, description.ifBlank { null }, if (doNotTrack) null else selectedAccountId, fee, selectedFeeConfigName, date)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank() && amount.toDoubleOrNull() != null && (doNotTrack || selectedAccountId != null || initialLoan != null)
            ) {
                 Icon(Icons.Default.Check, contentDescription = null)
                 Spacer(Modifier.width(8.dp))
                 Text("Save Loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
