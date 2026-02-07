package com.h2.wellspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.graphics.Shape
import com.h2.wellspend.ui.getGroupedItemShape
import com.h2.wellspend.ui.getGroupedItemBackgroundShape
import com.h2.wellspend.ui.theme.cardBackgroundColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.ui.getIconByName
import com.h2.wellspend.data.SystemCategory
import java.time.format.DateTimeFormatter
import java.time.LocalDate
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.toArgb
import java.util.Locale
import com.h2.wellspend.data.TimeRange

enum class GroupingMode {
    CATEGORY, ACCOUNT
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@Composable
fun ExpenseList(
    expenses: List<Expense>,
    categories: List<Category>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    budgets: List<com.h2.wellspend.data.Budget> = emptyList(),
    currency: String,
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    timeRange: TimeRange,

    onTimeRangeChange: (TimeRange) -> Unit,
    customDateRange: Pair<LocalDate, LocalDate>? = null,
    onCustomDateRangeChange: (Pair<LocalDate, LocalDate>) -> Unit = {},
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    onTransactionClick: (Expense) -> Unit = {},
    state: LazyListState = rememberLazyListState(),
    headerContent: @Composable () -> Unit = {},
    groupingMode: GroupingMode = GroupingMode.CATEGORY,
    onGroupingChange: (GroupingMode) -> Unit = {},
    startOfWeek: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY,
    expandedIds: List<String> = emptyList(),
    onToggleExpand: (String) -> Unit = {}
) {
    // Header Content Helper
    fun getAccountColor(accountId: String): Color {
        val accountColors = listOf(
            Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFFE91E63), 
            Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFF8BC34A), Color(0xFFFF5722),
            Color(0xFF795548), Color(0xFF607D8B)
        )
        val index = kotlin.math.abs(accountId.hashCode()) % accountColors.size
        return accountColors[index]
    }

    // Flatten expenses to include virtual fees
    val displayExpenses = remember(expenses) {
        expenses.flatMap { expense ->
            val list = mutableListOf<Expense>()
            // Only add base item if it is explicitly an EXPENSE
            if (expense.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE) {
                list.add(expense)
            }
            // Always add fee item if present
            if (expense.feeAmount > 0) {
                list.add(
                    expense.copy(
                        id = "fee_${expense.id}",
                        amount = expense.feeAmount,
                        category = SystemCategory.TransactionFee.name,
                        title = "Fee for ${
                            if (expense.title.isNotBlank()) expense.title else
                            when (expense.transactionType) {
                                com.h2.wellspend.data.TransactionType.INCOME -> "Income"
                                com.h2.wellspend.data.TransactionType.TRANSFER -> "Transfer"
                                else -> "Expense"
                            }
                        }",
                        feeAmount = 0.0, // Virtual item has no fee on itself
                        transactionType = com.h2.wellspend.data.TransactionType.EXPENSE // Fees are always Expenses
                    )
                )
            }
            list
        }
    }

    // Group expenses dynamically
    val groupedExpenses = remember(displayExpenses, groupingMode) {
        when (groupingMode) {
            GroupingMode.CATEGORY -> {
                displayExpenses.groupBy { it.category }
                    .mapValues { entry ->
                        val total = entry.value.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }.sumOf { it.amount }
                        val items = entry.value.sortedWith(
                            compareByDescending<Expense> { it.date.take(10) }
                                .thenByDescending { it.timestamp }
                        )
                        Pair(total, items)
                    }
                    .toList()
                    .sortedByDescending { it.second.first }
            }
            GroupingMode.ACCOUNT -> {
                expenses.groupBy { it.accountId } // Use original expenses to avoid duplicate fee items
                    .mapValues { (accountId, list) ->
                        // Calculate total expense amount
                        val totalExpense = list.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }.sumOf { it.amount }
                        val totalFees = list.sumOf { it.feeAmount }
                        val totalGroup = totalExpense + totalFees

                        // Sort regular items (Exclude non-expenses like Transfers/Incomes, they only contribute fees)
                        val sortedItems = list.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
                            .sortedWith(
                                compareByDescending<Expense> { it.date.take(10) }
                                    .thenByDescending { it.timestamp }
                            ).toMutableList()

                        // Add synthetic total fee item if needed
                        if (totalFees > 0) {
                            val feeItem = Expense(
                                id = "total_fees_${accountId ?: "unassigned"}",
                                amount = totalFees,
                                category = SystemCategory.TransactionFee.name,
                                title = "Transaction Fee",
                                date = java.time.LocalDate.now().toString(), // Helper date for display
                                timestamp = Long.MIN_VALUE, // Ensure it's at the end if sorted again, or just separate
                                note = "Total transaction fees for this account",
                                transactionType = com.h2.wellspend.data.TransactionType.EXPENSE
                            )
                            sortedItems.add(feeItem)
                        }

                        Pair(totalGroup, sortedItems)
                    }
                    .toList()
                    .sortedByDescending { it.second.first }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {


        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()), // Space for FAB + NavBar
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Selector
            item {
                DateSelector(
                    currentDate = currentDate,
                    onDateChange = onDateChange,
                    timeRange = timeRange,
                    onTimeRangeChange = onTimeRangeChange,
                    customDateRange = customDateRange,
                    onCustomDateRangeChange = onCustomDateRangeChange,
                    startOfWeek = startOfWeek
                )
            }

            // Header (Chart)
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    headerContent()
                }
            }
            
            // Grouping Toggle
            item {
                 Row(
                     modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).padding(horizontal = 16.dp),
                     horizontalArrangement = Arrangement.End,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text(
                         text = "Group by:",
                         style = MaterialTheme.typography.labelMedium,
                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.padding(end = 8.dp)
                     )
                     
                     Row(
                         modifier = Modifier
                             .clip(RoundedCornerShape(8.dp))
                             .background(MaterialTheme.colorScheme.surfaceVariant)
                             .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                     ) {
                         listOf(GroupingMode.CATEGORY, GroupingMode.ACCOUNT).forEach { mode ->
                             val isSelected = groupingMode == mode
                             val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                             val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                             
                             Box(
                                 modifier = Modifier
                                     .clickable { onGroupingChange(mode) }
                                     .background(bgColor)
                                     .padding(horizontal = 12.dp, vertical = 6.dp)
                             ) {
                                 Text(
                                     text = mode.name.lowercase().capitalize(),
                                     style = MaterialTheme.typography.labelMedium,
                                     color = textColor,
                                     fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
                                 )
                             }
                         }
                     }
                 }
            }

            if (groupedExpenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses recorded for this period.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(groupedExpenses) { (groupKey, data) ->
                    val (total, items) = data
                    
                    // DATA MAPPING
                    val displayCategory = if (groupingMode == GroupingMode.CATEGORY) {
                        val categoryName = groupKey ?: "Uncategorized" // Should not happen for category mode usually
                        categories.find { it.name == categoryName } 
                            ?: categories.find { it.name == SystemCategory.TransactionFee.name && categoryName == SystemCategory.TransactionFee.name }
                            ?: Category(name = categoryName, iconName = "Help", color = 0xFF9ca3af, isSystem = false)
                    } else {
                        // ACCOUNT MODE
                        val accountId = groupKey
                        val account = accounts.find { it.id == accountId }
                        if (account != null) {
                            Category(
                                name = account.name,
                                iconName = "Bank", // Use generic bank icon
                                color = getAccountColor(account.id).toArgb().toLong(),
                                isSystem = false
                            )
                        } else {
                            // Null Account (e.g. Cash or Unassigned)
                            Category(
                                name = "Unknown Account",
                                iconName = "Money",
                                color = 0xFF9ca3af,
                                isSystem = false
                            )
                        }
                    }
                    
                    // Budgets only apply in Category Mode
                    val displayBudget = if (groupingMode == GroupingMode.CATEGORY) {
                         budgets.find { it.category == displayCategory.name }
                    } else null

                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ExpenseCategoryItem(
                                category = displayCategory,
                                total = total,
                                items = items,
                                accounts = accounts,
                                loans = loans,
                                currency = currency,
                                budget = displayBudget,
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
                                },
                                onTransactionClick = onTransactionClick, // Pass it down
                                isExpanded = expandedIds.contains(groupKey ?: ""),
                                onToggleExpand = { onToggleExpand(groupKey ?: "") }
                            )
                        }
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
fun ExpenseCategoryItem(
    category: Category,
    total: Double,
    items: List<Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    budget: com.h2.wellspend.data.Budget? = null,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    onTransactionClick: (Expense) -> Unit,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {}
) {
    val color = Color(category.color)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackgroundColor())
    ) {
        // Header & Budget Wrapper
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(16.dp)
        ) {

            // Top Row (Icon, Name, Amount)
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = getIconByName(category.iconName),
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

            // Budget Progress Bar (Visible if budget set)
            if (budget != null) {
                val progress = (total / budget.limitAmount).toFloat().coerceIn(0f, 1f)
                val isOverBudget = total > budget.limitAmount
                val uiColor = if (isOverBudget) MaterialTheme.colorScheme.error else color

                Spacer(modifier = Modifier.height(16.dp))
                
                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = uiColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% Used",
                            style = MaterialTheme.typography.bodySmall,
                            color = uiColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Limit: $currency${String.format("%.0f", budget.limitAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
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
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 2.dp) // Add 1dp separation from header (in addition to gap color)
            ) {
                items.forEachIndexed { index, expense ->
                    // Custom shape logic: If index 0 (Top Item), force Top corners to 0dp (Flat). 
                    // Otherwise use standard grouped logic (which handles middle/bottom).
                    // Actually, getGroupedItemShape handles logic for index 0 as Top Rounded. We need to override that.
                    
                    val standardShape = getGroupedItemShape(index, items.size)
                    val shape = if (index == 0) {
                        // Keep bottom corners from standard logic (which might be rounded if size==1), force top to 0
                        // Use copy() to modify specific corners
                        standardShape.copy(
                            topStart = CornerSize(3.dp), 
                            topEnd = CornerSize(3.dp)
                        )
                    } else {
                        standardShape
                    }
                    
                    val standardBgShape = getGroupedItemBackgroundShape(index, items.size)
                    val backgroundShape = if (index == 0) {
                        standardBgShape.copy(
                            topStart = CornerSize(4.dp), 
                            topEnd = CornerSize(4.dp)
                        )
                    } else {
                        standardBgShape
                    }
                    
                    key(expense.id) {
                        Column(modifier = Modifier.padding(bottom = 2.dp)) {
                            ExpenseItem(
                                expense = expense,
                                currency = currency,
                                accounts = accounts,
                                loans = loans,
                                onDelete = onDelete,
                                onEdit = onEdit,
                                onTransactionClick = onTransactionClick,
                                shape = shape,
                                backgroundShape = backgroundShape
                            )
                        }
                    }
                }
            }
        }
}



}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(
    expense: Expense,
    currency: String,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    onTransactionClick: (Expense) -> Unit,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundShape: Shape = shape
) {
    val date = try {
        java.time.LocalDate.parse(expense.date.take(10))
    } catch (e: Exception) {
        java.time.LocalDate.now()
    }
    val formattedDate = if (expense.id.startsWith("total_fees_")) "Monthly Total" else date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

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
    val isBalanceAdjustment = expense.category == SystemCategory.BalanceAdjustment.name
    
    // Check if this is an initial loan transaction (non-editable/deletable)
    val isInitialLoanTransaction = expense.loanId != null && expense.title.startsWith("New Loan:")
    
    // Check if this is a virtual fee item
    val isFee = expense.id.startsWith("fee_")
    
    // Check if this is a total fee summary item
    val isTotalFee = expense.id.startsWith("total_fees_")
    
    // Combine flags for actions that should be disabled
    val isNonEditable = isBalanceAdjustment || isInitialLoanTransaction || isTotalFee
    val isNonDeletable = isInitialLoanTransaction || isFee || isTotalFee
    
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
                .clip(shape) // Clip to shape to fix aliasing
        ) {
            // Background (Actions)
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(backgroundShape)
            ) {
                // Left Action (Edit) - Always show, but disabled if non-editable
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(if (isNonEditable) Color.Gray else MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (isNonEditable) {
                                val msg = when {
                                    isTotalFee -> "This is a calculated summary and cannot be edited"
                                    isInitialLoanTransaction -> "Initial loan transactions cannot be edited"
                                    else -> "Balance adjustments cannot be edited"
                                }
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onEdit(expense)
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = if (isNonEditable) Color.DarkGray else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Right Action (Delete) - Always show, but disabled if initial loan transaction
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(if (isNonDeletable) Color.Gray else MaterialTheme.colorScheme.error)
                        .clickable {
                            if (isNonDeletable) {
                                val msg = when {
                                    isTotalFee -> "This is a calculated summary and cannot be deleted"
                                    isFee -> "Transaction fees cannot be deleted directly"
                                    else -> "Initial loan transactions cannot be deleted"
                                }
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                showDeleteDialog = true
                            }
                        },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (isNonDeletable) Color.DarkGray else MaterialTheme.colorScheme.onError
                        )
                    }
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
                                // Always allow swipe to reveal disabled buttons
                                val minOffset = -actionWidthPx
                                val maxOffset = actionWidthPx
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
                    .background(cardBackgroundColor(), shape) // Opaque background to hide actions with shape
                    .clip(shape) // Explicit clip for foreground
                    .combinedClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { 
                            if (!isFee && !isTotalFee) {
                                onTransactionClick(expense)
                            }
                        },
                        onLongClick = { 
                            if (!isFee && !isTotalFee) {
                                onTransactionClick(expense) 
                            }
                        }
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Details
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = if (isFee) "Fee for ${expense.title.removePrefix("Fee for ")}" else expense.title.ifEmpty { "No title" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedDate + extraInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Right Side: Amount
                Text(
                    text = "$currency${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
