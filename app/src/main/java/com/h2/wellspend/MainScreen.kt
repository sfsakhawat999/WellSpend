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
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInVertically
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
import com.h2.wellspend.data.Account
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip

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

    var loanTransactionToEdit by remember { mutableStateOf<Expense?>(null) }
    
    // Account Input State: Wrapper to handle "Add" (null content) vs "Edit" (account content)
    // If showAccountInput is true, we show the screen. The content depends on accountToEdit.
    var showAccountInput by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    // Data
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val budgets by viewModel.budgets.collectAsState(initial = emptyList())
    val currency by viewModel.currency.collectAsState(initial = "$")
    val themeMode by viewModel.themeMode.collectAsState(initial = "SYSTEM")
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = false)
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val loans by viewModel.loans.collectAsState(initial = emptyList())
    val balances by viewModel.accountBalances.collectAsState(initial = emptyMap())
    val excludeLoanTransactions by viewModel.excludeLoanTransactions.collectAsState()

    // Track if initial data has loaded (show skeleton until first real data arrives)
    var isDataLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(expenses, accounts) {
        // Consider data loaded once we have attempted to fetch (even if empty after load)
        // We use a small delay to ensure skeleton shows briefly for smooth UX
        if (!isDataLoaded && (expenses.isNotEmpty() || accounts.isNotEmpty())) {
            isDataLoaded = true
        }
        // Also mark as loaded after a timeout to handle empty database case
        kotlinx.coroutines.delay(500)
        isDataLoaded = true
    }

    val canNavigateBack = showAddExpense || showReport || showBudgets || showSettings || showTransfers || showLoans || showAccountInput || loanTransactionToEdit != null || currentScreen != Screen.HOME
    androidx.activity.compose.BackHandler(enabled = canNavigateBack) {
        when {
            loanTransactionToEdit != null -> loanTransactionToEdit = null
            showAddExpense -> showAddExpense = false
            showAccountInput -> { showAccountInput = false; accountToEdit = null }
            showReport -> showReport = false
            showBudgets -> showBudgets = false
            showSettings -> showSettings = false
            showTransfers -> showTransfers = false
            showLoans -> showLoans = false
            currentScreen != Screen.HOME -> currentScreen = Screen.HOME
        }
    }

    val currentMonthTransactions = expenses.filter {
        val date = try { LocalDate.parse(it.date.take(10)) } catch(e:Exception) { LocalDate.now() }
        date.month == currentDate.month && date.year == currentDate.year
    }

    // Calculate Total Spend: (All Expenses Base Amount) + (All Fees from any transaction type)
    // EXCLUDING Loan transactions with NO Account (Virtual/Cash/Untracked)
    // AND optionally excluding ALL loan transactions if setting is enabled
    val validTransactions = currentMonthTransactions.filter { transaction ->
        val isVirtualLoan = transaction.loanId != null && transaction.accountId == null
        val isExcludedLoan = excludeLoanTransactions && transaction.loanId != null
        !isVirtualLoan && !isExcludedLoan
    }
    
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
            if (!showAddExpense && !showReport && !showBudgets && !showSettings && !showTransfers && !showLoans && !showAccountInput && loanTransactionToEdit == null) {
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
            if ((currentScreen == Screen.HOME || currentScreen == Screen.EXPENSES || currentScreen == Screen.INCOME) && 
                !showAddExpense && !showReport && !showBudgets && !showSettings && !showTransfers && !showLoans && !showAccountInput && loanTransactionToEdit == null) {
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
                showAccountInput -> "OVERLAY_ACCOUNT_INPUT"
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
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))) togetherWith
                    (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))) using
                    SizeTransform(clip = false)
                },
                label = "MainContentTransition"
            ) { state ->
                when (state) {
                    "OVERLAY_ADD" -> {
                        val categoryOrder by viewModel.categoryOrder.collectAsState()
                        AddExpenseForm(
                            currency = currency,
                            accounts = accounts,
                            accountBalances = balances,
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
                         // Capture transaction safely - during exit animation, loanTransactionToEdit may be null
                         val transaction = loanTransactionToEdit
                         if (transaction != null) {
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
                             }
                         }
                         // Empty composable during exit animation when transaction is null
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
                            accountBalances = balances,
                            currency = currency,
                            onAddLoan = { name, amount, type, desc, accId, fee, feeName, date ->
                                viewModel.addLoan(name, amount, type, desc, accId, fee, feeName, date)
                            },
                            onAddTransaction = { loanId, loanName, amount, isPayment, accId, type, fee, feeName, date ->
                                viewModel.addLoanTransaction(loanId, loanName, amount, isPayment, accId, type, fee, feeName, date)
                            },
                            onUpdateLoan = { viewModel.updateLoan(it) },
                            onDeleteLoan = { loan, deleteTransactions -> viewModel.deleteLoan(loan, deleteTransactions) },
                            onBack = { showLoans = false }
                        )
                    }
                    "OVERLAY_SETTINGS" -> {
                            SettingsScreen(
                                currentCurrency = currency,
                                currentThemeMode = themeMode,
                                currentDynamicColor = dynamicColor,
                                excludeLoanTransactions = excludeLoanTransactions,
                                onCurrencyChange = { newCurrency ->
                                     viewModel.updateBudgets(emptyList(), newCurrency)
                                },
                                onThemeModeChange = { viewModel.updateThemeMode(it) },
                                onDynamicColorChange = { viewModel.updateDynamicColor(it) },
                                onExcludeLoanTransactionsChange = { viewModel.updateExcludeLoanTransactions(it) },
                                onExport = { 
                                    val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                                    exportLauncher.launch("wellspend_backup_$timestamp.json") 
                                },
                                onImport = { importLauncher.launch(arrayOf("application/json")) },
                                onBack = { showSettings = false }
                             )
                    }
                    "HOME" -> {
                        // Filter out virtual loan transactions (same logic as expense/income pages)
                        // AND respect value of excludeLoanTransactions
                        val validTransactionsForDisplay = currentMonthTransactions.filter { transaction ->
                            val isVirtualLoan = transaction.loanId != null && transaction.accountId == null
                            val isExcludedLoan = excludeLoanTransactions && transaction.loanId != null
                            !isVirtualLoan && !isExcludedLoan
                        }
                        
                        // Calculate balance at end of selected month
                        // Filter all expenses up to end of current month
                        val monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth())
                        val transactionsUpToMonthEnd = expenses.filter { exp ->
                            val expDate = try { LocalDate.parse(exp.date.take(10)) } catch(e: Exception) { LocalDate.now() }
                            !expDate.isAfter(monthEnd)
                        }
                        
                        val monthEndBalance = accounts.sumOf { account ->
                            val initial = account.initialBalance
                            val acctTransactions = transactionsUpToMonthEnd
                            
                            val accountExpenses = acctTransactions.filter { it.accountId == account.id && it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
                            val transfersOut = acctTransactions.filter { it.accountId == account.id && it.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER }
                            val transfersIn = acctTransactions.filter { it.transferTargetAccountId == account.id && it.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER }
                            val incomes = acctTransactions.filter { it.accountId == account.id && it.transactionType == com.h2.wellspend.data.TransactionType.INCOME }
                            
                            val totalExpense = accountExpenses.sumOf { it.amount + it.feeAmount }
                            val totalTransferOut = transfersOut.sumOf { it.amount + it.feeAmount }
                            val totalIncomeNet = incomes.sumOf { it.amount - it.feeAmount }
                            val totalTransferIn = transfersIn.sumOf { it.amount }
                            
                            initial + totalIncomeNet - totalExpense - totalTransferOut + totalTransferIn
                        }
                        
                        // Calculate income for this month (excluding virtual loan transactions)
                        val totalIncome = validTransactionsForDisplay
                            .filter { it.transactionType == com.h2.wellspend.data.TransactionType.INCOME }
                            .sumOf { it.amount - it.feeAmount }
                        
                        // All transactions for lazy loading (sorted by date desc)
                        val allMonthTransactions = validTransactionsForDisplay
                            .sortedByDescending { it.timestamp }
                        
                        DashboardScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            onReportClick = { showReport = true },
                            currency = currency,
                            totalBalance = monthEndBalance,
                            totalIncome = totalIncome,
                            totalExpense = totalSpend,
                            recentTransactions = allMonthTransactions,
                            allAccounts = accounts,
                            loans = loans,
                            isLoading = !isDataLoaded,
                            onEdit = { transaction ->
                                if (transaction.loanId != null) {
                                    loanTransactionToEdit = transaction
                                } else {
                                    expenseToEdit = transaction
                                    showAddExpense = true
                                }
                            },
                            onDelete = { id -> viewModel.deleteExpense(id) }
                        )
                    }
                    "OVERLAY_ACCOUNT_INPUT" -> {
                        com.h2.wellspend.ui.AccountInputScreen(
                            account = accountToEdit,
                            currentBalance = if (accountToEdit != null) balances[accountToEdit!!.id] else null,
                            currency = currency,
                            onDismiss = { 
                                showAccountInput = false 
                                accountToEdit = null
                            },
                            onSave = { account, adjustment ->
                                viewModel.updateAccount(account)
                                if (adjustment != null && adjustment != 0.0) {
                                     viewModel.addAdjustmentTransaction(account.id, account.name, adjustment)
                                }
                                showAccountInput = false
                                accountToEdit = null
                            }
                        )
                    }
                    "ACCOUNTS" -> {
                        com.h2.wellspend.ui.AccountScreen(
                            accounts = accounts,
                            balances = balances,
                            currency = currency,
                            onDeleteAccount = { viewModel.deleteAccount(it) },
                            isAccountUsed = { id -> expenses.any { it.accountId == id } },
                            onReorder = { viewModel.updateAccountOrder(it) },
                            onAdjustBalance = { accId, adj -> 
                                val accName = accounts.find { it.id == accId }?.name ?: "Unknown"
                                viewModel.addAdjustmentTransaction(accId, accName, adj) 
                            },
                            onAddAccount = { 
                                accountToEdit = null
                                showAccountInput = true 
                            },
                            onEditAccount = { acc ->
                                accountToEdit = acc
                                showAccountInput = true
                            }
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
                            state = listState,
                            chartData = chartData,
                            totalSpend = totalSpend
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
    currency: String,
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    recentTransactions: List<com.h2.wellspend.data.Expense>,
    allAccounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    isLoading: Boolean = false,
    onEdit: (com.h2.wellspend.data.Expense) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            currentDate = currentDate,
            onDateChange = onDateChange,
            onReportClick = onReportClick
        )

        if (isLoading) {
            // Skeleton Loading UI with shimmer effect
            val transition = rememberInfiniteTransition(label = "shimmer")
            val shimmerAlpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmer_alpha"
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Summary skeleton
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(4.dp))
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(16.dp))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(16.dp))
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Accounts skeleton
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(4.dp))
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(80.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(16.dp))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Transactions skeleton
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(4.dp))
                    )
                }
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(12.dp))
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
            ) {
            // Summary Cards Section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    // Summary Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Balance Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Total Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "$currency${if (totalBalance % 1.0 == 0.0) String.format("%.0f", totalBalance) else String.format("%.2f", totalBalance)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "+$currency${if (totalIncome % 1.0 == 0.0) String.format("%.0f", totalIncome) else String.format("%.2f", totalIncome)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        
                        // Expense Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Expenses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF44336)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "-$currency${if (totalExpense % 1.0 == 0.0) String.format("%.0f", totalExpense) else String.format("%.2f", totalExpense)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
            }
            


            // Transactions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRANSACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions this month. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(recentTransactions.size) { index ->
                    val transaction = recentTransactions[index]
                    val sourceAccountName = allAccounts.find { it.id == transaction.accountId }?.name ?: "No Account"
                    val targetAccountName = allAccounts.find { it.id == transaction.transferTargetAccountId }?.name
                    val loanName = if (transaction.loanId != null) {
                        loans.find { it.id == transaction.loanId }?.name
                    } else null
                    
                    val isIncome = transaction.transactionType == com.h2.wellspend.data.TransactionType.INCOME
                    val isTransfer = transaction.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER
                    
                    // Check if this is a balance adjustment (non-editable)
                    val isBalanceAdjustment = transaction.category == Category.BalanceAdjustment
                    
                    // Display text for description: "Category: Description" format
                    val displayDesc = when {
                        isTransfer && targetAccountName != null -> "Transfer: $sourceAccountName → $targetAccountName"
                        isTransfer -> "Transfer: $sourceAccountName → ?"
                        loanName != null -> transaction.description.ifEmpty { transaction.category.name }
                        isBalanceAdjustment -> transaction.description.ifEmpty { transaction.category.name }
                        transaction.description.isNotEmpty() -> "${transaction.category.name}: ${transaction.description}"
                        else -> transaction.category.name
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(IntrinsicSize.Min)
                    ) {
                        // Background Actions
                        Box(modifier = Modifier.matchParentSize()) {
                            // Left Action (Edit)
                            if (!isBalanceAdjustment) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(actionWidth + 24.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { onEdit(transaction) },
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
                            }
                            
                            // Right Action (Delete)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(actionWidth + 24.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .clickable { showDeleteDialog = true },
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
                                            val maxOffset = if (isBalanceAdjustment) 0f else actionWidthPx
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
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    displayDesc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
            }
            
            // Footer showing end of transactions
            if (recentTransactions.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No more transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
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
    expenses: List<com.h2.wellspend.data.Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    state: LazyListState,
    chartData: List<ChartData>,
    totalSpend: Double
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
            state = state,
            headerContent = {
                // Donut Chart at the top (scrollable)
                DonutChart(
                    data = chartData,
                    totalAmount = totalSpend,
                    currency = currency,
                    onCenterClick = { /* Already on details page */ }
                )
            }
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
