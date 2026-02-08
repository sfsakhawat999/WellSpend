package com.h2.wellspend.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.h2.wellspend.data.RecurringConfig
import com.h2.wellspend.data.TransactionType
import com.h2.wellspend.data.Account
import com.h2.wellspend.ui.getIconByName
import com.h2.wellspend.ui.theme.cardBackgroundColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.sp

@Composable
fun RecurringConfigScreen(
    configs: List<RecurringConfig>,
    accounts: List<Account>,
    categories: List<com.h2.wellspend.data.Category>,
    currency: String,
    onEdit: (RecurringConfig) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deletingConfigId by remember { mutableStateOf<String?>(null) }

    // Group configs by transaction type
    val expenseConfigs = configs.filter { it.transactionType == TransactionType.EXPENSE }
    val incomeConfigs = configs.filter { it.transactionType == TransactionType.INCOME }
    val transferConfigs = configs.filter { it.transactionType == TransactionType.TRANSFER }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring Config")
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = paddingValues.calculateTopPadding()) 
                // We intentionally ignore bottom padding here so the LazyColumn can scroll behind the nav bar
        ) {
            if (configs.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No recurring transactions",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Add a recurring transaction to see it here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp // Extra for FAB
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 16.dp + bottomPadding
                    )
                ) {
                    // Expense Configs
                    if (expenseConfigs.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                SectionHeader("EXPENSES")
                            }
                        }
                        itemsIndexed(expenseConfigs, key = { _, config -> config.id }) { index, config ->
                            val shape = when {
                                expenseConfigs.size == 1 -> RoundedCornerShape(16.dp)
                                index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                                index == expenseConfigs.lastIndex -> RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                else -> RoundedCornerShape(3.dp)
                            }
                            
                            val backgroundShape = when {
                                expenseConfigs.size == 1 -> RoundedCornerShape(17.dp)
                                index == 0 -> RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                index == expenseConfigs.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 17.dp, bottomEnd = 17.dp)
                                else -> RoundedCornerShape(4.dp)
                            }

                            RecurringTransactionItem(
                                config = config,
                                accounts = accounts,
                                categories = categories,
                                currency = currency,
                                onEdit = { onEdit(config) },
                                onDelete = {
                                    deletingConfigId = config.id
                                    showDeleteConfirmation = true
                                },
                                shape = shape,
                                backgroundShape = backgroundShape,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Income Configs
                    if (incomeConfigs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                SectionHeader("INCOME")
                            }
                        }
                        itemsIndexed(incomeConfigs, key = { _, config -> config.id }) { index, config ->
                            val shape = when {
                                incomeConfigs.size == 1 -> RoundedCornerShape(16.dp)
                                index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                                index == incomeConfigs.lastIndex -> RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                else -> RoundedCornerShape(3.dp)
                            }
                            
                            val backgroundShape = when {
                                incomeConfigs.size == 1 -> RoundedCornerShape(17.dp)
                                index == 0 -> RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                index == incomeConfigs.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 17.dp, bottomEnd = 17.dp)
                                else -> RoundedCornerShape(4.dp)
                            }

                            RecurringTransactionItem(
                                config = config,
                                accounts = accounts,
                                categories = categories,
                                currency = currency,
                                onEdit = { onEdit(config) },
                                onDelete = {
                                    deletingConfigId = config.id
                                    showDeleteConfirmation = true
                                },
                                shape = shape,
                                backgroundShape = backgroundShape,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Transfer Configs
                    if (transferConfigs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                SectionHeader("TRANSFERS")
                            }
                        }
                        itemsIndexed(transferConfigs, key = { _, config -> config.id }) { index, config ->
                            val shape = when {
                                transferConfigs.size == 1 -> RoundedCornerShape(16.dp)
                                index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                                index == transferConfigs.lastIndex -> RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                else -> RoundedCornerShape(3.dp)
                            }
                            
                            val backgroundShape = when {
                                transferConfigs.size == 1 -> RoundedCornerShape(17.dp)
                                index == 0 -> RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                index == transferConfigs.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 17.dp, bottomEnd = 17.dp)
                                else -> RoundedCornerShape(4.dp)
                            }

                            RecurringTransactionItem(
                                config = config,
                                accounts = accounts,
                                categories = categories,
                                currency = currency,
                                onEdit = { onEdit(config) },
                                onDelete = {
                                    deletingConfigId = config.id
                                    showDeleteConfirmation = true
                                },
                                shape = shape,
                                backgroundShape = backgroundShape,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && deletingConfigId != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                deletingConfigId = null
            },
            title = { Text("Delete Recurring Transaction") },
            text = { Text("Are you sure you want to delete this recurring transaction? This will not affect existing transactions.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(deletingConfigId!!)
                        showDeleteConfirmation = false
                        deletingConfigId = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirmation = false
                    deletingConfigId = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecurringTransactionItem(
    config: RecurringConfig,
    accounts: List<Account>,
    categories: List<com.h2.wellspend.data.Category>,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundShape: Shape = shape
) {
    val sourceAccountName = accounts.find { it.id == config.accountId }?.name ?: "No Account"
    val targetAccountName = accounts.find { it.id == config.transferTargetAccountId }?.name
    
    val isIncome = config.transactionType == TransactionType.INCOME
    val isTransfer = config.transactionType == TransactionType.TRANSFER
    
    // Display text for description
    val displayDesc = when {
        isTransfer && targetAccountName != null -> "Transfer: $sourceAccountName → $targetAccountName"
        isTransfer -> "Transfer: $sourceAccountName → ?"
        config.title.isNotEmpty() -> {
            val prefix = if (isIncome) "Income" else config.category
            "$prefix: ${config.title}"
        }
        else -> if (isIncome) "Income" else config.category
    }
    
    val nextDue = try {
        LocalDate.parse(config.nextDueDate.take(10))
            .format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (e: Exception) {
        config.nextDueDate
    }
    
    val frequencyStr = config.frequency.name.lowercase().replaceFirstChar { it.uppercase() }
    
    // Color coding
    val amountColor = when {
        isIncome -> Color(0xFF4CAF50) // Green
        isTransfer -> Color(0xFF2196F3) // Blue
        else -> Color(0xFFF44336) // Red
    }
    val amountPrefix = when {
        isIncome -> "+"
        isTransfer -> "↔"
        else -> "-"
    }
    
    // Swipe gesture state
    val density = androidx.compose.ui.platform.LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val actionWidth = 70.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
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
                    .clickable { 
                        scope.launch { offsetX.animateTo(0f) }
                        onEdit() 
                    },
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
                    .clickable { 
                        scope.launch { offsetX.animateTo(0f) }
                        onDelete() 
                    },
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
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
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
                    onClick = { 
                        scope.launch {
                            com.h2.wellspend.ui.performWiggle(offsetX, actionWidthPx, context)
                        }
                    },
                    onLongClick = {
                        scope.launch {
                            com.h2.wellspend.ui.performWiggle(offsetX, actionWidthPx, context)
                        }
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val categoryObj = categories.find { it.name == config.category }
            val transactionIcon = when {
                isIncome -> Icons.Default.AttachMoney
                isTransfer -> Icons.AutoMirrored.Filled.CompareArrows
                else -> getIconByName(categoryObj?.iconName ?: config.category)
            }
            
            val iconTint = when {
                isIncome -> Color(0xFF4CAF50)
                isTransfer -> Color(0xFF2196F3)
                else -> {
                    if (categoryObj != null) Color(categoryObj.color) else MaterialTheme.colorScheme.primary
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transactionIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge for Frequency
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                         Text(
                            frequencyStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isTransfer) {
                        Text(
                            sourceAccountName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "• Next: $nextDue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "$amountPrefix$currency${String.format("%.2f", config.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
