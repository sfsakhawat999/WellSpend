package com.h2.wellspend.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.LoanType

@Composable
fun EditLoanTransactionDialog(
    transaction: Expense,
    loan: Loan,
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String?, Double) -> Unit // amount, desc, accId, fee
) {
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var description by remember { mutableStateOf(transaction.description) }
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    var feeAmount by remember { mutableStateOf(transaction.feeAmount.toString()) }

    // Determine context for UI textual feedback
    // LOAN TYPE | TX TYPE | CONTEXT
    // LEND      | EXPENSE | You Lent More
    // LEND      | INCOME  | You Received Payment
    // BORROW    | INCOME  | You Borrowed More
    // BORROW    | EXPENSE | You Repaid
    
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("Description") }
                )
                
                Text("Account", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                    FilterChip(
                         selected = selectedAccountId == null,
                         onClick = { selectedAccountId = null },
                         label = { Text("None") }
                    )
                    Spacer(Modifier.width(4.dp))
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
                    OutlinedTextField(
                        value = feeAmount, 
                        onValueChange = { feeAmount = it }, 
                        label = { Text("Transaction Fee") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    val fee = feeAmount.toDoubleOrNull() ?: 0.0
                    if (amt != null) {
                        onConfirm(amt, description, selectedAccountId, fee)
                    }
                },
                enabled = amount.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
