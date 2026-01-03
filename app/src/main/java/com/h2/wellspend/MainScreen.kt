package com.h2.wellspend

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.h2.wellspend.ui.components.ChartData
import com.h2.wellspend.ui.components.DonutChart
import com.h2.wellspend.ui.components.ExpenseList
import com.h2.wellspend.ui.components.MonthlyReport
import com.h2.wellspend.ui.AccountScreen
import com.h2.wellspend.data.Expense
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.h2.wellspend.ui.components.MoreScreen
import com.h2.wellspend.ui.components.SettingsScreen
import com.h2.wellspend.ui.components.BudgetScreen


import com.h2.wellspend.ui.components.LoanScreen

enum class Screen {
    HOME, ACCOUNTS, INCOME, EXPENSES, MORE
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    val listState = rememberLazyListState()
    
    // Navigation State
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    // Overlay States (Sub-screens)
    var showAddExpense by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showBudgets by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTransfers by remember { mutableStateOf(false) }
    var showLoans by remember { mutableStateOf(false) }
    var showDataManagement by remember { mutableStateOf(false) } // Maps to Settings for now
    var loanTransactionToEdit by remember { mutableStateOf<Expense?>(null) }

    // Data
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val budgets by viewModel.budgets.collectAsState(initial = emptyList())
    val currency by viewModel.currency.collectAsState(initial = "$")
    val themeMode by viewModel.themeMode.collectAsState(initial = "SYSTEM")
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = false)
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val loans by viewModel.loans.collectAsState(initial = emptyList())
    val balances by viewModel.accountBalances.collectAsState(initial = emptyMap())

    // Handle back button
    val canNavigateBack = showAddExpense || showReport || showBudgets || showSettings || showTransfers || showLoans || currentScreen != Screen.HOME
    androidx.activity.compose.BackHandler(enabled = canNavigateBack) {
        when {
            showAddExpense -> showAddExpense = false
            showReport -> showReport = false
            showBudgets -> showBudgets = false
            showSettings -> showSettings = false
            showTransfers -> showTransfers = false
            showLoans -> showLoans = false
            currentScreen != Screen.HOME -> currentScreen = Screen.HOME
        }
    }

    // Filter transactions for current month
    val currentMonthTransactions = expenses.filter {
        val date = try { LocalDate.parse(it.date.take(10)) } catch(e:Exception) { LocalDate.now() }
        date.month == currentDate.month && date.year == currentDate.year
    }

    // Calculate Total Spend: (All Expenses Base Amount) + (All Fees from any transaction type)
    // EXCLUDING Loan transactions with NO Account (Virtual/Cash/Untracked)
    val validTransactions = currentMonthTransactions.filter { !(it.loanId != null && it.accountId == null) }
    
    val totalSpend = validTransactions.filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }.sumOf { it.amount } + 
                     validTransactions.sumOf { it.feeAmount }

    // For Chart: Only include explicitly categorized EXPENSES (exclude Income/Transfer base amounts)
    // For Chart: Only include explicitly categorized EXPENSES (exclude Income/Transfer base amounts)
    val expensesByCat = validTransactions
        .filter { it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
        .groupBy { it.category }
        
    val chartDataList = expensesByCat.map { (cat, list) ->
        ChartData(
            name = cat.name,
            value = list.sumOf { it.amount }, // Fees are handled separately
            color = CategoryColors[cat] ?: Color.Gray
        )
    }.toMutableList()

    // Add ALL fees (from Income, Transfer, and Expense) as a single "Transaction Fee" slice
    val totalFees = validTransactions.sumOf { it.feeAmount }
    if (totalFees > 0) {
        chartDataList.add(
            ChartData(
                name = Category.TransactionFee.name,
                value = totalFees,
                color = CategoryColors[Category.TransactionFee] ?: Color.Gray
            )
        )
    }
    val chartData = chartDataList.sortedByDescending { it.value }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.exportData(uri, context.contentResolver) { _, message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importData(uri, context.contentResolver) { _, message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showAddExpense && !showReport && !showBudgets && !showSettings && !showTransfers && !showLoans) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.HOME,
                        onClick = { currentScreen = Screen.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.ACCOUNTS,
                        onClick = { currentScreen = Screen.ACCOUNTS },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
                        label = { Text("Accounts") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.INCOME,
                        onClick = { currentScreen = Screen.INCOME },
                        icon = { Icon(Icons.Default.AttachMoney, contentDescription = "Income") },
                        label = { Text("Income") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.EXPENSES,
                        onClick = { currentScreen = Screen.EXPENSES },
                        icon = { Icon(Icons.AutoMirrored.Filled.ListIcon, contentDescription = "Expenses") },
                        label = { Text("Expenses") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.MORE,
                        onClick = { currentScreen = Screen.MORE },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                        label = { Text("More") }
                    )
                }
            }
        },
        floatingActionButton = {
            if ((currentScreen == Screen.HOME || currentScreen == Screen.EXPENSES || currentScreen == Screen.INCOME) && !showAddExpense && !showReport && !showBudgets && !showSettings && !showTransfers && !showLoans) {
                FloatingActionButton(
                    onClick = { 
                        expenseToEdit = null
                        showAddExpense = true
                    },
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
            val targetState = when {
                showAddExpense -> "OVERLAY_ADD"
                loanTransactionToEdit != null -> "OVERLAY_EDIT_LOAN_TRANSACTION"
                showReport -> "OVERLAY_REPORT"
                showBudgets -> "OVERLAY_BUDGETS"
                showSettings -> "OVERLAY_SETTINGS"
                showTransfers -> "OVERLAY_TRANSFERS"
                showLoans -> "OVERLAY_LOANS"
                else -> currentScreen.name
            }

            AnimatedContent(
                targetState = targetState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(200))
                },
                label = "MainContentTransition"
            ) { state ->
                when (state) {
                    "OVERLAY_ADD" -> {
                        val categoryOrder by viewModel.categoryOrder.collectAsState()
                        AddExpenseForm(
                            currency = currency,
                            accounts = accounts,
                            categories = categoryOrder,
                            initialExpense = expenseToEdit,
                            onAdd = { amount, desc, cat, date, isRecurring, freq, type, accId, targetAccId, fee, feeName ->
                                if (expenseToEdit != null) {
                                    viewModel.updateExpense(
                                        id = expenseToEdit!!.id, 
                                        amount = amount, 
                                        description = desc, 
                                        category = cat, 
                                        date = date, 
                                        isRecurring = isRecurring, 
                                        frequency = freq,
                                        transactionType = type,
                                        accountId = accId,
                                        targetAccountId = targetAccId,
                                        feeAmount = fee,
                                        feeConfigName = feeName
                                    )
                                } else {
                                    viewModel.addExpense(
                                        amount = amount, 
                                        description = desc, 
                                        category = cat, 
                                        date = date, 
                                        isRecurring = isRecurring, 
                                        frequency = freq,
                                        transactionType = type,
                                        accountId = accId,
                                        targetAccountId = targetAccId,
                                        feeAmount = fee,
                                        feeConfigName = feeName
                                    )
                                }
                                showAddExpense = false
                            },
                            onCancel = { showAddExpense = false },
                            onReorder = { viewModel.updateCategoryOrder(it) }
                        )
                    }
                    "OVERLAY_EDIT_LOAN_TRANSACTION" -> {
                         val transaction = loanTransactionToEdit!!
                         val relatedLoan = loans.find { it.id == transaction.loanId }
                         if (relatedLoan != null) {
                             com.h2.wellspend.ui.components.EditLoanTransactionScreen(
                                transaction = transaction,
                                loan = relatedLoan,
                                accounts = accounts,
                                currency = currency,
                                onDismiss = { loanTransactionToEdit = null },
                                onConfirm = { amt, desc, accId, fee, feeName, date ->
                                    viewModel.updateExpense(
                                        id = transaction.id,
                                        amount = amt,
                                        description = desc,
                                        category = transaction.category,
                                        date = date,
                                        isRecurring = false,
                                        frequency = com.h2.wellspend.data.RecurringFrequency.WEEKLY, // Dummy
                                        transactionType = transaction.transactionType,
                                        accountId = accId,
                                        targetAccountId = transaction.transferTargetAccountId,
                                        feeAmount = fee,
                                        feeConfigName = feeName,
                                        loanId = transaction.loanId
                                    )
                                    loanTransactionToEdit = null
                                }
                             )
                         } else {
                             // Should not happen, but reset
                             LaunchedEffect(Unit) { loanTransactionToEdit = null }
                         }
                    }
                    "OVERLAY_REPORT" -> {
                        MonthlyReport(
                            expenses = expenses,
                            currency = currency,
                            currentDate = currentDate,
                            onBack = { showReport = false }
                        )
                    }
                    "OVERLAY_BUDGETS" -> {
                         BudgetScreen(
                            currentBudgets = budgets,
                            currency = currency,
                            onSave = { newBudgets ->
                                 viewModel.updateBudgets(newBudgets, currency)
                            },
                            onBack = { showBudgets = false }
                         )
                    }
                    "OVERLAY_TRANSFERS" -> {
                        TransferListScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            onReportClick = { showReport = true },
                            expenses = currentMonthTransactions,
                            accounts = accounts,
                            currency = currency,
                            onDelete = { viewModel.deleteExpense(it) },
                            onEdit = { 
                                expenseToEdit = it
                                showAddExpense = true
                            },
                            onBack = { showTransfers = false }
                        )
                    }
                    "OVERLAY_LOANS" -> {
                        LoanScreen(
                            loans = loans,
                            expenses = expenses, // All expenses to calc balance
                            accounts = accounts,
                            currency = currency,
                            onAddLoan = { name, amount, type, desc, accId, fee, feeName, date ->
                                viewModel.addLoan(name, amount, type, desc, accId, fee, feeName, date)
                            },
                            onAddTransaction = { loanId, amount, isPayment, accId, type, fee, feeName, date ->
                                viewModel.addLoanTransaction(loanId, amount, isPayment, accId, type, fee, feeName, date)
                            },
                            onUpdateLoan = { viewModel.updateLoan(it) },
                            onDeleteLoan = { viewModel.deleteLoan(it) },
                            onBack = { showLoans = false }
                        )
                    }
                    "OVERLAY_SETTINGS" -> {
                         SettingsScreen(
                            currentCurrency = currency,
                            currentThemeMode = themeMode,
                            currentDynamicColor = dynamicColor,
                            onCurrencyChange = { newCurrency ->
                                 viewModel.updateBudgets(emptyList(), newCurrency)
                            },
                            onThemeModeChange = { viewModel.updateThemeMode(it) },
                            onDynamicColorChange = { viewModel.updateDynamicColor(it) },
                            onExport = { 
                                val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                                exportLauncher.launch("wellspend_backup_$timestamp.json") 
                            },
                            onImport = { importLauncher.launch(arrayOf("application/json")) },
                            onBack = { showSettings = false }
                         )
                    }
                    "HOME" -> {
                         DashboardScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            onReportClick = { showReport = true },
                            onChartClick = { currentScreen = Screen.EXPENSES },
                            chartData = chartData,
                            totalSpend = totalSpend,
                            currency = currency,
                            budgets = budgets,
                            onBudgetClick = { showBudgets = true },
                            accounts = accounts,
                            accountBalances = balances,
                            onAccountClick = { currentScreen = Screen.ACCOUNTS }
                        )
                    }
                    "ACCOUNTS" -> {
                         AccountScreen(
                            accounts = accounts,
                            balances = balances,
                            currency = currency,
                            onAddAccount = { viewModel.addAccount(it) },
                            onUpdateAccount = { viewModel.addAccount(it) },
                            onDeleteAccount = { viewModel.deleteAccount(it) },
                            isAccountUsed = { accountId -> expenses.any { it.accountId == accountId } },
                            onReorder = { viewModel.reorderAccounts(it) },
                            onAdjustBalance = { id, amount -> viewModel.adjustAccountBalance(id, amount) }
                         )
                    }
                    "INCOME" -> {
                        IncomeListScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            onReportClick = { showReport = true },
                            expenses = currentMonthTransactions,
                            accounts = accounts,
                            loans = loans,
                            currency = currency,
                            onDelete = { viewModel.deleteExpense(it) },
                            onEdit = { 
                                if (it.loanId != null) {
                                    loanTransactionToEdit = it
                                } else {
                                    expenseToEdit = it
                                    showAddExpense = true
                                }
                            }
                        )
                    }
                    "EXPENSES" -> {
                         ExpenseListScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            onReportClick = { showReport = true },
                            expenses = currentMonthTransactions,
                            accounts = accounts,
                            loans = loans,
                            currency = currency,
                            onDelete = { viewModel.deleteExpense(it) },
                            onEdit = { 
                                if (it.loanId != null) {
                                    loanTransactionToEdit = it
                                } else {
                                    expenseToEdit = it
                                    showAddExpense = true
                                }
                            },
                            state = listState
                        )
                    }
                    "MORE" -> {
                         MoreScreen(
                             onReportClick = { showReport = true },
                             onBudgetsClick = { showBudgets = true },
                             onSettingsClick = { showSettings = true },
                             onDataManagementClick = { showSettings = true }, // Maps to Settings for now
                             onTransfersClick = { showTransfers = true },
                             onLoansClick = { showLoans = true }
                         )
                    }
                }
            }

            // Dialog for Editing Loan Transaction
        }
    }
}

@Composable
fun TopBar(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onReportClick: () -> Unit
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

        IconButton(onClick = onReportClick) {
            Icon(Icons.Default.Description, contentDescription = "Report", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun DashboardScreen(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onReportClick: () -> Unit,
    onChartClick: () -> Unit,
    chartData: List<ChartData>,
    totalSpend: Double,
    currency: String,
    budgets: List<com.h2.wellspend.data.Budget>,
    onBudgetClick: () -> Unit,
    accounts: List<com.h2.wellspend.data.Account>,
    accountBalances: Map<String, Double>,
    onAccountClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            currentDate = currentDate,
            onDateChange = onDateChange,
            onReportClick = onReportClick
        )

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            DonutChart(
                data = chartData,
                totalAmount = totalSpend,
                currency = currency,
                onCenterClick = onChartClick
            )
            
            // Accounts Section
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCOUNTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                
                if (accounts.isEmpty()) {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { onAccountClick() }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No accounts. Add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        accounts.forEach { acc ->
                             // Calculate Balance (Incomplete: Currently only initialBalance is shown properly without full calculation logic in VM)
                             // Ideally we should pass a Map<String, Double> of balances or enrich Account object.
                             // For now, let's just show initialBalance as placeholder or just name.
                             // Actually, user wants to see balances. 
                             // I should assume the Account entity or a separate flow provides the calculated balance.
                             // But my repository only provides `getAllAccounts` which returns `List<Account>`. 
                             // The `Account` entity only has `initialBalance`.
                             // Wait, I missed the balance calculation logic in Repository!
                             // My plan said: "Implement Repository Logic (Balances...)".
                             // I checked `WellSpendRepository.kt` in the beginning and it had `getAllAccounts`.
                             // Did I implement `getAccountBalances`?
                             // I need to check `WellSpendRepository.kt`.
                             // If I didn't, I need to do it.
                             // For now, I'll display "Tap to see balance" or just initial balance.
                             // Actually, let's display the name and a placeholder.
                             
                             Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.width(140.dp)
                             ) {
                                 Column(modifier = Modifier.padding(16.dp)) {
                                     Text(acc.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                                     Spacer(modifier = Modifier.height(8.dp))
                                     val bal = accountBalances[acc.id] ?: acc.initialBalance
                                     Text("$currency${String.format("%.2f", bal)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                 }
                             }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPENDING & BUDGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
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
                                    Text("$currency${String.format("%.2f", item.value)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }

                                if (limit > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = if (isOver) "Over Budget" else "${percent.toInt()}% of $currency${String.format("%.2f", limit)}",
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
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable

fun ExpenseListScreen(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onReportClick: () -> Unit,
    expenses: List<com.h2.wellspend.data.Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    state: LazyListState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            currentDate = currentDate,
            onDateChange = onDateChange,
            onReportClick = onReportClick
        )
        
        // Show Expenses ONLY (Transfers -> More > Transfers, Income -> Bottom Tab)
        val expenseList = expenses.filter { 
            it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE &&
            !(it.loanId != null && it.accountId == null)
        }
        ExpenseList(
            expenses = expenseList,
            accounts = accounts,
            loans = loans,
            currency = currency,
            onDelete = onDelete,
            onEdit = onEdit,
            state = state
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferListScreen(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onReportClick: () -> Unit,
    expenses: List<com.h2.wellspend.data.Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Transfers", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
        TopBar(
            currentDate = currentDate,
            onDateChange = onDateChange,
            onReportClick = onReportClick
        )
        
        // Filter only Transfers
        val transfers = expenses.filter { it.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER }
        
        com.h2.wellspend.ui.components.TransferList(
            transfers = transfers,
            accounts = accounts,
            currency = currency,
            onDelete = onDelete,
            onEdit = onEdit
        )
    }
}

@Composable

fun IncomeListScreen(
    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onReportClick: () -> Unit,
    expenses: List<com.h2.wellspend.data.Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            currentDate = currentDate,
            onDateChange = onDateChange,
            onReportClick = onReportClick
        )
        
        // Filter only Incomes
        // Filter only Incomes
        val incomes = expenses.filter { 
            it.transactionType == com.h2.wellspend.data.TransactionType.INCOME &&
            !(it.loanId != null && it.accountId == null)
        }
        
        com.h2.wellspend.ui.components.IncomeList(
            incomes = incomes,
            accounts = accounts,
            loans = loans,
            currency = currency,
            onDelete = onDelete,
            onEdit = onEdit
        )
    }
}
