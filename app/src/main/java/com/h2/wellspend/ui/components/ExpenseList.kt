package com.h2.wellspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.ui.getCategoryColor
import com.h2.wellspend.ui.getCategoryIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import kotlinx.coroutines.delay

@Composable
fun ExpenseList(
    expenses: List<Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    state: LazyListState = rememberLazyListState()
) {
    // Flatten expenses to include virtual fees
    val displayExpenses = remember(expenses) {
        expenses.flatMap { expense ->
            val list = mutableListOf(expense)
            if (expense.feeAmount > 0) {
                list.add(
                    expense.copy(
                        id = "fee_${expense.id}",
                        amount = expense.feeAmount,
                        category = Category.TransactionFee,
                        description = "Fee for ${expense.description}",
                        feeAmount = 0.0, // Virtual item has no fee on itself
                        transactionType = com.h2.wellspend.data.TransactionType.EXPENSE // Fees are always Expenses
                    )
                )
            }
            list
        }
    }

    // Group expenses by category
    val groupedExpenses = remember(displayExpenses) {
        displayExpenses.groupBy { it.category }
            .mapValues { entry ->
                // Sum only Amounts (Virtual fees are now proper Expenses with Amount)
                // Filter for EXPENSE type to match existing logic (assuming this list is for Expenses)
                val total = entry.value.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }.sumOf { it.amount } 

                val items = entry.value.sortedWith(
                    compareByDescending<Expense> { it.date.take(10) }
                        .thenByDescending { it.timestamp }
                )
                Pair(total, items)
            }
            .toList()
            .sortedByDescending { it.second.first } // Sort by total amount
    }

    if (groupedExpenses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses recorded for this period.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp), // Space for FAB
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(groupedExpenses) { (category, data) ->
                val (total, items) = data
                ExpenseCategoryItem(
                    category = category,
                    total = total,
                    items = items,
                    accounts = accounts,
                    loans = loans,
                    currency = currency,
                    onDelete = onDelete,
                    onEdit = { expense ->
                        if (expense.id.startsWith("fee_")) {
                            val realId = expense.id.removePrefix("fee_")
                            val realExpense = expenses.find { it.id == realId }
                            if (realExpense != null) {
                                onEdit(realExpense)
                            }
                        } else {
                            onEdit(expense)
                        }
                    }
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Swipe left/right to edit or delete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseCategoryItem(
    category: Category,
    total: Double,
    items: List<Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val color = getCategoryColor(category)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = category.name,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${items.size} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currency${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(12.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded List
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                items.forEach { expense ->
                    key(expense.id) {
                        Column {
                            ExpenseItem(
                                expense = expense,
                                currency = currency,
                                accounts = accounts,
                                loans = loans,
                                onDelete = onDelete,
                                onEdit = onEdit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeeItem(
    expense: Expense,
    currency: String,
    onEdit: (Expense) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant) // Slightly different background
            .clickable { onEdit(expense) } // Editing fee opens parent expense
            .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), // Indented
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AttachMoney, // Or some other icon?
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column {
                Text(
                    text = "Transaction Fee${if (expense.feeConfigName != null) " (${expense.feeConfigName})" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "$currency${String.format("%.2f", expense.feeAmount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



@Composable
fun ExpenseItem(
    expense: Expense,
    currency: String,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit
) {
    val date = try {
        java.time.LocalDate.parse(expense.date.take(10))
    } catch (e: Exception) {
        java.time.LocalDate.now()
    }
    val formattedDate = date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Delete states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    var deleteConfirmed by remember { mutableStateOf(false) }

    // Account & Loan Info
    val accountName = accounts.find { it.id == expense.accountId }?.name
    val loanName = if (expense.loanId != null) loans.find { it.id == expense.loanId }?.name else null
    
    // Check if this is a balance adjustment (non-editable)
    val isBalanceAdjustment = expense.category == Category.BalanceAdjustment
    
    val extraInfo = buildString {
        if (expense.transactionType == com.h2.wellspend.data.TransactionType.INCOME) append(" • Income")
        else if (expense.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER) append(" • Transfer")
        
        if (loanName != null) append(" • Loan: $loanName")
        if (accountName != null) append(" • $accountName")
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        deleteConfirmed = true
                        showDeleteDialog = false
                        isVisible = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Trigger actual delete after animation
    LaunchedEffect(isVisible) {
        if (!isVisible && deleteConfirmed) {
            kotlinx.coroutines.delay(300) // Wait for animation
            onDelete(expense.id)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically() + fadeOut(),
        enter = expandVertically() + fadeIn()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
        ) {
            // Background (Actions)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Action (Visible when swiped right -> EDIT) - Hidden for BalanceAdjustment
                if (!isBalanceAdjustment) {
                    Box(
                        modifier = Modifier
                            .width(actionWidth)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onEdit(expense) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(actionWidth))
                }

                // Right Action (Visible when swiped left)
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
            Row(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                // For BalanceAdjustment, only allow swipe left (delete), not right (edit)
                                val minOffset = -actionWidthPx
                                val maxOffset = if (isBalanceAdjustment) 0f else actionWidthPx
                                val newValue = (offsetX.value + delta).coerceIn(minOffset, maxOffset)
                                offsetX.snapTo(newValue)
                            }
                        },
                        onDragStopped = {
                            val targetOffset = if (offsetX.value > actionWidthPx / 2) {
                                actionWidthPx // Snap Open (Right/Edit)
                            } else if (offsetX.value < -actionWidthPx / 2) {
                                -actionWidthPx // Snap Open (Left/Delete)
                            } else {
                                0f
                            }
                            scope.launch { offsetX.animateTo(targetOffset) }
                        }
                    )
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface) // Opaque background to hide actions
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description.ifEmpty { "No description" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formattedDate + extraInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                val amountPrefix = if (expense.transactionType == com.h2.wellspend.data.TransactionType.INCOME) "+ " else ""
                val amountColor = if (expense.transactionType == com.h2.wellspend.data.TransactionType.INCOME) Color(0xFF10b981) else MaterialTheme.colorScheme.onSurface
                
                Text(
                    text = "$amountPrefix$currency${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
