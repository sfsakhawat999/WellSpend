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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Surface
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
import com.h2.wellspend.utils.MathParser
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.h2.wellspend.data.LoanType
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditLoanTransactionDialog(
    initialTransaction: Expense,
    loan: Loan,
    accounts: List<Account>,
    accountBalances: Map<String, Double>,
    currency: String,
    onSave: (Double, String, String?, Double, String?, String, String) -> Unit, // amount, description, accountId, fee, feeConfigName, date, note
) {
    // BackHandler(onBack = onDismiss) // Handled by MainScreen
    var amount by remember { mutableStateOf(String.format("%.2f", initialTransaction.amount).trimEnd('0').trimEnd('.')) }
    var textDescription by remember { mutableStateOf(initialTransaction.title) }
    var note by remember { mutableStateOf(initialTransaction.note ?: "") }
    var selectedAccountId by remember { mutableStateOf(initialTransaction.accountId) }
    
    // Fee State
    var selectedFeeConfigName by remember { mutableStateOf<String?>(initialTransaction.feeConfigName) }
    var feeAmount by remember { mutableStateOf(initialTransaction.feeAmount.toString()) }
    var isCustomFee by remember { mutableStateOf(initialTransaction.feeConfigName == "Custom") }

    
    // Date State
    // Format YYYY-MM-DD from transaction.date
    var date by remember { mutableStateOf(initialTransaction.date.substring(0, 10)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(initialTransaction.timestamp) }
    val datePickerState = rememberDatePickerState()

    // Determine context for UI textual feedback
    val isLendMore = loan.type == LoanType.LEND && initialTransaction.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE
    val isReceivePay = loan.type == LoanType.LEND && initialTransaction.transactionType == com.h2.wellspend.data.TransactionType.INCOME
    val isBorrowMore = loan.type == LoanType.BORROW && initialTransaction.transactionType == com.h2.wellspend.data.TransactionType.INCOME
    val isRepay = loan.type == LoanType.BORROW && initialTransaction.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE
    
    val showFee = (isLendMore || isRepay) && selectedAccountId != null
    
    // Confirmation dialog state for saving without account
    var showNoAccountConfirmation by remember { mutableStateOf(false) }
    
    // Helper to calculate fee based on account rule
    val currentAccount = accounts.find { it.id == selectedAccountId }
    

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

    // Layout similar to TransactionForm
    Box(modifier = Modifier.fillMaxSize()) {
        var isAbcKeyboard by remember { mutableStateOf(false) }
        var isFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // Header Removed - Handled by MainScreen

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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currency,
                            style = androidx.compose.ui.text.TextStyle(fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = amount,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter {
                                    it.isDigit() || it == '.' || it == '+' || it == '-' || it == '*' || it == '/' || it == '(' || it == ')' || it == ' '
                                }
                                amount = filtered
                            },
                            textStyle = TextStyle(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (MathParser.hasMathOperator(amount) && !MathParser.areParenthesesCorrect(amount)) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (isAbcKeyboard) KeyboardType.Text else KeyboardType.Decimal
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused }
                        )
                    }
                }
                if (MathParser.hasMathOperator(amount)) {
                    val evalResult = MathParser.evaluate(amount)
                    if (evalResult != null) {
                        if (evalResult >= 0.0) {
                            Text(
                                text = "= $currency${String.format("%.2f", evalResult)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp, start = 48.dp)
                            )
                        } else {
                            Text(
                                text = "= -$currency${String.format("%.2f", kotlin.math.abs(evalResult))} (Negative amount not allowed)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp, start = 48.dp)
                            )
                        }
                    }
                }
            }
                
                OutlinedTextField(
                    value = textDescription, 
                    onValueChange = { textDescription = it }, 
                    label = { Text("Title") },
                    singleLine = true,
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
                AccountSelector(
                    accounts = accounts,
                    accountBalances = accountBalances,
                    currency = currency,
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = { selectedAccountId = it },
                    title = "Account"
                )

                
                if (showFee) {
                    val parsedAmount = (if (MathParser.hasMathOperator(amount)) MathParser.evaluate(amount) else amount.toDoubleOrNull()) ?: 0.0
                    FeeSelector(
                        account = currentAccount,
                        transactionAmount = parsedAmount,
                        currency = currency,
                        selectedConfigName = selectedFeeConfigName,
                        currentFeeAmount = feeAmount,
                        isCustomFee = isCustomFee,
                        onFeeChanged = { name, fee, isCustom ->
                             selectedFeeConfigName = name
                             feeAmount = fee
                             isCustomFee = isCustom
                        }
                    )
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 300) note = it },
                    label = { Text("Note") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Text(
                            text = "${note.length}/300",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
        ) {
            // Helper function to perform the save
            val performSave = {
                val amt = if (MathParser.hasMathOperator(amount)) MathParser.evaluate(amount) else amount.toDoubleOrNull()
                if (amt != null) {
                    val fee = if (selectedAccountId != null) feeAmount.toDoubleOrNull() ?: 0.0 else 0.0
                    val config = if (selectedAccountId != null) {
                        if(isCustomFee) "Custom" else selectedFeeConfigName
                    } else null
                    onSave(amt, textDescription, selectedAccountId, fee, config, date, note)
                }
            }
            
            Button(
                onClick = {
                    if (selectedAccountId == null) {
                        showNoAccountConfirmation = true
                    } else {
                        performSave()
                    }
                },
                enabled = amount.isNotEmpty() && (if (MathParser.hasMathOperator(amount)) {
                    MathParser.areParenthesesCorrect(amount) && MathParser.evaluate(amount) != null && MathParser.evaluate(amount)!! >= 0.0
                } else {
                    val amtVal = amount.toDoubleOrNull()
                    amtVal != null && amtVal >= 0.0
                }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                 Icon(Icons.Default.Check, contentDescription = null)
                 Spacer(modifier = Modifier.size(8.dp))
                 Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            // Confirmation dialog for no account
            if (showNoAccountConfirmation) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showNoAccountConfirmation = false },
                    title = { Text("No Account Selected") },
                    text = { Text("This transaction will not be linked to any account and won't affect account balances. Continue?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showNoAccountConfirmation = false
                            performSave()
                        }) {
                            Text("Save Anyway")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNoAccountConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    val isKeyboardVisible = com.h2.wellspend.utils.KeyboardUtils.keyboardAsState().value
    if (isFocused && isKeyboardVisible) {
        Surface(
            onClick = {
                isAbcKeyboard = !isAbcKeyboard
                focusRequester.requestFocus()
            },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .imePadding()
                .padding(16.dp),
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAbcKeyboard) "123" else "Abc",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
    }
}
