package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.ui.getIconByName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import com.h2.wellspend.ui.theme.cardBackgroundColor
import com.h2.wellspend.data.TimeRange
import com.h2.wellspend.utils.DateUtils

import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReport(
    expenses: List<Expense>,
    categories: List<Category>,
    currency: String,
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    timeRange: TimeRange,
    onTimeRangeChange: (TimeRange) -> Unit,
    showCompareDialog: Boolean,
    onDismissCompareDialog: () -> Unit,
    comparisonDate: LocalDate?,
    onComparisonDateChange: (LocalDate) -> Unit,
    excludeLoanTransactions: Boolean,
    startOfWeek: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY,
    customDateRange: Pair<LocalDate, LocalDate>? = null,
    onCustomDateRangeChange: (Pair<LocalDate, LocalDate>) -> Unit = {},
    reportComparisonCustomRange: Pair<LocalDate, LocalDate>? = null,
    onReportComparisonCustomRangeChange: (Pair<LocalDate, LocalDate>) -> Unit = {}
) {
    // 1. Filter by Date first
    val (incomeTotal, expenseTotal, transferTotal, _, expensePercentChange, categoryData, breakdownData) = remember(expenses, currentDate, comparisonDate, excludeLoanTransactions, timeRange, customDateRange, reportComparisonCustomRange) {
        val validExpenses = expenses
        

        val currentTransactions = DateUtils.filterByTimeRange(validExpenses, currentDate, timeRange, startOfWeek, customDateRange)
        
        // Calculate Comparison Range
        val prevTransactions = if (timeRange == TimeRange.CUSTOM) {
             val effectivePrevRange = reportComparisonCustomRange ?: run {
                 if (customDateRange != null) {
                     val duration = java.time.Period.between(customDateRange.first, customDateRange.second).days + 1
                     val prevEnd = customDateRange.first.minusDays(1)
                     val prevStart = prevEnd.minusDays(duration.toLong() - 1)
                     prevStart to prevEnd
                 } else null
             }
             DateUtils.filterByTimeRange(validExpenses, currentDate, timeRange, startOfWeek, effectivePrevRange)
        } else {
             // Use comparisonDate if set, otherwise default to previous period
             val prevDate = comparisonDate ?: DateUtils.adjustDate(currentDate, timeRange, false, customDateRange)
             DateUtils.filterByTimeRange(validExpenses, prevDate, timeRange, startOfWeek)
        }

        // Apply Exclusion Pref for SUMMARIES and CHARTS
        val filteredCurrentTransactions = if (excludeLoanTransactions) {
            currentTransactions.filter { it.loanId == null }
        } else currentTransactions
        
        val filteredPrevTransactions = if (excludeLoanTransactions) {
            prevTransactions.filter { it.loanId == null }
        } else prevTransactions

        // 2. Separate by Type for Current Period (Filtered)
        val currentIncomes = filteredCurrentTransactions.filter { it.transactionType == com.h2.wellspend.data.TransactionType.INCOME }
        val currentExpenses = filteredCurrentTransactions.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
        val currentTransfers = filteredCurrentTransactions.filter { it.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER }

        // Totals (Filtered)
        val incTotal = currentIncomes.sumOf { it.amount }
        // Expense Total = Expense Amount + ALL Fees (Expense Fees + Transfer Fees + Income Fees)
        val allFees = filteredCurrentTransactions.sumOf { it.feeAmount }
        val expTotal = currentExpenses.sumOf { it.amount } + allFees 
        val trTotal = currentTransfers.sumOf { it.amount }

        // 3. Comparison specific to EXPENSES (Base Amount + Fees) (Filtered)
        val prevExpenses = filteredPrevTransactions.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
        val prevAllFees = filteredPrevTransactions.sumOf { it.feeAmount }
        val prevExpTotal = prevExpenses.sumOf { it.amount } + prevAllFees
        
        val diff = expTotal - prevExpTotal
        val expChange = if (prevExpTotal == 0.0) 100.0 else (diff / prevExpTotal) * 100.0

        // 4. Category Data for CHART (Filtered)
        val currentCatMap = currentExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } } 
            .toMutableMap()
            
        val allTransactionFees = filteredCurrentTransactions.sumOf { it.feeAmount }
        if (allTransactionFees > 0) {
            val feeCat = com.h2.wellspend.data.SystemCategory.TransactionFee.name
            currentCatMap[feeCat] = (currentCatMap[feeCat] ?: 0.0) + allTransactionFees
        }

        val prevCatMap = prevExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .toMutableMap()

        if (prevAllFees > 0) {
            val feeCat = com.h2.wellspend.data.SystemCategory.TransactionFee.name
            prevCatMap[feeCat] = (prevCatMap[feeCat] ?: 0.0) + prevAllFees
        }

        val catData = currentCatMap.map { (catName, amount) ->
             val prevAmount = prevCatMap[catName] ?: 0.0
             val category = categories.find { it.name == catName } ?: Category(
                name = catName,
                iconName = catName,
                color = com.h2.wellspend.ui.getSystemCategoryColor(com.h2.wellspend.data.SystemCategory.Others).toArgb().toLong().also {
                     try { com.h2.wellspend.ui.getSystemCategoryColor(com.h2.wellspend.data.SystemCategory.valueOf(catName)).toArgb().toLong() } catch(e:Exception) {}
                },
                isSystem = false
            )
            CategoryData(category, amount, prevAmount)
        }.sortedByDescending { it.amount }
        
        // 5. Category Data for BREAKDOWN (Unfiltered - Show Loans)
        // We calculate this separately so the list shows everything
        val breakdownCurrentExpenses = currentTransactions.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
        val breakdownPrevExpenses = prevTransactions.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
        
        val breakdownCatMap = breakdownCurrentExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .toMutableMap()
            
        val breakdownAllFees = currentTransactions.sumOf { it.feeAmount }
        if (breakdownAllFees > 0) {
            val feeCat = com.h2.wellspend.data.SystemCategory.TransactionFee.name
            breakdownCatMap[feeCat] = (breakdownCatMap[feeCat] ?: 0.0) + breakdownAllFees
        }
        
        val breakdownPrevCatMap = breakdownPrevExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .toMutableMap()
            
        if (prevTransactions.sumOf { it.feeAmount } > 0) {
             val feeCat = com.h2.wellspend.data.SystemCategory.TransactionFee.name
             breakdownPrevCatMap[feeCat] = (breakdownPrevCatMap[feeCat] ?: 0.0) + prevTransactions.sumOf { it.feeAmount }
        }
        
        val breakdownData = breakdownCatMap.map { (catName, amount) ->
             val prevAmount = breakdownPrevCatMap[catName] ?: 0.0
             val category = categories.find { it.name == catName } ?: Category(
                name = catName,
                iconName = catName,
                color = com.h2.wellspend.ui.getSystemCategoryColor(com.h2.wellspend.data.SystemCategory.Others).toArgb().toLong().also {
                     try { com.h2.wellspend.ui.getSystemCategoryColor(com.h2.wellspend.data.SystemCategory.valueOf(catName)).toArgb().toLong() } catch(e:Exception) {}
                },
                isSystem = false
            )
            CategoryData(category, amount, prevAmount)
        }.sortedByDescending { it.amount }

        ReportData(incTotal, expTotal, trTotal, prevExpTotal, expChange, catData, breakdownData)
    }



    if (showCompareDialog) {
        when (timeRange) {
            TimeRange.CUSTOM -> {
                // Calculate default previous period (same as shown in summary)
                val defaultPrevRange = reportComparisonCustomRange ?: customDateRange?.let { 
                    val duration = java.time.Period.between(it.first, it.second).days + 1
                    val prevEnd = it.first.minusDays(1)
                    val prevStart = prevEnd.minusDays(duration.toLong() - 1)
                    prevStart to prevEnd
                }
                CustomRangePickerDialog(
                    initialDate = defaultPrevRange?.first ?: currentDate,
                    initialRange = defaultPrevRange,
                    onRangeSelected = { 
                        onReportComparisonCustomRangeChange(it)
                        onDismissCompareDialog()
                    },
                    onDismiss = onDismissCompareDialog
                )
            }
            TimeRange.DAILY -> {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = (comparisonDate ?: currentDate).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = onDismissCompareDialog,
                    confirmButton = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        val selectedDate = selectedMillis?.let { 
                            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate() 
                        }
                        val isDisabled = selectedDate != null && selectedDate.isEqual(currentDate)
                        TextButton(
                            onClick = {
                                selectedDate?.let { onComparisonDateChange(it) }
                                onDismissCompareDialog()
                            },
                            enabled = !isDisabled
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissCompareDialog) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            TimeRange.WEEKLY -> {
                WeekPickerDialog(
                    currentDate = comparisonDate ?: DateUtils.adjustDate(currentDate, timeRange, false),
                    onDateSelected = { 
                        onComparisonDateChange(it)
                        onDismissCompareDialog() 
                    },
                    onDismiss = onDismissCompareDialog,
                    startOfWeek = startOfWeek,
                    disabledDate = currentDate
                )
            }
            TimeRange.MONTHLY -> {
                MonthPickerDialog(
                    currentDate = comparisonDate ?: DateUtils.adjustDate(currentDate, timeRange, false),
                    onDateSelected = { 
                        onComparisonDateChange(it)
                        onDismissCompareDialog() 
                    },
                    onDismiss = onDismissCompareDialog,
                    disabledDate = currentDate
                )
            }
            TimeRange.YEARLY -> {
                YearPickerDialog(
                    currentDate = comparisonDate ?: DateUtils.adjustDate(currentDate, timeRange, false),
                    onDateSelected = { 
                        onComparisonDateChange(it)
                        onDismissCompareDialog() 
                    },
                    onDismiss = onDismissCompareDialog,
                    disabledDate = currentDate
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Date Switcher

        DateSelector(
            currentDate = currentDate,
            onDateChange = onDateChange,
            timeRange = timeRange,
            onTimeRangeChange = onTimeRangeChange,
            customDateRange = customDateRange,
            onCustomDateRangeChange = onCustomDateRangeChange,
            startOfWeek = startOfWeek
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Summary Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBackgroundColor(), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = DateUtils.formatDateForRange(currentDate, timeRange, startOfWeek, customDateRange).uppercase() + 
                           if (excludeLoanTransactions) " (LOANS EXCLUDED)" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Total Spending",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "$currency${String.format("%.2f", expenseTotal)}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (expensePercentChange > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (expensePercentChange > 0) Icons.Default.ArrowOutward else Icons.AutoMirrored.Filled.CallReceived, 
                        contentDescription = null,
                        tint = if (expensePercentChange > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.1f", abs(expensePercentChange))}% vs ${
                            if (timeRange == TimeRange.CUSTOM) {
                                DateUtils.formatDateForRange(currentDate, timeRange, startOfWeek, reportComparisonCustomRange ?: customDateRange?.let { 
                                    val duration = java.time.Period.between(it.first, it.second).days + 1
                                    val prevEnd = it.first.minusDays(1)
                                    val prevStart = prevEnd.minusDays(duration.toLong() - 1)
                                    prevStart to prevEnd
                                })
                            } else {
                                DateUtils.formatDateForRange((comparisonDate ?: DateUtils.adjustDate(currentDate, timeRange, false)), timeRange, startOfWeek)
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (expensePercentChange > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Income and Transfer Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+$currency${String.format("%.2f", incomeTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF10b981), // Green
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Transfers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currency${String.format("%.2f", transferTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF8b5cf6), // Violet
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Donut Chart (integrated into summary card)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (categoryData.isEmpty()) {
                     Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val chartData = categoryData.map { data ->
                        ChartData(
                            name = data.category.name,
                            value = data.amount,
                            color = Color(data.category.color)
                        )
                    }
                    DonutChart(
                        data = chartData,
                        totalAmount = expenseTotal,
                        currency = currency,
                        centerLabel = "Total Spending"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Breakdown List
            Text(
                text = "Breakdown",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdownData.forEach { data ->
                    val diff = data.amount - data.prevAmount
                    val isIncrease = diff > 0
                    val iconColor = Color(data.category.color)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBackgroundColor(), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular icon background (matching TransactionItem)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconByName(data.category.iconName),
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Category name and comparison text
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.category.name, 
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (data.prevAmount > 0) {
                                Text(
                                    text = "${if (isIncrease) "+" else ""}${String.format("%.0f", diff)} vs prev",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isIncrease) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            } else if (data.amount > 0 && data.prevAmount == 0.0) {
                                Text(
                                    text = "New",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        // Amount
                        Text(
                            text = "$currency${String.format("%.2f", data.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}


data class ReportData(
    val income: Double,
    val expense: Double,
    val transfer: Double,
    val prevExpense: Double,
    val expenseChange: Double,
    val categoryData: List<CategoryData>,
    val breakdownData: List<CategoryData>
)
data class CategoryData(val category: Category, val amount: Double, val prevAmount: Double)
