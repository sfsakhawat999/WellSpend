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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.LoanType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanTransactionScreen(
    loan: Loan,
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean, String?, Double, String?, LocalDate) -> Unit // feeConfigName added
) {
    var amount by remember { mutableStateOf("") }
    var isPayment by remember { mutableStateOf(true) } // True = Pay/Repay, False = Increase Loan
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    
    // Fee State
    var selectedFeeConfigName by remember { mutableStateOf<String?>("None") }
    var feeAmount by remember { mutableStateOf("") }
    var isCustomFee by remember { mutableStateOf(false) }

    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var doNotTrack by remember { mutableStateOf(false) }
    
    // Calculate Fee Logic
    val currentAccount = accounts.find { it.id == selectedAccountId }

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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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
                        text = "Update Loan: ${loan.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            },
            bottomBar = {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        enabled = amount.toDoubleOrNull() != null && (doNotTrack || selectedAccountId != null),
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            val fee = feeAmount.toDoubleOrNull() ?: 0.0
                            if (amt != null) {
                                onConfirm(amt, isPayment, if (doNotTrack) null else selectedAccountId, fee, selectedFeeConfigName, date)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Confirm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                Row {
                     // LEND: Pay = Receive Money, Increase = Give Money
                     // BORROW: Pay = Give Money, Increase = Receive Money
                     val payText = if (loan.type == LoanType.LEND) "Receive Payment" else "Repay Loan"
                     val incText = if (loan.type == LoanType.LEND) "Lend More" else "Borrow More"
                     
                    FilterChip(selected = isPayment, onClick = { isPayment = true }, label = { Text(payText) })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = !isPayment, onClick = { isPayment = false }, label = { Text(incText) })
                }
                
                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // Date Field
                val dateFormatted = remember(date) { date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")) }
                OutlinedTextField(
                    value = dateFormatted,
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    enabled = false,
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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().clickable { doNotTrack = !doNotTrack }
                ) {
                    Checkbox(checked = doNotTrack, onCheckedChange = { doNotTrack = it })
                    Text("Do not track as transaction")
                }

                if (!doNotTrack) {
                    val isMoneyOut = (loan.type == LoanType.LEND && !isPayment) || (loan.type == LoanType.BORROW && isPayment)
                    val accountLabel = if (isMoneyOut) "Pay From Account" else "Deposit To Account"
                    
                    Text(accountLabel, style = MaterialTheme.typography.bodySmall)
                     Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                         accounts.forEach { acc ->
                             FilterChip(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id }, label = { Text(acc.name) })
                             Spacer(Modifier.width(4.dp))
                         }
                     }
                     if (accounts.isEmpty()) {
                         Text("No accounts. Add one.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                     }
                } else {
                    LaunchedEffect(Unit) { selectedAccountId = null }
                }

                 // Fee Logic
                 val showFee = (loan.type == LoanType.LEND && !isPayment) || (loan.type == LoanType.BORROW && isPayment)
                 if (showFee && !doNotTrack) {
                    Text("Transaction Fees", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
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
                         Spacer(modifier = Modifier.height(8.dp))
                         OutlinedTextField(
                            value = feeAmount,
                            onValueChange = { feeAmount = it; if(!isCustomFee) isCustomFee = true; selectedFeeConfigName = "Custom" },
                            label = { Text("Fee Amount") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                         )
                    }
                 }
                 
                 Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
