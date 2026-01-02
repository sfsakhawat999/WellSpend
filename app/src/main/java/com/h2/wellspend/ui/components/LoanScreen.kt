package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState // implicitly needed for horizontalScroll? Yes or just scrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.LoanType
import com.h2.wellspend.data.TransactionType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    loans: List<Loan>,
    expenses: List<Expense>, // To calc balance
    accounts: List<Account>,
    currency: String,
    onAddLoan: (String, Double, LoanType, String?, String?) -> Unit,
    onAddTransaction: (String, Double, Boolean, String?, LoanType) -> Unit, // Boolean: isPayment
    onDeleteLoan: (Loan) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Lent, 1 = Borrowed
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var loanForTransaction by remember { mutableStateOf<Loan?>(null) } // If set, show transaction dialog

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddLoanDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Lent (Assets)") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Borrowed (Debts)") })
            }

            val filteredLoans = loans.filter { 
                if (selectedTab == 0) it.type == LoanType.LEND else it.type == LoanType.BORROW 
            }

            if (filteredLoans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No loans found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLoans) { loan ->
                        // Calculate Balance
                        // LEND: Init (Expense) + Increase (Expense) - Pay (Income)
                        // BORROW: Init (Income) + Increase (Income) - Pay (Expense)
                        // Actually, logic is simpler:
                        // LEND: Sum(Expense) - Sum(Income)
                        // BORROW: Sum(Income) - Sum(Expense)
                        
                        val loanExpenses = expenses.filter { it.loanId == loan.id }
                        val sumExpense = loanExpenses.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
                        val sumIncome = loanExpenses.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
                        
                        val balance = if (loan.type == LoanType.LEND) {
                             sumExpense - sumIncome
                        } else {
                             sumIncome - sumExpense
                        }

                        LoanItem(
                            loan = loan,
                            balance = balance,
                            currency = currency,
                            onTransactionClick = { loanForTransaction = it },
                            onDeleteClick = { onDeleteLoan(loan) } // Simple delete for now
                        )
                    }
                }
            }
        }
    }

    if (showAddLoanDialog) {
        AddLoanDialog(
            accounts = accounts,
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { name, amount, type, desc, accId ->
                onAddLoan(name, amount, type, desc, accId)
                showAddLoanDialog = false
            }
        )
    }

    if (loanForTransaction != null) {
        AddLoanTransactionDialog(
            loan = loanForTransaction!!,
            accounts = accounts,
            currency = currency,
            onDismiss = { loanForTransaction = null },
            onConfirm = { amount, isPayment, accId ->
                onAddTransaction(loanForTransaction!!.id, amount, isPayment, accId, loanForTransaction!!.type)
                loanForTransaction = null
            }
        )
    }
}

@Composable
fun LoanItem(
    loan: Loan,
    balance: Double,
    currency: String,
    onTransactionClick: (Loan) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onTransactionClick(loan) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = loan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!loan.description.isNullOrEmpty()) {
                    Text(text = loan.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currency${String.format("%.2f", balance)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (balance > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                 // Delete button small
                 IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                     Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                 }
            }
        }
    }
}

@Composable
fun AddLoanDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, LoanType, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LoanType.LEND) }
    var description by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<String?>(null) } // Null = No Account

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (Person/Entity)") })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }) // Keyboard type technically
                
                Row {
                    FilterChip(
                        selected = selectedType == LoanType.LEND,
                        onClick = { selectedType = LoanType.LEND },
                        label = { Text("Lend (I give)") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedType == LoanType.BORROW,
                        onClick = { selectedType = LoanType.BORROW },
                        label = { Text("Borrow (I take)") }
                    )
                }

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") })

                // Account Selector
                Text("Source/Dest Account (Optional)")
                // Simple dropdown or list ?
                // For simplicity, just a few chips or a click to select logic.
                // Or "None" + List
                 Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                     FilterChip(selected = selectedAccountId == null, onClick = { selectedAccountId = null }, label = { Text("None") })
                     accounts.forEach { acc ->
                         Spacer(Modifier.width(4.dp))
                         FilterChip(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id }, label = { Text(acc.name) })
                     }
                 }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                if (name.isNotBlank() && amt != null) {
                    onConfirm(name, amt, selectedType, description.ifBlank { null }, selectedAccountId)
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddLoanTransactionDialog(
    loan: Loan,
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var isPayment by remember { mutableStateOf(true) } // True = Pay/Repay, False = Increase
    var selectedAccountId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Loan: ${loan.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                     // LEND: Pay = Receive Money, Increase = Give Money
                     // BORROW: Pay = Give Money, Increase = Receive Money
                     val payText = if (loan.type == LoanType.LEND) "Receive Payment" else "Repay Loan"
                     val incText = if (loan.type == LoanType.LEND) "Lend More" else "Borrow More"
                     
                    FilterChip(selected = isPayment, onClick = { isPayment = true }, label = { Text(payText) })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = !isPayment, onClick = { isPayment = false }, label = { Text(incText) })
                }
                
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") })
                
                Text(if (isPayment) "To/From Account" else "Source/Dest Account")
                 Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                     FilterChip(selected = selectedAccountId == null, onClick = { selectedAccountId = null }, label = { Text("None") })
                     accounts.forEach { acc ->
                         Spacer(Modifier.width(4.dp))
                         FilterChip(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id }, label = { Text(acc.name) })
                     }
                 }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt != null) {
                    onConfirm(amt, isPayment, selectedAccountId)
                }
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
