package com.h2.wellspend

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.h2.wellspend.data.Category
import com.h2.wellspend.ui.CategoryColors
import com.h2.wellspend.ui.components.AddExpenseForm
import com.h2.wellspend.ui.components.BudgetSettings
import com.h2.wellspend.ui.components.ChartData
import com.h2.wellspend.ui.components.DonutChart
import com.h2.wellspend.ui.components.ExpenseList
import com.h2.wellspend.ui.components.MonthlyReport
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ViewState {
    DASHBOARD, LIST, ADD, REPORT, BUDGETS
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    
    var viewState by remember { mutableStateOf(ViewState.DASHBOARD) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    // Handle back button
    androidx.activity.compose.BackHandler(enabled = viewState != ViewState.DASHBOARD) {
        viewState = ViewState.DASHBOARD
    }

    // Filter expenses for current month
    val currentMonthExpenses = expenses.filter {
        val date = LocalDate.parse(it.date.substring(0, 10))
        date.month == currentDate.month && date.year == currentDate.year
    }

    val totalSpend = currentMonthExpenses.sumOf { it.amount }

    val chartData = currentMonthExpenses.groupBy { it.category }
        .map { (cat, list) ->
            ChartData(
                name = cat.name,
                value = list.sumOf { it.amount },
                color = CategoryColors[cat] ?: Color.Gray
            )
        }
        .sortedByDescending { it.value }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.exportData(uri, context.contentResolver) { success, message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importData(uri, context.contentResolver) { success, message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (viewState == ViewState.DASHBOARD || viewState == ViewState.LIST) {
                FloatingActionButton(
                    onClick = { viewState = ViewState.ADD },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AnimatedContent(
                targetState = viewState,
                transitionSpec = {
                    if (targetState == ViewState.DASHBOARD || targetState == ViewState.LIST) {
                        // Going back to root
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    } else {
                        // Going to detail
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    }
                },
                label = "MainNavigation"
            ) { state ->
                when (state) {
                    ViewState.DASHBOARD -> {
                        DashboardScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            onReportClick = { viewState = ViewState.REPORT },
                            onToggleView = { viewState = ViewState.LIST },
                            chartData = chartData,
                            totalSpend = totalSpend,
                            currency = currency,
                            budgets = budgets,
                            onBudgetClick = { viewState = ViewState.BUDGETS }
                        )
                    }
                    ViewState.LIST -> {
                        ExpenseListScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            onReportClick = { viewState = ViewState.REPORT },
                            onToggleView = { viewState = ViewState.DASHBOARD },
                            expenses = currentMonthExpenses,
                            currency = currency,
                            onDelete = { viewModel.deleteExpense(it) }
                        )
                    }
                    ViewState.ADD -> {
                        val categoryOrder by viewModel.categoryOrder.collectAsState()
                        AddExpenseForm(
                            currency = currency,
                            categories = categoryOrder,
                            onAdd = { amount, desc, cat, date, isRecurring, freq ->
                                viewModel.addExpense(amount, desc, cat, date, isRecurring, freq)
                                viewState = ViewState.DASHBOARD
                            },
                            onCancel = { viewState = ViewState.DASHBOARD },
                            onReorder = { viewModel.updateCategoryOrder(it) }
                        )
                    }
                    ViewState.BUDGETS -> {
                        BudgetSettings(
                            currentBudgets = budgets,
                            currentCurrency = currency,
                            currentThemeMode = themeMode,
                            currentDynamicColor = dynamicColor,
                            onSave = { newBudgets, newCurrency ->
                                viewModel.updateBudgets(newBudgets, newCurrency)
                            },
                            onThemeModeChange = { viewModel.updateThemeMode(it) },
                            onDynamicColorChange = { viewModel.updateDynamicColor(it) },
                            onExport = { exportLauncher.launch("wellspend_backup.json") },
                            onImport = { importLauncher.launch(arrayOf("application/json")) },
                            onClose = { viewState = ViewState.DASHBOARD }
                        )
                    }
                    ViewState.REPORT -> {
                        MonthlyReport(
                            expenses = expenses,
                            currency = currency,
                            currentDate = currentDate,
                            onBack = { viewState = ViewState.DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onReportClick: () -> Unit,
    onToggleView: () -> Unit,
    isListView: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDateChange(currentDate.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Text(
                text = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onDateChange(currentDate.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        Row {
            IconButton(onClick = onReportClick) {
                Icon(Icons.Default.Description, contentDescription = "Report", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            IconButton(onClick = onToggleView) {
                Icon(
                    imageVector = if (isListView) Icons.Default.BarChart else Icons.Default.List,
                    contentDescription = "Toggle View",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onReportClick: () -> Unit,
    onToggleView: () -> Unit,
    chartData: List<ChartData>,
    totalSpend: Double,
    currency: String,
    budgets: List<com.h2.wellspend.data.Budget>,
    onBudgetClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            currentDate = currentDate,
            onDateChange = onDateChange,
            onReportClick = onReportClick,
            onToggleView = onToggleView,
            isListView = false
        )

        Column(modifier = Modifier.fillMaxSize()) {
            DonutChart(
                data = chartData,
                totalAmount = totalSpend,
                currency = currency,
                onCenterClick = onToggleView
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPENDING & BUDGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.clickable { onBudgetClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Settings", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Budget List
                if (chartData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No expenses this month. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        chartData.forEach { item ->
                            val budget = budgets.find { it.category.name == item.name }
                            val limit = budget?.limitAmount ?: 0.0
                            val percent = if (limit > 0) (item.value / limit) * 100 else 0.0
                            val isOver = limit > 0 && item.value > limit

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).background(item.color, CircleShape))
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                    }
                                    Text("$currency${item.value.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }

                                if (limit > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = if (isOver) "Over Budget" else "${percent.toInt()}% of $currency${limit.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(minOf(percent / 100, 1.0).toFloat())
                                                .height(8.dp)
                                                .background(if (isOver) MaterialTheme.colorScheme.error else item.color, RoundedCornerShape(4.dp))
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
}

@Composable
fun ExpenseListScreen(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onReportClick: () -> Unit,
    onToggleView: () -> Unit,
    expenses: List<com.h2.wellspend.data.Expense>,
    currency: String,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            currentDate = currentDate,
            onDateChange = onDateChange,
            onReportClick = onReportClick,
            onToggleView = onToggleView,
            isListView = true
        )
        ExpenseList(
            expenses = expenses,
            currency = currency,
            onDelete = onDelete
        )
    }
}
