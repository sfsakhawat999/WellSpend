package com.h2.wellspend.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.SystemCategory
import com.h2.wellspend.data.TransactionType
import com.h2.wellspend.ui.getIconByName
import com.h2.wellspend.ui.performWiggle
import com.h2.wellspend.ui.theme.cardBackgroundColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Expense,
    accounts: List<Account>,
    loans: List<Loan>,
    categories: List<Category>,
    currency: String,
    onEdit: (Expense) -> Unit,
    onDelete: (String) -> Unit,
    onTransactionClick: (Expense) -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundShape: Shape = shape
) {
    val sourceAccountName = accounts.find { it.id == transaction.accountId }?.name ?: "No Account"
    val targetAccountName = accounts.find { it.id == transaction.transferTargetAccountId }?.name
    val loanName = if (transaction.loanId != null) {
        loans.find { it.id == transaction.loanId }?.name
    } else null
    
    val isIncome = transaction.transactionType == TransactionType.INCOME
    val isTransfer = transaction.transactionType == TransactionType.TRANSFER
    
    // Check if this is a balance adjustment (non-editable)
    val isBalanceAdjustment = transaction.category == SystemCategory.BalanceAdjustment.name
    
    // Check if this is an initial loan transaction (non-editable/deletable)
    // These are the automatic transactions created when a loan is created
    val isInitialLoanTransaction = transaction.loanId != null && transaction.title.startsWith("New Loan:")
    
    // Combine flags for actions that should be disabled
    val isNonEditable = isBalanceAdjustment || isInitialLoanTransaction
    
    // Display text for description: "Category: Description" format
    val displayDesc = when {
        isTransfer && targetAccountName != null -> "Transfer: $sourceAccountName → $targetAccountName"
        isTransfer -> "Transfer: $sourceAccountName → ?"
        loanName != null -> transaction.title.ifEmpty { transaction.category }
        isBalanceAdjustment -> transaction.title.ifEmpty { transaction.category }
        transaction.title.isNotEmpty() -> "${transaction.category}: ${transaction.title}"
        else -> transaction.category
    }
    
    val dateStr = try {
        val date = LocalDate.parse(transaction.date.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (e: Exception) { "" }
    
    // Color coding: Green for income, Red for expense, Blue for transfer
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
    val actionWidth = 70.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(transaction.id)
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
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape) // Clip the outer box to the shape so actions also respect corners
    ) {
        // Background Actions
        val context = androidx.compose.ui.platform.LocalContext.current
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(cardBackgroundColor())
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
                            val msg = if (isInitialLoanTransaction) "Initial loan transactions cannot be edited" else "Balance adjustments cannot be edited"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onEdit(transaction)
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
        
        // Foreground Content (Swipeable)
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
                .background(cardBackgroundColor(), shape) // Apply shape to foreground
                // .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) // Removed border as requested
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null, // Custom ripple handled by material? or just disable visual feedback here to avoid conflict
                    onClick = { onTransactionClick(transaction) },
                    onLongClick = { onTransactionClick(transaction) }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction Icon
            val categoryObj = categories.find { it.name == transaction.category }
            val transactionIcon = when {
                isIncome -> Icons.Default.AttachMoney
                isTransfer -> Icons.AutoMirrored.Filled.CompareArrows
                else -> getIconByName(categoryObj?.iconName ?: transaction.category)
            }
            
            val iconTint = when {
                isIncome -> Color(0xFF4CAF50)
                isTransfer -> Color(0xFF2196F3)
                isBalanceAdjustment -> MaterialTheme.colorScheme.onSurfaceVariant
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
                    if (!isTransfer) {
                        Text(
                            sourceAccountName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "$amountPrefix$currency${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
