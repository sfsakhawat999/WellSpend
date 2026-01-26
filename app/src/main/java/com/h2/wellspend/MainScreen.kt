package com.h2.wellspend

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import com.h2.wellspend.data.Loan
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
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text

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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.SystemCategory
import com.h2.wellspend.ui.CategoryColors
import com.h2.wellspend.ui.components.AddExpenseForm
import com.h2.wellspend.ui.components.DateSelector
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import com.h2.wellspend.ui.components.TransactionItem


import com.h2.wellspend.ui.components.LoanScreen
import com.h2.wellspend.data.Budget
import androidx.compose.material3.LinearProgressIndicator
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
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip

enum class Screen {
    HOME, ACCOUNTS, INCOME, EXPENSES
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var defaultTransactionType by remember { mutableStateOf<com.h2.wellspend.data.TransactionType?>(null) }
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
    var showCategoryManagement by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    var loanTransactionToEdit by remember { mutableStateOf<Expense?>(null) }
    
    // Account Input State: Wrapper to handle "Add" (null content) vs "Edit" (account content)
    // If showAccountInput is true, we show the screen. The content depends on accountToEdit.
    var showAccountInput by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    // Data
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val budgets by viewModel.budgets.collectAsState(initial = emptyList())
    val currency by viewModel.currency.collectAsState(initial = "$")
    val usedCategoryNames by viewModel.usedCategoryNames.collectAsState(initial = emptySet())
    val themeMode by viewModel.themeMode.collectAsState(initial = "SYSTEM")
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = false)
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val loans by viewModel.loans.collectAsState(initial = emptyList())
    val balances by viewModel.accountBalances.collectAsState(initial = emptyMap())
    val excludeLoanTransactions by viewModel.excludeLoanTransactions.collectAsState()
    val showAccountsOnHomepage by viewModel.showAccountsOnHomepage.collectAsState()
    val allCategories by viewModel.categoryOrder.collectAsState(initial = emptyList())

    // Hoisted States for Sub-screens
    // Budgets
    val localBudgetLimits = remember { mutableStateMapOf<Category, String>() }
    LaunchedEffect(showBudgets, budgets) {
        if (showBudgets) {
            // Reset/Init draft logic when opening
            viewModel.categoryOrder.value.forEach { cat ->
                val existing = budgets.find { it.category == cat.name }
                localBudgetLimits[cat] = existing?.limitAmount?.toString() ?: ""
            }
        }
    }

    // Loans
    var isCreatingLoan by remember { mutableStateOf(false) }
    var editingLoan by remember { mutableStateOf<com.h2.wellspend.data.Loan?>(null) }
    var loanForTransaction by remember { mutableStateOf<com.h2.wellspend.data.Loan?>(null) }

    // Monthly Report
    var showReportCompareDialog by remember { mutableStateOf(false) }
    var reportComparisonDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

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

    val canNavigateBack = showAddExpense || showReport || showBudgets || showSettings || showTransfers || showLoans || showCategoryManagement || showAccountInput || loanTransactionToEdit != null || currentScreen != Screen.HOME || (showLoans && (isCreatingLoan || editingLoan != null || loanForTransaction != null))
    androidx.activity.compose.BackHandler(enabled = canNavigateBack) {
        when {
            loanTransactionToEdit != null -> loanTransactionToEdit = null
            showAddExpense -> { showAddExpense = false; defaultTransactionType = null }
            showAccountInput -> { showAccountInput = false; accountToEdit = null }
            showReport -> showReport = false
            showBudgets -> showBudgets = false
            showSettings -> showSettings = false
            showTransfers -> showTransfers = false
            // Handle Loan Sub-screens
            showLoans && loanForTransaction != null -> loanForTransaction = null
            showLoans && (isCreatingLoan || editingLoan != null) -> { isCreatingLoan = false; editingLoan = null }
            showLoans -> showLoans = false
            showCategoryManagement -> showCategoryManagement = false
            currentScreen != Screen.HOME -> currentScreen = Screen.HOME
        }
    }

    val currentMonthTransactions = expenses.filter {
        val date = try { LocalDate.parse(it.date.take(10)) } catch(e:Exception) { LocalDate.now() }
        date.month == currentDate.month && date.year == currentDate.year
    }.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.timestamp })

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
        val categoryObj = allCategories.find { it.name == cat }
        ChartData(
            name = cat,
            value = list.sumOf { it.amount }, // Fees are handled separately
            color = categoryObj?.let { Color(it.color) } ?: Color.Gray
        )
    }.toMutableList()

    // Add ALL fees (from Income, Transfer, and Expense) as a single "Transaction Fee" slice
    val totalFees = validTransactions.sumOf { it.feeAmount }
    if (totalFees > 0) {
        val txFeeCat = allCategories.find { it.name == SystemCategory.TransactionFee.name }
        chartDataList.add(
            ChartData(
                name = SystemCategory.TransactionFee.name,
                value = totalFees,
                color = txFeeCat?.let { Color(it.color) } ?: Color.Gray
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

    val isBottomBarVisible = !showAddExpense && !showReport && !showBudgets && !showSettings && !showTransfers && !showLoans && !showAccountInput && !showCategoryManagement && loanTransactionToEdit == null

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
        val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
        
        LaunchedEffect(isBottomBarVisible) {
            if (!isBottomBarVisible) {
                // Delay to match the exit animation duration so it doesn't flash white while sliding down
                delay(300)
                window.navigationBarColor = backgroundColor
            } else {
                // Immediate update when showing
                window.navigationBarColor = surfaceVariantColor
            }
        }
    }

    // Cache states for exit animations to prevent layout shifts
    var lastExpenseToEdit by remember { mutableStateOf<Expense?>(null) }
    if (showAddExpense) lastExpenseToEdit = expenseToEdit

    var lastAccountToEdit by remember { mutableStateOf<Account?>(null) }
    if (showAccountInput) lastAccountToEdit = accountToEdit

    var lastLoanTransactionToEdit by remember { mutableStateOf<Expense?>(null) }
    if (loanTransactionToEdit != null) lastLoanTransactionToEdit = loanTransactionToEdit

    var lastLoanForTransaction by remember { mutableStateOf<Loan?>(null) }
    if (loanForTransaction != null) lastLoanForTransaction = loanForTransaction
    
    var lastEditingLoan by remember { mutableStateOf<Loan?>(null) }
    if (isCreatingLoan || editingLoan != null) lastEditingLoan = editingLoan


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,


    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val targetState = when {
                showAddExpense -> "OVERLAY_ADD"
                showAccountInput -> "OVERLAY_ACCOUNT_INPUT"
                loanTransactionToEdit != null -> "OVERLAY_EDIT_LOAN_TRANSACTION"
                // Hoisted Loan States
                loanForTransaction != null -> "OVERLAY_LOANS_TRANSACTION"
                isCreatingLoan -> "OVERLAY_LOANS_ADD"
                editingLoan != null -> "OVERLAY_LOANS_EDIT"
                showLoans -> "OVERLAY_LOANS_LIST"
                
                showReport -> "OVERLAY_REPORT"
                showBudgets -> "OVERLAY_BUDGETS"
                showSettings -> "OVERLAY_SETTINGS"
                showTransfers -> "OVERLAY_TRANSFERS"
                showCategoryManagement -> "OVERLAY_CATEGORIES"
                else -> currentScreen.name
            }

            AnimatedContent(
                targetState = targetState,
                transitionSpec = {
                    val isOverlayTarget = targetState.startsWith("OVERLAY_")
                    val isOverlayInitial = initialState.startsWith("OVERLAY_")
                    
                    // Specific Logic for Loan Sub-screens
                    val isLoanSubScreenTarget = targetState == "OVERLAY_LOANS_ADD" || targetState == "OVERLAY_LOANS_EDIT" || targetState == "OVERLAY_LOANS_TRANSACTION"
                    val isLoanSubScreenInitial = initialState == "OVERLAY_LOANS_ADD" || initialState == "OVERLAY_LOANS_EDIT" || initialState == "OVERLAY_LOANS_TRANSACTION"
                    
                    if (isLoanSubScreenTarget && initialState == "OVERLAY_LOANS_LIST") {
                         // Push Sub-screen over List
                         slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300)) togetherWith
                         fadeOut(animationSpec = tween(200))
                    } else if (targetState == "OVERLAY_LOANS_LIST" && isLoanSubScreenInitial) {
                         // Pop Sub-screen
                         fadeIn(animationSpec = tween(300)) togetherWith
                         slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                    } else if (isOverlayTarget && !isOverlayInitial) {
                        // Entering an overlay (Push)
                        slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(200)) using SizeTransform(clip = false)
                    } else if (!isOverlayTarget && isOverlayInitial) {
                        // Exiting an overlay (Pop)
                        fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300)) using SizeTransform(clip = false)
                    } else {
                        // Tab changes or Overlay to Overlay (default fade/scale)
                        (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))) togetherWith
                        (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))) using SizeTransform(clip = false)
                    }
                },
                label = "MainContentTransition"
            ) { state ->
                Column(modifier = Modifier.fillMaxSize()) {
                    val title = when(state) {
                        "OVERLAY_ADD" -> if(lastExpenseToEdit != null) "Edit Expense" else "Add Expense"
                        "OVERLAY_REPORT" -> "Monthly Report"
                        "OVERLAY_BUDGETS" -> "Budgets"
                        "OVERLAY_SETTINGS" -> "Settings"
                        "OVERLAY_TRANSFERS" -> "Transfers"
                        "OVERLAY_LOANS_ADD" -> "New Loan"
                        "OVERLAY_LOANS_EDIT" -> "Edit Loan"
                        "OVERLAY_LOANS_TRANSACTION" -> "${lastLoanForTransaction?.name ?: ""} Transaction"
                        "OVERLAY_LOANS_LIST" -> "Loans"
                        "OVERLAY_CATEGORIES" -> "Categories"
                        "OVERLAY_ACCOUNT_INPUT" -> if(lastAccountToEdit != null) "Edit Account" else "Add Account"
                        "OVERLAY_EDIT_LOAN_TRANSACTION" -> "Edit Loan Transaction"
                        "HOME" -> "Dashboard"
                        "ACCOUNTS" -> "Accounts"
                        "INCOME" -> "Income"
                        "EXPENSES" -> "Expenses"
                        else -> "WellSpend"
                    }
                    
                    val isOverlay = state.startsWith("OVERLAY_")
                    
                    if (state != "OVERLAY_ADD") {
                        WellSpendTopAppBar(
                            title = title,
                            // Enable back if it's an overlay (except root tabs), OR if we are in a loan sub-screen
                            canNavigateBack = (isOverlay && state != "HOME" && state != "ACCOUNTS" && state != "INCOME" && state != "EXPENSES") || 
                                              (state.startsWith("OVERLAY_LOANS_") && state != "OVERLAY_LOANS_LIST"),
                            onBack = {
                                when(state) {
                                    "OVERLAY_EDIT_LOAN_TRANSACTION" -> loanTransactionToEdit = null
                                    "OVERLAY_ACCOUNT_INPUT" -> { showAccountInput = false; accountToEdit = null }
                                    "OVERLAY_REPORT" -> showReport = false
                                    "OVERLAY_BUDGETS" -> showBudgets = false
                                    "OVERLAY_SETTINGS" -> showSettings = false
                                    "OVERLAY_TRANSFERS" -> showTransfers = false
                                    "OVERLAY_LOANS_TRANSACTION" -> loanForTransaction = null
                                    "OVERLAY_LOANS_ADD", "OVERLAY_LOANS_EDIT" -> {
                                        isCreatingLoan = false
                                        editingLoan = null
                                    }
                                    "OVERLAY_LOANS_LIST" -> showLoans = false
                                    "OVERLAY_CATEGORIES" -> showCategoryManagement = false
                                }
                            },
                            actions = {
                                var showMenu by remember { mutableStateOf(false) }
                                when(state) {
                                    "OVERLAY_BUDGETS" -> {
                                        Button(
                                            onClick = {
                                                val newBudgets = localBudgetLimits.mapNotNull { (cat, limitStr) ->
                                                    val limit = limitStr.toDoubleOrNull()
                                                    if (limit != null && limit > 0) {
                                                        com.h2.wellspend.data.Budget(cat.name, limit)
                                                    } else null
                                                }
                                                viewModel.updateBudgets(newBudgets, currency)
                                                showBudgets = false
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) { Text("Save") }
                                    }
                                    "OVERLAY_REPORT" -> {
                                        IconButton(onClick = { showReportCompareDialog = true }) {
                                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Compare", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        val reportExpenses = remember(expenses, currentDate) {
                                            expenses.filter {
                                                val date = java.time.LocalDate.parse(it.date.substring(0, 10))
                                                date.month == currentDate.month && date.year == currentDate.year
                                            }
                                        }
                                        val localContext = androidx.compose.ui.platform.LocalContext.current
                                        IconButton(onClick = {
                                            try {
                                                 val csvHeader = "Date,Category,Type,Amount,Fee,Description,Recurring\n"
                                                 val csvData = reportExpenses.joinToString("\n") { "${it.date},${it.category},${it.transactionType},${it.amount},${it.feeAmount},${it.description},${it.isRecurring}" }
                                                 val csvContent = csvHeader + csvData
                                                 val fileName = "expenses_${currentDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM_yyyy"))}.csv"
                                                 val file = java.io.File(localContext.cacheDir, fileName)
                                                 file.writeText(csvContent)
                                                 val uri = androidx.core.content.FileProvider.getUriForFile(localContext, "com.h2.wellspend.fileprovider", file)
                                                 val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                     type = "text/csv"
                                                     putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                     addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                 }
                                                 localContext.startActivity(android.content.Intent.createChooser(sendIntent, "Export Expenses"))
                                            } catch(e: Exception) { e.printStackTrace() }
                                        }) { Icon(Icons.Default.Download, contentDescription = "Export") }
                                    }
                                    "OVERLAY_LOANS" -> { /* No Actions */ }
                                    "OVERLAY_ACCOUNT_INPUT", "OVERLAY_EDIT_LOAN_TRANSACTION" -> { /* No Actions */ }
                                    else -> {
                                        // Only show the menu on main root tabs
                                        if (state == "HOME" || state == "ACCOUNTS" || state == "INCOME" || state == "EXPENSES") {
                                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                                            DropdownMenu(
                                                expanded = showMenu, 
                                                onDismissRequest = { showMenu = false },
                                                modifier = Modifier.width(200.dp)
                                            ) {
                                                DropdownMenuItem(text = { Text("Budgets") }, onClick = { showMenu = false; showBudgets = true }, leadingIcon = { Icon(Icons.Default.BarChart, null) })
                                                DropdownMenuItem(text = { Text("Transfers") }, onClick = { showMenu = false; showTransfers = true }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, null) })
                                                DropdownMenuItem(text = { Text("Loans") }, onClick = { showMenu = false; showLoans = true }, leadingIcon = { Icon(Icons.Default.AttachMoney, null) })
                                                DropdownMenuItem(text = { Text("Categories") }, onClick = { showMenu = false; showCategoryManagement = true }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) })
                                                DropdownMenuItem(text = { Text("Monthly Report") }, onClick = { showMenu = false; showReport = true }, leadingIcon = { Icon(Icons.Default.Description, null) })
                                                DropdownMenuItem(text = { Text("Settings") }, onClick = { showMenu = false; showSettings = true }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                when (state) {
                    "OVERLAY_ADD" -> {
                        val categoryOrder by viewModel.categoryOrder.collectAsState()
                        AddExpenseForm(
                            currency = currency,
                            accounts = accounts,
                            accountBalances = balances,
                            categories = categoryOrder,
                            initialExpense = lastExpenseToEdit, // Use cached
                            initialTransactionType = defaultTransactionType,
                            onAdd = { amount, desc, cat, date, isRecurring, freq, type, accId, targetAccId, fee, feeName, note ->
                                if (lastExpenseToEdit != null) {
                                    viewModel.updateExpense(
                                        id = lastExpenseToEdit!!.id, 
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
                                        feeConfigName = feeName,
                                        note = note
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
                                        feeConfigName = feeName,
                                        note = note
                                    )
                                }
                                showAddExpense = false
                                defaultTransactionType = null
                            },
                            onCancel = { showAddExpense = false; defaultTransactionType = null },
                            onReorder = { viewModel.updateCategoryOrder(it) },
                            onAddCategory = { viewModel.addCategory(it) }
                        )
                    }
                    "OVERLAY_EDIT_LOAN_TRANSACTION" -> {
                         val transaction = lastLoanTransactionToEdit // Use cached
                         if (transaction != null) {
                             val relatedLoan = loans.find { it.id == transaction.loanId }
                             if (relatedLoan != null) {
                                 com.h2.wellspend.ui.components.EditLoanTransactionScreen(
                                    transaction = transaction,
                                    loan = relatedLoan,
                                    accounts = accounts,
                                    accountBalances = balances,
                                    currency = currency,
                                    onDismiss = { loanTransactionToEdit = null },
                                    onConfirm = { amt, desc, accId, fee, feeName, date ->
                                        viewModel.updateExpense(
                                            id = transaction.id,
                                            amount = amt,
                                            description = desc,
                                            category = allCategories.find { it.name == transaction.category },
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
                    }
                    "OVERLAY_REPORT" -> {
                        MonthlyReport(
                            expenses = expenses,
                            categories = allCategories,
                            currency = currency,
                            currentDate = currentDate,
                            onDateChange = { currentDate = it }, // Connected
                            showCompareDialog = showReportCompareDialog,
                            onDismissCompareDialog = { showReportCompareDialog = false },
                            comparisonDate = reportComparisonDate,
                            onComparisonDateChange = { reportComparisonDate = it }
                        )
                    }
                    "OVERLAY_BUDGETS" -> {
                         val categories by viewModel.categoryOrder.collectAsState()
                         BudgetScreen(
                            categories = categories,
                            currency = currency,
                            draftBudgets = localBudgetLimits
                         )
                    }
                    "OVERLAY_TRANSFERS" -> {
                        TransferListScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            expenses = currentMonthTransactions,
                            accounts = accounts,
                            currency = currency,

                            onDelete = { viewModel.deleteExpense(it) },
                            onEdit = { 
                                expenseToEdit = it
                                showAddExpense = true
                            }
                        )
                    }
                    "OVERLAY_LOANS_TRANSACTION" -> {
                        val loan = lastLoanForTransaction // Use cached
                        if (loan != null) {
                            com.h2.wellspend.ui.components.AddLoanTransactionScreen(
                                loan = loan,
                                accounts = accounts,
                                accountBalances = balances,
                                currency = currency,
                                onDismiss = { loanForTransaction = null },
                                onConfirm = { amount, isPayment, accId, fee, feeName, date ->
                                    viewModel.addLoanTransaction(loan.id, loan.name, amount, isPayment, accId, loan.type, fee, feeName, date)
                                    loanForTransaction = null
                                }
                            )
                        } else { Box(Modifier.fillMaxSize()) }
                    }
                    "OVERLAY_LOANS_ADD", "OVERLAY_LOANS_EDIT" -> {
                        com.h2.wellspend.ui.components.LoanInputScreen(
                            initialLoan = lastEditingLoan, // Use cached
                            accounts = accounts,
                            accountBalances = balances,
                            currency = currency,
                            onSave = { name, amount, type, desc, accId, fee, feeName, date ->
                                if (lastEditingLoan != null) {
                                    viewModel.updateLoan(lastEditingLoan!!.copy(name = name, amount = amount, description = desc, type = type))
                                    editingLoan = null
                                } else {
                                    viewModel.addLoan(name, amount, type, desc, accId, fee, feeName, date)
                                    isCreatingLoan = false
                                }
                            },
                            onCancel = {
                                isCreatingLoan = false
                                editingLoan = null
                            }
                        )
                    }
                    "OVERLAY_LOANS_LIST" -> {
                        // Pass empty lambdas for things now handled by MainScreen
                        LoanScreen(
                            loans = loans,
                            expenses = expenses,
                            accounts = accounts,
                            currency = currency,
                            onTransactionClick = { loanForTransaction = it },
                            onEditLoan = { editingLoan = it },
                            onDeleteLoan = { loan, deleteTransactions -> viewModel.deleteLoan(loan, deleteTransactions) },
                            onEditTransaction = { expense -> 
                                expenseToEdit = expense
                                showAddExpense = true
                            },
                            onDeleteTransaction = { expenseId -> viewModel.deleteExpense(expenseId) },
                            onAddLoanStart = { isCreatingLoan = true }
                        )
                    }
                    "OVERLAY_SETTINGS" -> {
                            SettingsScreen(
                                currentCurrency = currency,
                                currentThemeMode = themeMode,
                                currentDynamicColor = dynamicColor,
                                excludeLoanTransactions = excludeLoanTransactions,
                                showAccountsOnHomepage = showAccountsOnHomepage,
                                onCurrencyChange = { newCurrency ->
                                     viewModel.updateBudgets(emptyList(), newCurrency)
                                },
                                onThemeModeChange = { viewModel.updateThemeMode(it) },
                                onDynamicColorChange = { viewModel.updateDynamicColor(it) },
                                onExcludeLoanTransactionsChange = { viewModel.updateExcludeLoanTransactions(it) },
                                onShowAccountsOnHomepageChange = { viewModel.updateShowAccountsOnHomepage(it) },
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
                            .sortedByDescending { it.date }
                        
                        DashboardScreen(
                            currentDate = currentDate,
                            onDateChange = { currentDate = it },
                            currency = currency,
                            totalBalance = monthEndBalance,
                            totalIncome = totalIncome,
                            totalExpense = totalSpend,
                            recentTransactions = allMonthTransactions,
                            allAccounts = accounts,
                            accountBalances = balances,
                            loans = loans,
                            showAccounts = showAccountsOnHomepage,
                            isLoading = !isDataLoaded,
                            onEdit = { transaction ->
                                if (transaction.loanId != null) {
                                    loanTransactionToEdit = transaction
                                } else {
                                    expenseToEdit = transaction
                                    showAddExpense = true
                                }
                            },
                            onDelete = { id -> viewModel.deleteExpense(id) },
                            onBalanceClick = { currentScreen = Screen.ACCOUNTS },
                            onIncomeClick = { currentScreen = Screen.INCOME },
                            onExpenseClick = { currentScreen = Screen.EXPENSES },
                            onAccountClick = { currentScreen = Screen.ACCOUNTS },
                            categories = allCategories
                        )
                    }
                    "OVERLAY_ACCOUNT_INPUT" -> {
                        com.h2.wellspend.ui.AccountInputScreen(
                            account = lastAccountToEdit, // Use cached
                            currentBalance = if (lastAccountToEdit != null) balances[lastAccountToEdit!!.id] else null,
                            currency = currency,

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

                    "OVERLAY_CATEGORIES" -> {
                        com.h2.wellspend.ui.components.CategoryManagementScreen(
                            categories = allCategories,
                            onAddCategory = { viewModel.addCategory(it) },
                            onUpdateCategory = { viewModel.addCategory(it) }, 
                            onDeleteCategory = { viewModel.deleteCategory(it) },
                            usedCategoryNames = usedCategoryNames,
                            showAddDialog = showAddCategoryDialog,
                            onDismissAddDialog = { showAddCategoryDialog = false }
                        )
                    }
                    "ACCOUNTS" -> {
                        com.h2.wellspend.ui.AccountScreen(

                            accounts = accounts,
                            balances = balances,
                            currency = currency,
                            onDeleteAccount = { viewModel.deleteAccount(it) },
                            onReorder = { viewModel.updateAccountOrder(it) },
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
                            expenses = currentMonthTransactions,
                            categories = allCategories,
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
                            totalSpend = totalSpend,
                            budgets = budgets
                        )
                    }
                     "MORE" -> {
                          // Deprecated
                          Box(modifier = Modifier.fillMaxSize())
                     }
                }
            }
                    }
                }

            // Floating Bottom Bar
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                 AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically(animationSpec = tween(300)) { it },
                    exit = slideOutVertically(animationSpec = tween(300)) { it }
                ) {
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
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Expenses") },
                            label = { Text("Expenses") }
                        )
                    }
                }
            }



            // Floating Action Buttons (Custom Positioning)
            Box(modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (isBottomBarVisible) 90.dp else 24.dp, end = 24.dp)
            ) {
                if ((currentScreen == Screen.HOME || currentScreen == Screen.EXPENSES || currentScreen == Screen.INCOME || currentScreen == Screen.ACCOUNTS || currentScreen == Screen.ACCOUNTS || showTransfers || showLoans) && 
                    !showAddExpense && !showReport && !showBudgets && !showSettings && !showAccountInput && loanTransactionToEdit == null && !showCategoryManagement) {
                        // Logic for deciding which FAB to show
                        // If standard add expense/transaction
                        if (!showLoans || (showLoans && !isCreatingLoan && editingLoan == null && loanForTransaction == null)) {
                             FloatingActionButton(
                                onClick = { 
                                    if(showLoans) {
                                         isCreatingLoan = true
                                    } else if (showTransfers) {
                                         // Transfer FAB if needed, though Transfers page usually has its own
                                         expenseToEdit = null
                                         defaultTransactionType = com.h2.wellspend.data.TransactionType.TRANSFER
                                         showAddExpense = true
                                    } else if (currentScreen == Screen.ACCOUNTS) {
                                         accountToEdit = null
                                         showAccountInput = true
                                    } else if (currentScreen == Screen.INCOME) {
                                         expenseToEdit = null
                                         defaultTransactionType = com.h2.wellspend.data.TransactionType.INCOME
                                         showAddExpense = true
                                    } else {
                                        expenseToEdit = null
                                        defaultTransactionType = com.h2.wellspend.data.TransactionType.EXPENSE
                                        showAddExpense = true
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                } else if (showCategoryManagement && !showAddExpense && !showAccountInput && !showReport && !showBudgets && !showSettings && !showLoans) {
                      FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                         Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
            }

            // Dialog for Editing Loan Transaction
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellSpendTopAppBar(
    title: String,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { 
            Text(
                title, 
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold 
            ) 
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

// DateSelector moved to ui/components/DateSelector.kt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(

    currentDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    currency: String,
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    recentTransactions: List<com.h2.wellspend.data.Expense>,
    allAccounts: List<com.h2.wellspend.data.Account>,
    accountBalances: Map<String, Double>,
    loans: List<com.h2.wellspend.data.Loan>,
    showAccounts: Boolean,
    isLoading: Boolean = false,
    onEdit: (com.h2.wellspend.data.Expense) -> Unit,
    onDelete: (String) -> Unit,
    onBalanceClick: () -> Unit,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    categories: List<com.h2.wellspend.data.Category>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DateSelector(
            currentDate = currentDate,
            onDateChange = onDateChange
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
                        .height(84.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(16.dp))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(16.dp))
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Accounts skeleton
                if (showAccounts) {
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
                }
                
                // Transactions skeleton
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), RoundedCornerShape(4.dp))
                    )
                }
                repeat(5) { index ->
                    val shape = when(index) {
                        0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                        4 -> RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(3.dp)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(vertical = 1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha), shape)
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
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onBalanceClick() }
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
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onIncomeClick() }
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
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onExpenseClick() }
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
            


            // Accounts Section (Conditional)
            if (showAccounts) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ACCOUNTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(allAccounts.size) { index ->
                                val account = allAccounts[index]
                                val balance = accountBalances[account.id] ?: account.initialBalance
                                
                                Card(
                                    modifier = Modifier
                                        .widthIn(min = 140.dp)
                                        .clickable { onAccountClick(account.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                       Box(
                                           modifier = Modifier
                                               .size(40.dp)
                                               .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                           contentAlignment = Alignment.Center
                                       ) {
                                           Icon(
                                               Icons.Default.AccountBalanceWallet,
                                               contentDescription = null,
                                               tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                               modifier = Modifier.size(24.dp)
                                           )
                                       }
                                       Spacer(modifier = Modifier.width(12.dp))
                                       Column {
                                           Text(
                                               text = account.name,
                                               style = MaterialTheme.typography.labelMedium,
                                               color = MaterialTheme.colorScheme.onSurfaceVariant
                                           )
                                           Spacer(modifier = Modifier.height(2.dp))
                                           Text(
                                                text = "$currency${String.format("%.2f", balance)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                           )
                                       }
                                    } 
                                }
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
                    
                    val shape = when {
                        recentTransactions.size == 1 -> RoundedCornerShape(16.dp)
                        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                        index == recentTransactions.lastIndex -> RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(3.dp)
                    }
                    
                    val backgroundShape = when {
                        recentTransactions.size == 1 -> RoundedCornerShape(17.dp)
                        index == 0 -> RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        index == recentTransactions.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 17.dp, bottomEnd = 17.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                    
                    val paddingModifier = Modifier.padding(
                        horizontal = 16.dp, 
                        vertical = 1.dp
                    )

                    com.h2.wellspend.ui.components.TransactionItem(
                        transaction = transaction,
                        accounts = allAccounts,
                        loans = loans,
                        categories = categories,
                        currency = currency,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        modifier = paddingModifier,
                        shape = shape,
                        backgroundShape = backgroundShape
                    )

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
    expenses: List<com.h2.wellspend.data.Expense>,
    categories: List<com.h2.wellspend.data.Category>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    state: LazyListState,
    chartData: List<ChartData>,
    totalSpend: Double,
    budgets: List<Budget>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DateSelector(
            currentDate = currentDate,
            onDateChange = onDateChange
        )
        
        // Show Expenses ONLY (Transfers -> More > Transfers, Income -> Bottom Tab)
        val expenseList = expenses.filter { 
            it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE &&
            !(it.loanId != null && it.accountId == null)
        }
        ExpenseList(
            expenses = expenseList,
            categories = categories,
            accounts = accounts,
            loans = loans,
            currency = currency,
            budgets = budgets,
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
    expenses: List<com.h2.wellspend.data.Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DateSelector(
            currentDate = currentDate,
            onDateChange = onDateChange
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
    expenses: List<com.h2.wellspend.data.Expense>,
    accounts: List<com.h2.wellspend.data.Account>,
    loans: List<com.h2.wellspend.data.Loan>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DateSelector(
            currentDate = currentDate,
            onDateChange = onDateChange
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
