package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.Loan
import com.h2.wellspend.data.SystemCategory
import com.h2.wellspend.data.TransactionType
import com.h2.wellspend.ui.getIconByName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionPreviewScreen(
    transaction: Expense,
    accounts: List<Account>,
    loans: List<Loan>,
    categories: List<Category>,
    currency: String,
    onEdit: (Expense) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    val categoryObj = categories.find { it.name == transaction.category }
    val isIncome = transaction.transactionType == TransactionType.INCOME
    val isTransfer = transaction.transactionType == TransactionType.TRANSFER
    val isExpense = transaction.transactionType == TransactionType.EXPENSE
    
    // Non-editable logic
    val isBalanceAdjustment = transaction.category == SystemCategory.BalanceAdjustment.name
    val isInitialLoanTransaction = transaction.loanId != null && transaction.title.startsWith("New Loan:")
    val isNonEditable = isBalanceAdjustment || isInitialLoanTransaction
    
    val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown Account"
    val targetAccountName = accounts.find { it.id == transaction.transferTargetAccountId }?.name
    val loanName = if (transaction.loanId != null) loans.find { it.id == transaction.loanId }?.name else null
    
    val dateStr = try {
        val date = LocalDate.parse(transaction.date.take(10))
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    } catch (e: Exception) { transaction.date }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Content Area (Scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            val iconVec = when {
                isTransfer -> Icons.AutoMirrored.Filled.CompareArrows
                isIncome -> Icons.Default.AttachMoney
                else -> getIconByName(categoryObj?.iconName ?: "Help")
            }
            val iconColor = when {
                isTransfer -> Color(0xFF2196F3)
                isIncome -> Color(0xFF4CAF50)
                else -> if (categoryObj != null) Color(categoryObj.color) else MaterialTheme.colorScheme.primary
            }
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVec,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Amount
            val amountPrefix = if (isIncome) "+" else if (isExpense) "-" else ""
            Text(
                text = "$amountPrefix$currency${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF4CAF50) else if (isTransfer) Color(0xFF2196F3) else Color(0xFFef4444)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            if (transaction.title.isNotEmpty()) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Details List
            DetailRow(icon = Icons.Default.CalendarToday, label = "Date", value = dateStr)
            
            val createdAtStr = remember(transaction.timestamp) {
                try {
                    java.time.Instant.ofEpochMilli(transaction.timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm a"))
                } catch (e: Exception) {
                    ""
                }
            }
            
            if (createdAtStr.isNotEmpty()) {
                 DetailRow(icon = androidx.compose.material.icons.Icons.Default.AccessTime, label = "Created At", value = createdAtStr)
            }
            DetailRow(icon = Icons.Default.Category, label = "Category", value = transaction.category)
            
            if (isTransfer && targetAccountName != null) {
                DetailRow(icon = Icons.Default.AccountBalanceWallet, label = "From Account", value = accountName)
                DetailRow(icon = Icons.Default.AccountBalanceWallet, label = "To Account", value = targetAccountName)
            } else {
                 DetailRow(icon = Icons.Default.AccountBalanceWallet, label = "Account", value = accountName)
            }
            
           if (loanName != null) {
                DetailRow(icon = Icons.Default.AttachMoney, label = "Loan", value = loanName)
            }
            
            if (!transaction.note.isNullOrEmpty()) {
                DetailRow(icon = Icons.Default.Notes, label = "Note", value = transaction.note!!)
            }
            
            if (transaction.feeAmount > 0) {
                 DetailRow(
                     icon = Icons.Default.AttachMoney, 
                     label = "Fee", 
                     value = "$currency${String.format("%.2f", transaction.feeAmount)} ${if(transaction.feeConfigName != null) "(${transaction.feeConfigName})" else ""}"
                 )
            }
        }

        // Bottom Actions Area
        Column(modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp)) {
            if (isNonEditable) {
                if (isInitialLoanTransaction) {
                    Text(
                        text = "Initial loan transactions cannot be edited or deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (isBalanceAdjustment) {
                    Text(
                        text = "Balance adjustments cannot be edited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Edit Button
                Button(
                    onClick = { onEdit(transaction) },
                    modifier = Modifier.weight(1f),
                    enabled = !isNonEditable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
                
                // Delete Button
                var showDeleteDialog by remember { mutableStateOf(false) }
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isInitialLoanTransaction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Transaction") },
                        text = { Text("Are you sure you want to delete this transaction?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    onDelete(transaction.id)
                                    onDismiss() // Close preview after delete
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
            }
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
