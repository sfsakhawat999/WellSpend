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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.LoanType
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoanTransactionDialog(
    transaction: Expense,
    loan: Loan,
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String?, Double, String?, String) -> Unit // amount, desc, accId, fee, feeConfigName, date
) {
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.size(48.dp)) // Balance close button
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            val fee = feeAmount.toDoubleOrNull() ?: 0.0
                            if (amt != null && selectedAccountId != null) {
                                onConfirm(amt, description, selectedAccountId, fee, selectedFeeConfigName, date)
                            }
                        },
                        enabled = amount.isNotBlank() && selectedAccountId != null,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount Input
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currency,
                            style = androidx.compose.ui.text.TextStyle(fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        androidx.compose.material3.TextField(
                            value = amount,
                            onValueChange = { amount = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                            placeholder = { Text("0", style = androidx.compose.ui.text.TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth(0.6f)
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
                
                Spacer(modifier = Modifier.height(100.dp)) // Bottom padding
            }
        }
    }
}
