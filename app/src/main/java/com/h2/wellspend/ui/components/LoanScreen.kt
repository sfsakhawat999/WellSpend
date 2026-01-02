package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
    onAddLoan: (String, Double, LoanType, String?, String?, Double, java.time.LocalDate) -> Unit, // Double: feeAmount
    onAddTransaction: (String, Double, Boolean, String?, LoanType, Double, java.time.LocalDate) -> Unit, // Double: feeAmount
    onUpdateLoan: (Loan) -> Unit,
    onDeleteLoan: (Loan) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Lent, 1 = Borrowed
    var isCreatingLoan by remember { mutableStateOf(false) }
    var editingLoan by remember { mutableStateOf<Loan?>(null) }
    var loanForTransaction by remember { mutableStateOf<Loan?>(null) } // If set, show transaction dialog

    if (isCreatingLoan || editingLoan != null) {
        LoanInputScreen(
            initialLoan = editingLoan,
            accounts = accounts,
            currency = currency,
            onSave = { name, amount, type, desc, accId, fee, date ->
                if (editingLoan != null) {
                     val updated = editingLoan!!.copy(name = name, amount = amount, description = desc, type = type)
                     onUpdateLoan(updated)
                     editingLoan = null
                } else {
                     onAddLoan(name, amount, type, desc, accId, fee, date)
                     isCreatingLoan = false
                }
            },
            onCancel = {
                isCreatingLoan = false
                editingLoan = null
            }
        )
    } else {

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
            FloatingActionButton(onClick = { isCreatingLoan = true }) {
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
                            onTransactionClick = { loanForTransaction = loan },
                            onEditClick = { editingLoan = loan },
                            onDeleteClick = { onDeleteLoan(loan) } // Simple delete for now
                        )
                    }
                }
            }
        }
    }
    }

        if (loanForTransaction != null) {
        AddLoanTransactionDialog(
            loan = loanForTransaction!!,
            accounts = accounts,
            currency = currency,
            onDismiss = { loanForTransaction = null },
            onConfirm = { amount, isPayment, accId, fee, date ->
                onAddTransaction(loanForTransaction!!.id, amount, isPayment, accId, loanForTransaction!!.type, fee, date)
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
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Delete states
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Loan") },
            text = { Text("Are you sure you want to delete this loan? Associated transactions will remain but become unlinked.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp)) // Clip the whole box for corners
    ) {
        // Background Actions
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action (Edit) - Revealed by swiping RIGHT
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onEditClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Right Action (Delete) - Revealed by swiping LEFT
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { showDeleteDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }

        // Foreground (Content)
        Card(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newValue = (offsetX.value + delta).coerceIn(-actionWidthPx, actionWidthPx)
                            offsetX.snapTo(newValue)
                        }
                    },
                    onDragStopped = {
                        val targetOffset = when {
                            offsetX.value > actionWidthPx / 2 -> actionWidthPx // Snap Open (Right/Edit)
                            offsetX.value < -actionWidthPx / 2 -> -actionWidthPx // Snap Open (Left/Delete)
                            else -> 0f
                        }
                        scope.launch { offsetX.animateTo(targetOffset) }
                    }
                )
                .fillMaxWidth()
                .clickable { onTransactionClick(loan) },
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp) // Match Clip
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
                }
            }
        }
    }
}

@Composable
fun AddLoanDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, LoanType, String?, String?, Double, java.time.LocalDate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LoanType.LEND) }
    var description by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<String?>(null) } // Null = No Account
    var feeAmount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now()) }
    var doNotTrack by remember { mutableStateOf(false) }

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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().clickable { doNotTrack = !doNotTrack }
                ) {
                    Checkbox(checked = doNotTrack, onCheckedChange = { doNotTrack = it })
                    Text("Do not track as transaction")
                }

                if (!doNotTrack) {
                     val accountLabel = if (selectedType == LoanType.LEND) "Pay From Account" else "Deposit To Account"
                     Text(accountLabel)
                     Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                         accounts.forEach { acc ->
                             FilterChip(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id }, label = { Text(acc.name) })
                             Spacer(Modifier.width(4.dp))
                         }
                     }
                     if (selectedAccountId == null && accounts.isNotEmpty()) {
                        LaunchedEffect(Unit) { selectedAccountId = accounts.first().id }
                     }
                } else {
                    LaunchedEffect(Unit) { selectedAccountId = null }
                }
                 // Fee Option (Only if Lending)
                if (selectedType == LoanType.LEND) {
                     OutlinedTextField(value = feeAmount, onValueChange = { feeAmount = it }, label = { Text("Transaction Fee (Optional)") })
                 }

                 // Date Selector
                 val context = androidx.compose.ui.platform.LocalContext.current
                 val dateInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                 val dateFormatted = remember(date) { date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")) }
                 
                 OutlinedTextField(
                     value = dateFormatted,
                     onValueChange = {},
                     label = { Text("Date") },
                     readOnly = true,
                     trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                     interactionSource = dateInteractionSource
                 )
                 
                 LaunchedEffect(dateInteractionSource) {
                     dateInteractionSource.interactions.collect { interaction ->
                         if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                             val datePickerDialog = android.app.DatePickerDialog(
                                 context,
                                 { _, year, month, dayOfMonth ->
                                     date = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                                 },
                                 date.year,
                                 date.monthValue - 1,
                                 date.dayOfMonth
                             )
                             datePickerDialog.show()
                         }
                     }
                 }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                val fee = feeAmount.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && amt != null) {
                    onConfirm(name, amt, selectedType, description.ifBlank { null }, if (doNotTrack) null else selectedAccountId, fee, date)
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
    onConfirm: (Double, Boolean, String?, Double, java.time.LocalDate) -> Unit // Double: feeAmount
) {
    var amount by remember { mutableStateOf("") }
    var isPayment by remember { mutableStateOf(true) } // True = Pay/Repay, False = Increase
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var feeAmount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now()) }
    var doNotTrack by remember { mutableStateOf(false) }

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
                
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().clickable { doNotTrack = !doNotTrack }
                ) {
                    Checkbox(checked = doNotTrack, onCheckedChange = { doNotTrack = it })
                    Text("Do not track as transaction")
                }

                if (!doNotTrack) {
                    // Logic:
                    // Money OUT (Expense): LEND Increase OR BORROW Payment/Repay
                    // Money IN (Income): LEND Payment/Receive OR BORROW Increase
                    val isMoneyOut = (loan.type == LoanType.LEND && !isPayment) || (loan.type == LoanType.BORROW && isPayment)
                    val accountLabel = if (isMoneyOut) "Pay From Account" else "Deposit To Account"
                    
                    Text(accountLabel)
                     Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                         accounts.forEach { acc ->
                             FilterChip(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id }, label = { Text(acc.name) })
                             Spacer(Modifier.width(4.dp))
                         }
                     }
                     if (accounts.isEmpty()) {
                         Text("No accounts. Add one.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                     } else if (selectedAccountId == null) {
                        // LaunchedEffect(Unit) { selectedAccountId = accounts.first().id }
                     }
                } else {
                    LaunchedEffect(Unit) { selectedAccountId = null }
                }

                 // Fee Logic
                 // Show fee if: (LEND && Increase (!isPayment)) OR (BORROW && Payment/Repay (isPayment))
                 // "when i am sending the money"
                 val showFee = (loan.type == LoanType.LEND && !isPayment) || (loan.type == LoanType.BORROW && isPayment)
                 if (showFee) {
                     OutlinedTextField(value = feeAmount, onValueChange = { feeAmount = it }, label = { Text("Transaction Fee (Optional)") })
                 }

                 // Date Selector
                 val context = androidx.compose.ui.platform.LocalContext.current
                 val dateInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                 val dateFormatted = remember(date) { date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")) }
                 
                 OutlinedTextField(
                     value = dateFormatted,
                     onValueChange = {},
                     label = { Text("Date") },
                     readOnly = true,
                     trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                     interactionSource = dateInteractionSource
                 )
                 
                 LaunchedEffect(dateInteractionSource) {
                     dateInteractionSource.interactions.collect { interaction ->
                         if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                             val datePickerDialog = android.app.DatePickerDialog(
                                 context,
                                 { _, year, month, dayOfMonth ->
                                     date = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                                 },
                                 date.year,
                                 date.monthValue - 1,
                                 date.dayOfMonth
                             )
                             datePickerDialog.show()
                         }
                     }
                 }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount.toDoubleOrNull() != null && (doNotTrack || selectedAccountId != null),
                onClick = {
                val amt = amount.toDoubleOrNull()
                val fee = feeAmount.toDoubleOrNull() ?: 0.0
                if (amt != null) {
                    onConfirm(amt, isPayment, if (doNotTrack) null else selectedAccountId, fee, date)
                }
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
