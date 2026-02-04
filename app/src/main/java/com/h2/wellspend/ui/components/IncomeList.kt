package com.h2.wellspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import com.h2.wellspend.ui.getIconByName
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Expense
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import androidx.compose.animation.core.Animatable
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.TimeRange

import com.h2.wellspend.data.SystemCategory
import androidx.compose.ui.graphics.Shape
import com.h2.wellspend.ui.getGroupedItemShape
import com.h2.wellspend.ui.getGroupedItemBackgroundShape
import com.h2.wellspend.ui.theme.cardBackgroundColor

@Composable
fun IncomeList(
    incomes: List<Expense>,
    accounts: List<Account>,
    loans: List<com.h2.wellspend.data.Loan>,
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
    headerContent: @Composable () -> Unit = {},
    useGrouping: Boolean = true,
    startOfWeek: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY
) {
    // Group incomes by account
    val groupedIncomes = remember(incomes, useGrouping) {
        if (useGrouping) {
            incomes.groupBy { it.accountId }
                .mapValues { entry ->
                    val total = entry.value.sumOf { it.amount }
                    val items = entry.value.sortedWith(
                        compareByDescending<Expense> { it.date.take(10) }
                            .thenByDescending { it.timestamp }
                    )
                    Pair(total, items)
                }
                .toList()
                .sortedByDescending { it.second.first } // Sort by total amount
        } else {
            emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(if (useGrouping) 12.dp else 0.dp)
        ) {
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

            // Header (Chart or Total)
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    headerContent()
                }
            }

            if (incomes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No income recorded for this period.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (useGrouping) {
                    items(groupedIncomes) { (accountId, data) ->
                        val (total, items) = data
                        val account = accounts.find { it.id == accountId }
                        
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            IncomeAccountItem(
                                account = account,
                                total = total,
                                items = items,
                                loans = loans,
                                currency = currency,
                                onDelete = onDelete,
                                onEdit = onEdit,
                                onTransactionClick = onTransactionClick
                            )
                        }
                    }
                } else {
                    // Flat List
                     itemsIndexed(incomes) { index, income ->
                        val shape = getGroupedItemShape(index, incomes.size)
                        val backgroundShape = getGroupedItemBackgroundShape(index, incomes.size)
                        val account = accounts.find { it.id == income.accountId }
                        
                        Box(modifier = Modifier.padding(vertical = 1.dp).padding(horizontal = 16.dp)) {
                            IncomeItem(
                                income = income,
                                loans = loans,
                                currency = currency,
                                onEdit = onEdit,
                                onDelete = onDelete,
                                onTransactionClick = onTransactionClick,
                                showIcon = true,
                                account = account,
                                shape = shape,
                                backgroundShape = backgroundShape
                            )
                        }
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
                            "Swipe left/right to edit or delete.",
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
fun IncomeAccountItem(
    account: Account?,
    total: Double,
    items: List<Expense>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    onTransactionClick: (Expense) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Generate a consistent color for the account
    val accountColors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFFE91E63), 
        Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFF8BC34A), Color(0xFFFF5722),
        Color(0xFF795548), Color(0xFF607D8B)
    )
    val color = if (account != null) {
        val colorIndex = kotlin.math.abs(account.id.hashCode()) % accountColors.size
        accountColors[colorIndex]
    } else {
        Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackgroundColor())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
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
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = account?.name ?: "Unknown Account",
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
                        text = "+$currency${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF10b981), // Green for Income
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
                    .padding(top = 2.dp)
            ) {
                items.forEachIndexed { index, income ->
                    val standardShape = getGroupedItemShape(index, items.size)
                    val shape = if (index == 0) {
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
                    
                    key(income.id) {
                        Box(modifier = Modifier.padding(bottom = 2.dp)) {
                            IncomeItem(
                                income = income,
                                loans = loans,
                                currency = currency,
                                onEdit = onEdit,
                                onDelete = onDelete,
                                onTransactionClick = onTransactionClick,
                                showIcon = false,
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
fun IncomeItem(
    income: Expense,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onEdit: (Expense) -> Unit,
    onDelete: (String) -> Unit,
    onTransactionClick: (Expense) -> Unit,
    showIcon: Boolean = false,
    account: Account? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundShape: Shape = shape
) {
    val date = try {
        LocalDate.parse(income.date.take(10))
    } catch (e: Exception) {
        LocalDate.now()
    }
    
    val loanName = if (income.loanId != null) loans.find { it.id == income.loanId }?.name else null
    val extraInfo = buildString {
        if (loanName != null) append(" • Loan: $loanName")
        if (account != null) append(" • To: ${account.name}")
    }
    
    val formattedDate = date.format(DateTimeFormatter.ofPattern("EEE, MMM d")) + extraInfo

    // Check if this is a balance adjustment (non-editable)
    val isBalanceAdjustment = income.category == SystemCategory.BalanceAdjustment.name

    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Delete states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    var deleteConfirmed by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Income") },
            text = { Text("Are you sure you want to delete this income record?") },
            confirmButton = {
                TextButton(
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
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(isVisible) {
        if (!isVisible && deleteConfirmed) {
            delay(300) 
            onDelete(income.id)
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
                .height(IntrinsicSize.Min)
                .clip(shape) 
        ) {
            // Background (Actions)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(cardBackgroundColor())
                    .clip(backgroundShape)
            ) {
                // Non-editable constraints
                val isInitialLoanTransaction = income.loanId != null && income.title.startsWith("New Loan:")
                val isNonEditable = isBalanceAdjustment || isInitialLoanTransaction

                // Left Action (Edit)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(if (isNonEditable) Color.Gray else MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (isNonEditable) {
                                val msg = if (isInitialLoanTransaction) "Initial loan transactions cannot be edited" else "Balance adjustments cannot be edited"
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onEdit(income)
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

                // Right Action (Delete)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(if (isInitialLoanTransaction) Color.Gray else MaterialTheme.colorScheme.error)
                        .clickable {
                             if (isInitialLoanTransaction) {
                                android.widget.Toast.makeText(context, "Initial loan transactions cannot be deleted", android.widget.Toast.LENGTH_SHORT).show()
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
                            tint = if (isInitialLoanTransaction) Color.DarkGray else MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            // Foreground (Content)
            Box(
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
                             val targetOffset = if (offsetX.value > actionWidthPx / 2) actionWidthPx else if (offsetX.value < -actionWidthPx / 2) -actionWidthPx else 0f
                             scope.launch { offsetX.animateTo(targetOffset) }
                        }
                    )
                    .fillMaxWidth()
                    .background(cardBackgroundColor(), shape) // Opaque background
                    .combinedClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { onTransactionClick(income) },
                        onLongClick = { onTransactionClick(income) }
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                         // Icon
                        if (showIcon) {
                             // Original Icon Logic
                            val iconVector = if (isBalanceAdjustment || income.category == SystemCategory.Loan.name) {
                                 getIconByName(income.category)
                            } else {
                                Icons.Default.AttachMoney
                            }
                            
                            val iconTint = if (isBalanceAdjustment) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF4CAF50) // Green
                            
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(iconTint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Column {
                            Text(
                                text = income.title.ifEmpty { "No title" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        text = "+ $currency${String.format("%.2f", income.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF10b981), // Green
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
