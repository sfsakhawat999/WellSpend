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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.ui.getCategoryColor
import com.h2.wellspend.ui.getCategoryIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReport(
    expenses: List<Expense>,
    currency: String,
    currentDate: LocalDate,
    onBack: () -> Unit
) {
    var showCompareDialog by remember { mutableStateOf(false) }
    var comparisonDate by remember { mutableStateOf<LocalDate?>(null) }

    val (currentTotal, prevTotal, percentChange, categoryData) = remember(expenses, currentDate, comparisonDate) {
        val currentMonthExpenses = expenses.filter {
            val date = LocalDate.parse(it.date.substring(0, 10))
            date.month == currentDate.month && date.year == currentDate.year
        }
        
        // Use comparisonDate if set, otherwise default to previous month
        val prevDate = comparisonDate ?: currentDate.minusMonths(1)
        val prevMonthExpenses = expenses.filter {
            val date = LocalDate.parse(it.date.substring(0, 10))
            date.month == prevDate.month && date.year == prevDate.year
        }

        val cTotal = currentMonthExpenses.sumOf { it.amount }
        val pTotal = prevMonthExpenses.sumOf { it.amount }
        val diff = cTotal - pTotal
        val pChange = if (pTotal == 0.0) 100.0 else (diff / pTotal) * 100.0

        val currentCatMap = currentMonthExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        
        val prevCatMap = prevMonthExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        val catData = currentCatMap.map { (cat, amount) ->
            val prevAmount = prevCatMap[cat] ?: 0.0
            CategoryData(cat, amount, prevAmount)
        }.sortedByDescending { it.amount }

        Quadruple(cTotal, pTotal, pChange, catData)
    }

    // Available months for comparison
    val availableMonths = remember(expenses) {
        expenses.map { LocalDate.parse(it.date.substring(0, 10)).withDayOfMonth(1) }
            .distinct()
            .sortedDescending()
            .filter { !(it.month == currentDate.month && it.year == currentDate.year) }
    }

    if (showCompareDialog) {
        Dialog(onDismissRequest = { showCompareDialog = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Compare with...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    availableMonths.forEach { date ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    comparisonDate = date
                                    showCompareDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                color = if (comparisonDate == date) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { showCompareDialog = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Monthly Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCompareDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Compare", tint = MaterialTheme.colorScheme.primary)
                    }
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = {
                        val csvHeader = "Date,Category,Amount,Description,Recurring\n"
                        val csvData = expenses.joinToString("\n") {
                            "${it.date},${it.category.name},${it.amount},${it.description},${it.isRecurring}"
                        }
                        val csvContent = csvHeader + csvData

                        try {
                            val fileName = "expenses_${currentDate.format(DateTimeFormatter.ofPattern("MMM_yyyy"))}.csv"
                            val file = java.io.File(context.cacheDir, fileName)
                            file.writeText(csvContent)
                            
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "com.h2.wellspend.fileprovider",
                                file
                            )

                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Expenses")
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {


        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Summary Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$currency${String.format("%.2f", currentTotal)}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (percentChange > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (percentChange > 0) Icons.Default.ArrowOutward else Icons.AutoMirrored.Filled.CallReceived, // Using CallReceived as arrow down right approx
                        contentDescription = null,
                        tint = if (percentChange > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.1f", abs(percentChange))}% vs ${
                            (comparisonDate ?: currentDate.minusMonths(1)).format(DateTimeFormatter.ofPattern("MMM yyyy"))
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (percentChange > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Previous: $currency${String.format("%.2f", prevTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chart (Simple horizontal bars for now)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (categoryData.isEmpty()) {
                     Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val maxVal = categoryData.first().amount
                    categoryData.forEach { data ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(data.category),
                                contentDescription = data.category.name,
                                tint = getCategoryColor(data.category),
                                modifier = Modifier.width(24.dp).size(16.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((data.amount / maxVal).toFloat())
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(getCategoryColor(data.category))
                                )
                            }
                        }
                    }
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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categoryData.forEach { data ->
                    val diff = data.amount - data.prevAmount
                    val isIncrease = diff > 0
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(
                                imageVector = getCategoryIcon(data.category),
                                contentDescription = null,
                                tint = getCategoryColor(data.category),
                                modifier = Modifier.size(24.dp).padding(end = 8.dp)
                            )
                            Text(data.category.name, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currency${String.format("%.2f", data.amount)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
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
                    }
                }
            }
        }
    }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
data class CategoryData(val category: Category, val amount: Double, val prevAmount: Double)
