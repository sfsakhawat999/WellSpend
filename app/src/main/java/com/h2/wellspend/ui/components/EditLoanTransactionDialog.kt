package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.activity.compose.BackHandler
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.LoanType
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoanTransactionScreen(
    transaction: Expense,
    loan: Loan,
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String?, Double, String?, String) -> Unit // amount, desc, accId, fee, feeConfigName, date
) {
    BackHandler(onBack = onDismiss)
    var amount by remember { mutableStateOf(String.format("%.2f", transaction.amount).trimEnd('0').trimEnd('.')) }
    var description by remember { mutableStateOf(transaction.description) }
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    
    // Fee State
    var selectedFeeConfigName by remember { mutableStateOf<String?>(transaction.feeConfigName) }
    var feeAmount by remember { mutableStateOf(transaction.feeAmount.toString()) }
    var isCustomFee by remember { mutableStateOf(transaction.feeConfigName == "Custom") }

    var doNotTrack by remember { mutableStateOf(false) } // Removed/Ignored

    
    // Date State
    // Format YYYY-MM-DD from transaction.date
    var date by remember { mutableStateOf(transaction.date.substring(0, 10)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Determine context for UI textual feedback
    val isLendMore = loan.type == LoanType.LEND && transaction.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE
    val isReceivePay = loan.type == LoanType.LEND && transaction.transactionType == com.h2.wellspend.data.TransactionType.INCOME
    val isBorrowMore = loan.type == LoanType.BORROW && transaction.transactionType == com.h2.wellspend.data.TransactionType.INCOME
    val isRepay = loan.type == LoanType.BORROW && transaction.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE
    
    val title = when {
        isLendMore -> "Edit Lending"
        isReceivePay -> "Edit Received Payment"
        isBorrowMore -> "Edit Borrowing"
        isRepay -> "Edit Repayment"
        else -> "Edit Transaction"
    }

    val showFee = isLendMore || isRepay
    
    // Helper to calculate fee based on account rule
    val currentAccount = accounts.find { it.id == selectedAccountId }
    
    // Auto-update fee when account or amount changes, unless custom
    LaunchedEffect(amount, selectedAccountId, selectedFeeConfigName) {
        if (!isCustomFee && selectedFeeConfigName != null && selectedFeeConfigName != "None" && selectedFeeConfigName != "Custom") {
            val config = currentAccount?.feeConfigs?.find { it.name == selectedFeeConfigName }
            if (config != null) {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val calculated = if (config.isPercentage) (amt * config.value / 100) else config.value
                feeAmount = String.format("%.2f", calculated)
            }
        } else if (selectedFeeConfigName == "None") {
            feeAmount = "0.0"
        }
    }

    // Date Picker Logic
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Layout similar to AddExpenseForm
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
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(32.dp))
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Big Amount Input - Left aligned to match expense form
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = currency,
                        style = androidx.compose.ui.text.TextStyle(fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Allow only valid decimal input with max 2 decimal places
                            val filtered = newValue.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split(".")
                            amount = when {
                                parts.size == 1 -> filtered
                                parts.size == 2 -> "${parts[0]}.${parts[1].take(2)}"
                                else -> amount
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start
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
                                            textAlign = TextAlign.Start
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.widthIn(min = 200.dp)
                    )
                }
            }
                
                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Date Field
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    enabled = false, // To make it look like read-only click target
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                // Checkbox removed

                // Account Selection
                Text("Account", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                     accounts.forEach { acc ->
                         FilterChip(
                             selected = selectedAccountId == acc.id,
                             onClick = { selectedAccountId = acc.id },
                             label = { Text(acc.name) }
                         )
                         Spacer(Modifier.width(4.dp))
                     }
                }

                
                if (showFee) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transaction Fees", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = selectedFeeConfigName == "None" || selectedFeeConfigName == null, onClick = { selectedFeeConfigName = "None"; isCustomFee = false }, label = { Text("None") })
                            
                            currentAccount?.feeConfigs?.forEach { config ->
                                FilterChip(
                                    selected = selectedFeeConfigName == config.name,
                                    onClick = { selectedFeeConfigName = config.name; isCustomFee = false },
                                    label = { Text("${config.name} (${if(config.isPercentage) "${config.value}%" else currency + config.value})") }
                                )
                            }
                            
                            FilterChip(selected = isCustomFee, onClick = { selectedFeeConfigName = "Custom"; isCustomFee = true }, label = { Text("Custom") })
                        }
                        
                        if (isCustomFee || (feeAmount.toDoubleOrNull() ?: 0.0) > 0) {
                             OutlinedTextField(
                                value = feeAmount,
                                onValueChange = { feeAmount = it; if(!isCustomFee) isCustomFee = true; selectedFeeConfigName = "Custom" },
                                label = { Text("Fee Amount") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                             )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                     val amt = amount.toDoubleOrNull()
                     if (amt != null) {
                         val fee = feeAmount.toDoubleOrNull() ?: 0.0
                         val config = if(isCustomFee) "Custom" else selectedFeeConfigName
                         onConfirm(amt, description, selectedAccountId, fee, config, date)
                     }
                },
                enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null && selectedAccountId != null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                 Icon(Icons.Default.Check, contentDescription = null)
                 Spacer(modifier = Modifier.size(8.dp))
                 Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
