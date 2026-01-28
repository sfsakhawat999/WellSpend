package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.Loan
import com.h2.wellspend.ui.getGroupedItemBackgroundShape
import com.h2.wellspend.ui.getGroupedItemShape

import androidx.compose.ui.graphics.Color

@Composable
fun SearchScreen(
    searchResults: List<Expense>,
    accounts: List<Account>,
    loans: List<Loan>,
    categories: List<Category>,
    currency: String,
    onEdit: (Expense) -> Unit,
    onDelete: (String) -> Unit,
    onTransactionClick: (Expense) -> Unit = {}
) {
    if (searchResults.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No results found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        // Calculate Totals
        val totalIncome = searchResults
            .filter { it.transactionType == com.h2.wellspend.data.TransactionType.INCOME }
            .sumOf { it.amount - it.feeAmount }

        val totalExpense = searchResults
            .filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
            .sumOf { it.amount } + searchResults.sumOf { it.feeAmount }

        val totalTransfer = searchResults
            .filter { it.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER }
            .sumOf { it.amount }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), // Match Dashboard padding
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp) // Grouped items have no space between them
        ) {
            // Summary Section
            if (totalIncome > 0 || totalExpense > 0 || totalTransfer > 0) {
                item {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Results count - centered at top
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${searchResults.size} Results",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // Income/Expense/Transfer summary
                            if (totalIncome > 0 || totalExpense > 0 || totalTransfer > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    if (totalIncome > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Income",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$currency${"%.2f".format(totalIncome)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF10b981)
                                            )
                                        }
                                    }
                                    if (totalExpense > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Expense",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$currency${"%.2f".format(totalExpense)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    if (totalTransfer > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Transfer",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$currency${"%.2f".format(totalTransfer)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            itemsIndexed(searchResults) { index, transaction ->
                // Shape Logic from DashboardScreen/MainScreen (Preserved)
                val shape = when {
                    searchResults.size == 1 -> RoundedCornerShape(16.dp)
                    index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                    index == searchResults.lastIndex -> RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    else -> RoundedCornerShape(3.dp)
                }

                val backgroundShape = when {
                    searchResults.size == 1 -> RoundedCornerShape(17.dp)
                    index == 0 -> RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    index == searchResults.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 17.dp, bottomEnd = 17.dp)
                    else -> RoundedCornerShape(4.dp)
                }

                val paddingModifier = Modifier.padding(vertical = 1.dp)

                TransactionItem(
                    transaction = transaction,
                    accounts = accounts,
                    loans = loans,
                    categories = categories,
                    currency = currency,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onTransactionClick = onTransactionClick,
                    modifier = paddingModifier,
                    shape = shape,
                    backgroundShape = backgroundShape
                )
            }
        }
    }
}
