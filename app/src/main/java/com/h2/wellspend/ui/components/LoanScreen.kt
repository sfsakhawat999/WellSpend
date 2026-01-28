package com.h2.wellspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState // implicitly needed for horizontalScroll? Yes or just scrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import com.h2.wellspend.ui.getGroupedItemShape
import com.h2.wellspend.ui.getGroupedItemBackgroundShape
import com.h2.wellspend.ui.theme.cardBackgroundColor
import com.h2.wellspend.ui.performWiggle
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.filled.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    loans: List<Loan>,
    expenses: List<Expense>,
    accounts: List<Account>,
    currency: String,
    onTransactionClick: (Loan) -> Unit,
    onEditLoan: (Loan) -> Unit,
    onDeleteLoan: (Loan, Boolean) -> Unit,
    onEditTransaction: (Expense) -> Unit,

    onDeleteTransaction: (String) -> Unit,
    onTransactionItemClick: (Expense) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onAddLoanStart: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Lent (Assets)") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Borrowed (Debts)") })
        }

        val filteredLoans = loans.filter { 
            if (selectedTab == 0) it.type == LoanType.LEND else it.type == LoanType.BORROW 
        }
        
        // Sort by Balance (Due Amount) Descending
        val sortedLoansWithBalance = filteredLoans.map { loan ->
            val loanExpenses = expenses.filter { it.loanId == loan.id }
            val sumExpense = loanExpenses.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
            val sumIncome = loanExpenses.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
            
            val balance = if (loan.type == LoanType.LEND) {
                sumExpense - sumIncome
            } else {
                sumIncome - sumExpense
            }
            Triple(loan, balance, loanExpenses)
        }.sortedByDescending { it.second }

        if (sortedLoansWithBalance.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No loans found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedLoansWithBalance) { (loan, balance, transactions) ->
                    LoanCard(
                        loan = loan,
                        balance = balance,
                        transactions = transactions,
                        accounts = accounts,
                        currency = currency,
                        onTransactionClick = { onTransactionClick(loan) },
                        onEditClick = { onEditLoan(loan) },
                        onDeleteClick = { deleteTransactions -> onDeleteLoan(loan, deleteTransactions) },
                        onEditTransaction = onEditTransaction,
                        onDeleteTransaction = onDeleteTransaction,
                        onTransactionItemClick = onTransactionItemClick,
                        shape = RoundedCornerShape(16.dp),
                        backgroundShape = RoundedCornerShape(17.dp)
                    )
                }
                
                item {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            "Swipe left/right to edit or delete.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoanCard(
    loan: Loan,
    balance: Double,
    transactions: List<Expense>,
    accounts: List<Account>,
    currency: String,
    onTransactionClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: (Boolean) -> Unit,
    onEditTransaction: (Expense) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onTransactionItemClick: (Expense) -> Unit,
    shape: Shape,
    backgroundShape: Shape
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val initialTransaction = remember(transactions) {
        transactions.find { it.title.trim().startsWith("New Loan:", ignoreCase = true) }
    }
    val isInitialAmountTracked = initialTransaction != null

    // Filter out the initial "New Loan" transaction to prevent double counting/listing
    // The Initial Amount is already shown in the header row
    val displayTransactions = remember(transactions) {
        transactions.filter { !it.title.trim().startsWith("New Loan:", ignoreCase = true) }
            .sortedWith(compareByDescending<Expense> { it.date.take(10) }.thenByDescending { it.timestamp })
    }
    
    // Calculate Progress
    val totalPrincipal = if (loan.type == LoanType.LEND) {
        loan.amount + displayTransactions.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
    } else {
        loan.amount + displayTransactions.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
    }
    
    val repaid = if (loan.type == LoanType.LEND) {
        displayTransactions.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
    } else {
        displayTransactions.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    
    val progress = if (totalPrincipal > 0) (repaid / totalPrincipal).toFloat().coerceIn(0f, 1f) else 0f
    
    // Color Logic
    val barColor = if (loan.type == LoanType.LEND) Color(0xFF10b981) else MaterialTheme.colorScheme.error
    
    // Swipe gesture state
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 70.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Delete Dialog Logic
    var showDeleteDialog by remember { mutableStateOf(false) }
    var revertTransactions by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Loan") },
            text = { 
                Column {
                    Text("Delete this loan?")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { revertTransactions = !revertTransactions }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = revertTransactions, onCheckedChange = { revertTransactions = it })
                        Text(" Revert transactions")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onDeleteClick(revertTransactions); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        // Swipeable Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                // No clip here, handled by parent Column
        ) {
            // Background Actions
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(cardBackgroundColor())
                    .clip(backgroundShape)
            ) {
                // Left Action (Edit)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                // Right Action (Delete)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
            
            // Foreground Content (Swipeable)
            Column(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                val minOffset = -actionWidthPx
                                val maxOffset = actionWidthPx
                                val newValue = (offsetX.value + delta).coerceIn(minOffset, maxOffset)
                                offsetX.snapTo(newValue)
                            }
                        },
                        onDragStopped = {
                            val targetOffset = if (offsetX.value > actionWidthPx / 2) {
                                actionWidthPx
                            } else if (offsetX.value < -actionWidthPx / 2) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                            scope.launch { offsetX.animateTo(targetOffset) }
                        }
                    )
                    .fillMaxWidth()
                    .background(cardBackgroundColor())
                    .combinedClickable(
                        onClick = { onTransactionClick() },
                        onLongClick = {
                            scope.launch { performWiggle(offsetX, actionWidthPx, context, "Swipe left/right for options") }
                        }
                    )
                    .padding(16.dp)
            ) {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = loan.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (loan.type == LoanType.LEND) "Lent" else "Borrowed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                       Text(
                            text = "$currency${String.format("%.2f", balance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                       )
                       Spacer(modifier = Modifier.width(8.dp))
                       // Expand/Collapse Arrow Button
                       Box(
                           modifier = Modifier
                               .size(32.dp)
                               .clip(CircleShape)
                               .background(MaterialTheme.colorScheme.surfaceVariant)
                               .clickable(
                                   interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                   indication = androidx.compose.material.ripple.rememberRipple(bounded = true)
                               ) { isExpanded = !isExpanded },
                           contentAlignment = Alignment.Center
                       ) {
                           Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                           )
                       }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress Bar
                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                       modifier = Modifier.fillMaxWidth(),
                       horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% Repaid",
                            style = MaterialTheme.typography.bodySmall,
                            color = barColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Total: $currency${String.format("%.0f", totalPrincipal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Expanded Content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = 2.dp)) {

                 

                 // Transactions List
                 displayTransactions.forEach { trans ->
                     androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(2.dp))
                     LoanTransactionItem(
                        loan = loan,
                        transaction = trans,
                        currency = currency,
                        accounts = accounts,
                        onEdit = onEditTransaction,
                        onDelete = onDeleteTransaction,
                        onTransactionClick = onTransactionItemClick
                     )
                 }

                 // Initial Amount Item - Swipeable but disabled (Moved to Bottom)
                 androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(2.dp))
                 val initialDensity = androidx.compose.ui.platform.LocalDensity.current
                 val initialActionWidth = 70.dp
                 val initialActionWidthPx = with(initialDensity) { initialActionWidth.toPx() }
                 val initialOffsetX = remember { androidx.compose.animation.core.Animatable(0f) }
                 val initialScope = rememberCoroutineScope()
                 val initialContext = androidx.compose.ui.platform.LocalContext.current
                 
                 // Special shape for the last item (Initial Amount) to match card bottom
                 val initialShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                 val initialBackgroundShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 17.dp, bottomEnd = 17.dp)

                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(IntrinsicSize.Min)
                         .clip(initialShape) // Clip outer box to foreground shape
                 ) {
                     // Background Actions (Disabled)
                     Box(modifier = Modifier.matchParentSize().background(cardBackgroundColor()).clip(initialBackgroundShape)) {
                         // Left Action (Edit - Disabled)
                         Box(
                             modifier = Modifier
                                 .align(Alignment.CenterStart)
                                 .width(initialActionWidth + 24.dp)
                                 .fillMaxHeight()
                                 .background(Color.Gray)
                                 .clickable {
                                     android.widget.Toast.makeText(initialContext, "Initial loan amount cannot be edited", android.widget.Toast.LENGTH_SHORT).show()
                                 },
                             contentAlignment = Alignment.CenterStart
                         ) {
                             Box(modifier = Modifier.width(initialActionWidth), contentAlignment = Alignment.Center) {
                                 Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.DarkGray)
                             }
                         }
                         
                         // Right Action (Delete - Disabled)
                         Box(
                             modifier = Modifier
                                 .align(Alignment.CenterEnd)
                                 .width(initialActionWidth + 24.dp)
                                 .fillMaxHeight()
                                 .background(Color.Gray)
                                 .clickable {
                                     android.widget.Toast.makeText(initialContext, "Initial loan amount cannot be deleted", android.widget.Toast.LENGTH_SHORT).show()
                                 },
                             contentAlignment = Alignment.CenterEnd
                         ) {
                             Box(modifier = Modifier.width(initialActionWidth), contentAlignment = Alignment.Center) {
                                 Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.DarkGray)
                             }
                         }
                     }
                     
                     // Foreground Content (Swipeable)
                     Row(
                         modifier = Modifier
                             .offset { androidx.compose.ui.unit.IntOffset(initialOffsetX.value.toInt(), 0) }
                             .draggable(
                                 orientation = Orientation.Horizontal,
                                 state = rememberDraggableState { delta ->
                                     initialScope.launch {
                                         val newValue = (initialOffsetX.value + delta).coerceIn(-initialActionWidthPx, initialActionWidthPx)
                                         initialOffsetX.snapTo(newValue)
                                     }
                                 },
                                 onDragStopped = {
                                     val targetOffset = when {
                                         initialOffsetX.value > initialActionWidthPx / 2 -> initialActionWidthPx
                                         initialOffsetX.value < -initialActionWidthPx / 2 -> -initialActionWidthPx
                                         else -> 0f
                                     }
                                     initialScope.launch { initialOffsetX.animateTo(targetOffset) }
                                 }
                             )
                             .fillMaxWidth()
                             .background(cardBackgroundColor(), initialShape)
                                .combinedClickable(
                                     onClick = {
                                         if (initialTransaction != null) {
                                             onTransactionItemClick(initialTransaction)
                                         } else {
                                             initialScope.launch { performWiggle(initialOffsetX, initialActionWidthPx, initialContext, "Swipe left/right for options") }
                                         }
                                     },
                                     onLongClick = {
                                         initialScope.launch { performWiggle(initialOffsetX, initialActionWidthPx, initialContext, "Swipe left/right for options") }
                                     }
                                 )
                             .padding(16.dp),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Column {
                             Text("Initial Amount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                             
                             val createdDate = java.time.Instant.ofEpochMilli(loan.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                             val currentYear = java.time.LocalDate.now().year
                             val pattern = if (createdDate.year == currentYear) "MMM d" else "MMM d, yyyy"
                             val dateStr = createdDate.format(java.time.format.DateTimeFormatter.ofPattern(pattern))
                             
                             Row {
                                 Text(
                                     dateStr,
                                     style = MaterialTheme.typography.bodySmall,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                                 
                                 if (!isInitialAmountTracked || (initialTransaction != null && initialTransaction.accountId == null)) {
                                     Text(
                                         " • Untracked",
                                         style = MaterialTheme.typography.bodySmall,
                                         color = androidx.compose.ui.graphics.Color(0xFFFFA000)
                                     )
                                 } else if (initialTransaction != null) {
                                     val accountName = accounts.find { it.id == initialTransaction.accountId }?.name ?: "Unknown Account"
                                     Text(
                                         " • $accountName",
                                         style = MaterialTheme.typography.bodySmall,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                     )
                                 }
                             }
                         }
                         Text(
                             "$currency${String.format("%.2f", loan.amount)}",
                             style = MaterialTheme.typography.bodyMedium,
                             fontWeight = FontWeight.Bold
                         )
                     }
                 }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoanTransactionItem(
    loan: Loan,
    transaction: Expense,
    currency: String,
    accounts: List<Account>,
    onEdit: (Expense) -> Unit,
    onDelete: (String) -> Unit,
    onTransactionClick: (Expense) -> Unit,
    shape: Shape = RoundedCornerShape(3.dp),
    backgroundShape: Shape = RoundedCornerShape(4.dp)
) {
    val amountColor = if (transaction.transactionType == TransactionType.INCOME) Color(0xFF10b981) else Color(0xFFef4444)
    val label = when {
        loan.type == LoanType.LEND && transaction.transactionType == TransactionType.EXPENSE -> "Lent more"
        loan.type == LoanType.LEND && transaction.transactionType == TransactionType.INCOME -> "Repayment"
        loan.type == LoanType.BORROW && transaction.transactionType == TransactionType.INCOME -> "Borrowed more"
        loan.type == LoanType.BORROW && transaction.transactionType == TransactionType.EXPENSE -> "Repayment"
        else -> "Transaction"
    }
    val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown Account"
    
    // Swipe gesture state
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 70.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete(transaction.id)
                    showDeleteDialog = false 
                }) {
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
            .clip(shape) // Clip outer box to foreground shape for cleaner interaction area
    ) {
        // Background Actions
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(cardBackgroundColor())
                .clip(backgroundShape)
        ) {
            // Left Action (Edit)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(actionWidth + 24.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onEdit(transaction) },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            // Right Action (Delete)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidth + 24.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { showDeleteDialog = true },
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
        
        // Foreground Content (Swipeable)
        Row(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val minOffset = -actionWidthPx
                            val maxOffset = actionWidthPx
                            val newValue = (offsetX.value + delta).coerceIn(minOffset, maxOffset)
                            offsetX.snapTo(newValue)
                        }
                    },
                    onDragStopped = {
                        val targetOffset = if (offsetX.value > actionWidthPx / 2) {
                            actionWidthPx
                        } else if (offsetX.value < -actionWidthPx / 2) {
                            -actionWidthPx
                        } else {
                            0f
                        }
                        scope.launch { offsetX.animateTo(targetOffset) }
                    }
                )
                .fillMaxWidth()
                .background(cardBackgroundColor(), shape)
                .combinedClickable(
                    onClick = { onTransactionClick(transaction) },
                    onLongClick = { onTransactionClick(transaction) }
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
             Column {
                 Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                 
                 val date = java.time.LocalDate.parse(transaction.date.take(10))
                 val currentYear = java.time.LocalDate.now().year
                 val pattern = if (date.year == currentYear) "MMM d" else "MMM d, yyyy"
                 val dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern(pattern))
                 
                 Row {
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(" • $accountName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 }
             }
             Text(
                 "$currency${String.format("%.2f", transaction.amount)}",
                 style = MaterialTheme.typography.bodyMedium,
                 fontWeight = FontWeight.Bold,
                 color = amountColor
             )
        }
    }
}
