package com.h2.wellspend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2.wellspend.data.Budget
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.RecurringConfig
import com.h2.wellspend.data.RecurringFrequency
import com.h2.wellspend.data.WellSpendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import com.h2.wellspend.data.SystemCategory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.toArgb

import java.util.UUID

class MainViewModel(
    private val repository: WellSpendRepository,
    initialThemeMode: String? = null,
    initialDynamicColor: Boolean = false,
    initialOnboardingCompleted: Boolean = false
) : ViewModel() {


    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recurringConfigs = repository.recurringConfigs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val loans = repository.loans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    enum class SearchField {
        ALL, TITLE, NOTE, AMOUNT, CATEGORY
    }

    enum class SortOption {
        DATE, AMOUNT, TITLE
    }

    enum class SortOrder {
        ASC, DESC
    }

    data class SearchFilter(
        val type: com.h2.wellspend.data.TransactionType? = null, // null = All
        val startDate: LocalDate? = null,
        val endDate: LocalDate? = null,
        val searchField: SearchField = SearchField.ALL,
        val sortOption: SortOption = SortOption.DATE,
        val sortOrder: SortOrder = SortOrder.DESC
    )
    
    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter = _searchFilter.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchFilter())

    val searchResults: StateFlow<List<Expense>> = combine(
        expenses,
        _searchQuery,
        _searchFilter
    ) { allExpenses, query, filter ->
        if (query.isBlank() && filter.type == null && filter.startDate == null && filter.endDate == null) {
            emptyList()
        } else {
            allExpenses.filter { expense ->
                // Query Match based on selected field
                val queryMatch = if (query.isBlank()) true else {
                    when (filter.searchField) {
                        SearchField.ALL -> {
                            expense.title.contains(query, ignoreCase = true) ||
                            (expense.note?.contains(query, ignoreCase = true) == true) ||
                            (expense.amount.toInt().toString().contains(query)) ||
                            (expense.category.contains(query, ignoreCase = true))
                        }
                        SearchField.TITLE -> expense.title.contains(query, ignoreCase = true)
                        SearchField.NOTE -> expense.note?.contains(query, ignoreCase = true) == true
                        SearchField.AMOUNT -> expense.amount.toInt().toString().contains(query)
                        SearchField.CATEGORY -> expense.category.contains(query, ignoreCase = true)
                    }
                }
                
                // Type Match
                val typeMatch = filter.type == null || expense.transactionType == filter.type
                
                // Date Match
                val dateMatch = try {
                    val expDate = LocalDate.parse(expense.date.take(10))
                    val startMatch = filter.startDate?.let { !expDate.isBefore(it) } ?: true
                    val endMatch = filter.endDate?.let { !expDate.isAfter(it) } ?: true
                    startMatch && endMatch
                } catch (e: Exception) {
                    true // If date parse fails, include it (or exclude, depending on policy. Including is safer)
                }
                
                queryMatch && typeMatch && dateMatch
            }.let { filtered ->
                // Apply sorting with order
                when (filter.sortOption) {
                    SortOption.DATE -> if (filter.sortOrder == SortOrder.DESC) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
                    SortOption.AMOUNT -> if (filter.sortOrder == SortOrder.DESC) filtered.sortedByDescending { it.amount } else filtered.sortedBy { it.amount }
                    SortOption.TITLE -> if (filter.sortOrder == SortOrder.DESC) filtered.sortedByDescending { it.title.lowercase() } else filtered.sortedBy { it.title.lowercase() }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }



// ... (existing code) ...

    fun addLoan(
        name: String,
        amount: Double,
        type: com.h2.wellspend.data.LoanType,
        description: String?,
        accountId: String?,
        feeAmount: Double,
        feeConfigName: String?,
        date: java.time.LocalDate
    ) {
        viewModelScope.launch {
            val loanId = UUID.randomUUID().toString()
            val loan = com.h2.wellspend.data.Loan(
                id = loanId,
                name = name,
                type = type,
                amount = amount,
                description = description
            )
            repository.addLoan(loan)

            // Initial Transaction
            // If Account Selected:
            //   LEND -> EXPENSE (Money Out)
            //   BORROW -> INCOME (Money In)
            // If No Account: Still create transaction for history, but accountId=null
            
            val transactionType = if (type == com.h2.wellspend.data.LoanType.LEND) {
                com.h2.wellspend.data.TransactionType.EXPENSE
            } else {
                com.h2.wellspend.data.TransactionType.INCOME
            }

            val expense = Expense(
                amount = amount,
                title = "New Loan: $name",
                category = SystemCategory.Loan.name,
                date = date.atStartOfDay().toString(),
                timestamp = System.currentTimeMillis(),
                transactionType = transactionType,
                accountId = accountId,
                loanId = loanId,
                feeAmount = feeAmount,
                feeConfigName = feeConfigName
            )
            repository.addExpense(expense)
        }
    }

    fun updateLoan(loan: com.h2.wellspend.data.Loan) {
        viewModelScope.launch {
             repository.addLoan(loan)
        }
    }

    fun reorderAccounts(accounts: List<com.h2.wellspend.data.Account>) {
        viewModelScope.launch {
            val updatedAccounts = accounts.mapIndexed { index, account ->
                account.copy(sortOrder = index)
            }
            repository.updateAccountOrders(updatedAccounts)
        }
    }

    fun addLoanTransaction(
        loanId: String,
        loanName: String,
        amount: Double,
        isPayment: Boolean, // True = Pay/Receive, False = Increase Loan
        accountId: String?,
        loanType: com.h2.wellspend.data.LoanType,
        feeAmount: Double,
        feeConfigName: String?,
        date: java.time.LocalDate,
        note: String? = null
    ) {
        viewModelScope.launch {
            try {
                // Logic:
                // LEND (Asset):
                //   Increase -> I give more money -> EXPENSE
                //   Pay (Receive) -> I get money back -> INCOME
                // BORROW (Liability):
                //   Increase -> I borrow more -> INCOME
                //   Pay (Repay) -> I pay back -> EXPENSE
                
                val transactionType = if (loanType == com.h2.wellspend.data.LoanType.LEND) {
                    if (isPayment) com.h2.wellspend.data.TransactionType.INCOME else com.h2.wellspend.data.TransactionType.EXPENSE
                } else {
                    if (isPayment) com.h2.wellspend.data.TransactionType.EXPENSE else com.h2.wellspend.data.TransactionType.INCOME
                }
                
                val desc = if (isPayment) "Loan Payment: $loanName" else "Loan Increase: $loanName"

                val expense = Expense(
                    amount = amount,
                    title = desc,
                    category = SystemCategory.Loan.name,
                    date = date.atStartOfDay().toString(),
                    timestamp = System.currentTimeMillis(),
                    transactionType = transactionType,
                    accountId = accountId,
                    loanId = loanId,
                    feeAmount = feeAmount,
                    feeConfigName = feeConfigName,
                    note = note
                )
                repository.addExpense(expense)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteLoan(loan: com.h2.wellspend.data.Loan, deleteTransactions: Boolean = false) {
        viewModelScope.launch {
            val allExpenses = repository.getAllExpensesOneShot()
            val linkedExpenses = allExpenses.filter { it.loanId == loan.id }
            
            if (deleteTransactions) {
                // Delete all linked transactions (this automatically reverts balances)
                if (linkedExpenses.isNotEmpty()) {
                    repository.deleteExpenses(linkedExpenses)
                }
            } else {
                // Just unlink transactions (orphaned history)
                val expensesToUpdate = linkedExpenses.map { 
                    it.copy(loanId = null, title = "${it.title} (Deleted Loan: ${loan.name})") 
                }
                
                if (expensesToUpdate.isNotEmpty()) {
                    repository.addExpenses(expensesToUpdate)
                }
            }
            
            repository.deleteLoan(loan)
        }
    }

    
    val currency: StateFlow<String> = repository.currency
        .map { it ?: "$" } // Default to $ if null
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    private val _optimisticUpdates = kotlinx.coroutines.flow.MutableStateFlow<List<Category>?>(null)

    val categoryOrder: StateFlow<List<Category>> = kotlinx.coroutines.flow.combine(
        repository.sortedCategories,
        _optimisticUpdates
    ) { repo, optimistic ->
        (optimistic ?: repo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usedCategoryNames: StateFlow<Set<String>> = expenses.map { list -> 
        list.map { it.category }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Calculate Balances
    val accountBalances: StateFlow<Map<String, Double>> = kotlinx.coroutines.flow.combine(
        accounts,
        expenses
    ) { accs, exps ->
        accs.associate { account ->
            val initial = account.initialBalance
            val accountExpenses = exps.filter { it.accountId == account.id && it.transactionType == com.h2.wellspend.data.TransactionType.EXPENSE }
            val transfersOut = exps.filter { it.accountId == account.id && it.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER }
            val transfersIn = exps.filter { it.transferTargetAccountId == account.id && it.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER }
            val incomes = exps.filter { it.accountId == account.id && it.transactionType == com.h2.wellspend.data.TransactionType.INCOME }
            
            val totalExpense = accountExpenses.sumOf { it.amount + it.feeAmount }
            val totalTransferOut = transfersOut.sumOf { it.amount + it.feeAmount }
            val totalIncome = incomes.sumOf { it.amount }
            val totalIncomeFee = incomes.sumOf { it.feeAmount }
            val totalIncomeNet = totalIncome - totalIncomeFee

            val totalTransferIn = transfersIn.sumOf { it.amount }
            
            val balance = initial + totalIncomeNet - totalExpense - totalTransferOut + totalTransferIn
            account.id to balance
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            repository.ensureCategoriesInitialized()
        }
        // Clear optimistic update when repository catches up
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(repository.sortedCategories, _optimisticUpdates) { repo, opt ->
                repo to opt
            }.collect { (repo, opt) ->
                if (opt != null && repo == opt) {
                    _optimisticUpdates.value = null
                }
            }
        }
    }
    val themeMode: StateFlow<String> = repository.themeMode
        .map { it ?: "SYSTEM" } // Default to SYSTEM
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialThemeMode ?: "SYSTEM")

    val dynamicColor: StateFlow<Boolean> = repository.dynamicColor
        .map { it?.toBoolean() ?: true } // Default to true
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialDynamicColor)

    val excludeLoanTransactions: StateFlow<Boolean> = repository.excludeLoanTransactions
        .map { it?.toBoolean() ?: false } // Default to false
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val onboardingCompleted: StateFlow<Boolean> = repository.onboardingCompleted
        .map { it?.toBoolean() ?: false } // Default to false
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialOnboardingCompleted)

    val showAccountsOnHomepage: StateFlow<Boolean> = repository.showAccountsOnHomepage
        .map { it?.toBoolean() ?: false } // Default to false
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkRecurringExpenses()
    }

    private fun checkRecurringExpenses() {
        viewModelScope.launch {
            val configs = repository.getRecurringConfigsOneShot()
            val now = LocalDate.now()
            val newExpenses = mutableListOf<Expense>()
            val updatedConfigs = mutableListOf<RecurringConfig>()

            configs.forEach { config ->
                var nextDate = LocalDate.parse(config.nextDueDate.substring(0, 10))
                var configChanged = false

                while (nextDate <= now) {
                    configChanged = true
                    newExpenses.add(
                        Expense(
                            id = UUID.randomUUID().toString(),
                            amount = config.amount,
                            category = config.category,
                            title = "${config.title} (Recurring)",
                            date = nextDate.atStartOfDay().toString(), // ISO-ish
                            timestamp = System.currentTimeMillis(),
                            // isRecurring removed
                            transactionType = config.transactionType,
                            accountId = config.accountId,
                            transferTargetAccountId = config.transferTargetAccountId,
                            feeAmount = config.feeAmount,
                            feeConfigName = config.feeConfigName
                        )
                    )

                    nextDate = if (config.frequency == RecurringFrequency.WEEKLY) {
                        nextDate.plusWeeks(1)
                    } else {
                        nextDate.plusMonths(1)
                    }
                }

                if (configChanged) {
                    updatedConfigs.add(config.copy(nextDueDate = nextDate.toString()))
                }
            }

            if (newExpenses.isNotEmpty()) {
                repository.addExpenses(newExpenses)
                repository.updateRecurringConfigs(updatedConfigs)
            }
        }
    }

    fun addExpense(
        amount: Double, 
        title: String, 
        category: Category?, 
        date: String, 
        isRecurring: Boolean, 
        frequency: RecurringFrequency,
        transactionType: com.h2.wellspend.data.TransactionType,
        accountId: String?,
        targetAccountId: String?,

        feeAmount: Double,
        feeConfigName: String?,
        note: String? = null
    ) {
        viewModelScope.launch {
            val categoryName = category?.name ?: SystemCategory.Others.name
            val expense = Expense(
                amount = amount,
                title = title,
                category = categoryName,
                date = date, // Should be ISO string
                timestamp = System.currentTimeMillis(),
                // isRecurring removed
                transactionType = transactionType,
                accountId = accountId,
                transferTargetAccountId = targetAccountId,
                feeAmount = feeAmount,
                feeConfigName = feeConfigName,
                note = note
            )
            repository.addExpense(expense)

            if (isRecurring) {
                val startDate = LocalDate.parse(date.substring(0, 10))
                val nextDate = if (frequency == RecurringFrequency.WEEKLY) startDate.plusWeeks(1) else startDate.plusMonths(1)
                
                val config = RecurringConfig(
                    amount = amount,
                    category = categoryName,
                    title = title,
                    note = note,
                    frequency = frequency,
                    nextDueDate = nextDate.toString(),
                    transactionType = transactionType,
                    accountId = accountId,
                    transferTargetAccountId = targetAccountId,
                    feeAmount = feeAmount,
                    feeConfigName = feeConfigName
                )
                repository.addRecurringConfig(config)
            }
        }
    }

    fun updateExpense(
        id: String, 
        amount: Double, 
        title: String, 
        category: Category?, 
        date: String, 
        isRecurring: Boolean, 
        frequency: RecurringFrequency,
        transactionType: com.h2.wellspend.data.TransactionType,
        accountId: String?,
        targetAccountId: String?,
        feeAmount: Double,

        feeConfigName: String?,
        loanId: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            val categoryName = category?.name ?: SystemCategory.Others.name
            val expense = Expense(
                id = id,
                amount = amount,
                title = title,
                category = categoryName,
                date = date,
                timestamp = System.currentTimeMillis(), 
                // isRecurring removed
                transactionType = transactionType,
                accountId = accountId,
                transferTargetAccountId = targetAccountId,
                feeAmount = feeAmount,
                feeConfigName = feeConfigName,
                loanId = loanId,
                note = note
            )
            repository.addExpense(expense) // Room's Insert(onConflict = REPLACE) handles update

            if (isRecurring) {
                 val startDate = LocalDate.parse(date.substring(0, 10))
                val nextDate = if (frequency == RecurringFrequency.WEEKLY) startDate.plusWeeks(1) else startDate.plusMonths(1)
                
                val config = RecurringConfig(
                    amount = amount,
                    category = categoryName,
                    title = title,
                    note = note,
                    frequency = frequency,
                    nextDueDate = nextDate.toString(),
                    transactionType = transactionType,
                    accountId = accountId,
                    transferTargetAccountId = targetAccountId,
                    feeAmount = feeAmount,
                    feeConfigName = feeConfigName
                )
                repository.addRecurringConfig(config)
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }

    // Recurring Config Methods
    fun updateRecurringConfig(config: RecurringConfig) {
        viewModelScope.launch {
            repository.addRecurringConfig(config) // Room REPLACE handles update
        }
    }

    fun deleteRecurringConfig(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringConfig(id)
        }
    }

    // Account Methods
    fun addAccount(account: com.h2.wellspend.data.Account) {
        viewModelScope.launch {
            repository.addAccount(account)
        }
    }
    
    fun deleteAccount(account: com.h2.wellspend.data.Account) {
        viewModelScope.launch {
            // Unlink expenses first
            val allExpenses = repository.getAllExpensesOneShot()
            val expensesToUpdate = mutableListOf<Expense>()
            val expensesToDelete = mutableListOf<Expense>()
            
            allExpenses.forEach { expense ->
                var modified = false
                var newExpense = expense
                
                // Case 1: Account is the source
                if (expense.accountId == account.id) {
                    // Start of request: "delete income histories"
                    if (expense.transactionType == com.h2.wellspend.data.TransactionType.INCOME) {
                        expensesToDelete.add(expense)
                        return@forEach // Skip update logic for this item
                    }
                    // End of request

                    // Default behavior: Unlink source
                    // If it was a Transfer, it remains a Transfer (from "null" source)
                    newExpense = newExpense.copy(accountId = null)
                    modified = true
                }
                
                // Case 2: Account is the target
                if (expense.transferTargetAccountId == account.id) {
                    newExpense = newExpense.copy(transferTargetAccountId = null)
                    modified = true
                    
                    // If it was a transfer, and target is gone:
                    // It implies money left Source to "Nowhere".
                    // Convert to EXPENSE for the source account.
                    if (expense.transactionType == com.h2.wellspend.data.TransactionType.TRANSFER) {
                        newExpense = newExpense.copy(transactionType = com.h2.wellspend.data.TransactionType.EXPENSE)
                    }
                }
                
                if (modified) {
                    expensesToUpdate.add(newExpense)
                }
            }
            
            if (expensesToDelete.isNotEmpty()) {
                repository.deleteExpenses(expensesToDelete)
            }

            if (expensesToUpdate.isNotEmpty()) {
                repository.addExpenses(expensesToUpdate)
            }

            repository.deleteAccount(account)
        }
    }

    fun updateAccount(account: com.h2.wellspend.data.Account) {
        addAccount(account)
    }

    fun addAdjustmentTransaction(accountId: String, accountName: String, adjustment: Double) {
        adjustAccountBalance(accountId, accountName, adjustment)
    }

    fun updateAccountOrder(accounts: List<com.h2.wellspend.data.Account>) {
        reorderAccounts(accounts)
    }

    fun adjustAccountBalance(accountId: String, accountName: String, adjustment: Double) {
        viewModelScope.launch {
            val amount = kotlin.math.abs(adjustment)
            val type = if (adjustment > 0) com.h2.wellspend.data.TransactionType.INCOME else com.h2.wellspend.data.TransactionType.EXPENSE
            
            val expense = Expense(
                amount = amount,
                title = "Balance Adjustment: $accountName",
                category = SystemCategory.BalanceAdjustment.name,
                date = LocalDate.now().atStartOfDay().toString(),
                timestamp = System.currentTimeMillis(),
                // isRecurring removed
                transactionType = type,
                accountId = accountId,
                feeAmount = 0.0
            )
            repository.addExpense(expense)
        }
    }

    fun updateBudgets(newBudgets: List<Budget>, newCurrency: String) {
        viewModelScope.launch {
            newBudgets.forEach { repository.setBudget(it) }
            repository.setCurrency(newCurrency)
        }
    }

    fun updateCategoryOrder(newOrder: List<Category>) {
        _optimisticUpdates.value = newOrder
        viewModelScope.launch {
            repository.updateCategoryOrder(newOrder)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }
    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColor(enabled)
        }
    }

    fun updateExcludeLoanTransactions(exclude: Boolean) {
        viewModelScope.launch {
            repository.setExcludeLoanTransactions(exclude)
        }
    }

    fun updateShowAccountsOnHomepage(show: Boolean) {
        viewModelScope.launch {
            repository.setShowAccountsOnHomepage(show)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
        }
    }

    fun exportData(uri: android.net.Uri, contentResolver: android.content.ContentResolver, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val expenses = repository.getAllExpensesOneShot()
                val budgets = repository.getAllBudgetsOneShot()
                val accounts = repository.getAllAccountsOneShot()
                val recurringConfigs = repository.getRecurringConfigsOneShot()
                val loans = repository.getAllLoansOneShot()
                val categories = repository.sortedCategories.first() // Get current visible categories
                val appData = com.h2.wellspend.data.AppData(expenses, budgets, accounts, recurringConfigs, loans, categories)
                
                val gson = com.google.gson.Gson()
                val jsonString = gson.toJson(appData)
                
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true, "Export successful")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importData(uri: android.net.Uri, contentResolver: android.content.ContentResolver, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    
                    val gson = com.google.gson.GsonBuilder()
                        .create()
                        
                    // Parse into ImportAppData which allows nullable new fields
                    val importData = gson.fromJson(jsonString, ImportAppData::class.java)
                    
                    if (importData != null) {
                        // Map nullable import objects to strict entities with defaults
                        val expenses = importData.expenses.map { it.toExpense() }
                        val budgets = importData.budgets // No change
                        val accounts = importData.accounts // Strict Account is fine if entire list is missing (null list handled in repo)
                        // But if list exists, items must be valid. Old data has no list.
                        // New data has list.
                        
                        val recurringConfigs = importData.recurringConfigs?.map { it.toRecurringConfig() }
                        val loans = importData.loans
                        var categories = importData.categories ?: emptyList()
                        
                        // Check for missing categories in imported expenses
                        val importedCategoryNames = expenses.map { it.category }.toSet()
                        val existingCategoryNames = categories.map { it.name }.toSet()
                        val missingCategoryNames = importedCategoryNames - existingCategoryNames
                        
                        if (missingCategoryNames.isNotEmpty()) {
                            val newCategories = missingCategoryNames.map { name ->
                                val sysCat = try { com.h2.wellspend.data.SystemCategory.valueOf(name) } catch (e: Exception) { null }
                                
                                if (sysCat != null) {
                                    val isSystemProtected = setOf(
                                         com.h2.wellspend.data.SystemCategory.Others.name,
                                         com.h2.wellspend.data.SystemCategory.Loan.name,
                                         com.h2.wellspend.data.SystemCategory.TransactionFee.name,
                                         com.h2.wellspend.data.SystemCategory.BalanceAdjustment.name
                                    ).contains(name)
                                    
                                    val color = com.h2.wellspend.ui.CategoryColors[sysCat]?.toArgb()?.toLong() 
                                        ?: (0xFF000000 or (kotlin.random.Random.nextLong() and 0x00FFFFFF))

                                    com.h2.wellspend.data.Category(
                                        name = name,
                                        iconName = name, 
                                        color = color,
                                        isSystem = isSystemProtected
                                    )
                                } else {
                                    val randomColor = (0xFF000000 or (kotlin.random.Random.nextLong() and 0x00FFFFFF))
                                    com.h2.wellspend.data.Category(
                                        name = name,
                                        iconName = "Label", 
                                        color = randomColor,
                                        isSystem = false
                                    )
                                }
                            }
                            categories = categories + newCategories
                        }

                        repository.importData(expenses, budgets, accounts, recurringConfigs, loans, categories)
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, "Import successful")
                        }
                    } else {
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(false, "Import failed: Invalid data structure")
                        }
                    }
                }
            } catch (e: com.google.gson.JsonSyntaxException) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Import failed: Invalid JSON format")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.localizedMessage}")
                }
            }
        }
    }
    fun addCategory(category: Category) {
        viewModelScope.launch {
            repository.addCategory(category)
        }
    }
    
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}

// Import DTOs to handle legacy data (missing fields)
@androidx.annotation.Keep
private data class ImportAppData(
    val expenses: List<ImportExpense>,
    val budgets: List<Budget>,
    val accounts: List<com.h2.wellspend.data.Account>? = null,
    val recurringConfigs: List<ImportRecurringConfig>? = null,
    val loans: List<com.h2.wellspend.data.Loan>? = null,
    val categories: List<Category>? = null
)

@androidx.annotation.Keep
private data class ImportExpense(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: String,
    val title: String? = null, // New field name
    val description: String? = null, // Legacy field name for backward compatibility
    val date: String,
    val timestamp: Long,
    // isRecurring removed
    val transactionType: com.h2.wellspend.data.TransactionType? = null,
    val accountId: String? = null,
    val transferTargetAccountId: String? = null,
    val feeAmount: Double? = null,
    val feeConfigName: String? = null,
    val loanId: String? = null,
    val note: String? = null
) {
    fun toExpense(): Expense {
        return Expense(
            id = id,
            amount = amount,
            category = category,
            title = title ?: description ?: "", // Use title if available, fallback to description
            date = date,
            timestamp = timestamp,
            // isRecurring removed
            transactionType = transactionType ?: com.h2.wellspend.data.TransactionType.EXPENSE,
            accountId = accountId,
            transferTargetAccountId = transferTargetAccountId,
            feeAmount = feeAmount ?: 0.0,
            feeConfigName = feeConfigName,
            loanId = loanId,
            note = note
        )
    }
}

@androidx.annotation.Keep
private data class ImportRecurringConfig(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: String,
    val title: String? = null,
    val description: String? = null,
    val note: String? = null,
    val frequency: RecurringFrequency,
    val nextDueDate: String,
    val transactionType: com.h2.wellspend.data.TransactionType? = null,
    val accountId: String? = null,
    val transferTargetAccountId: String? = null,
    val feeAmount: Double? = null,
    val feeConfigName: String? = null
) {
    fun toRecurringConfig(): RecurringConfig {
        return RecurringConfig(
            id = id,
            amount = amount,
            category = category,
            title = title ?: description ?: "",
            note = note,
            frequency = frequency,
            nextDueDate = nextDueDate,
            transactionType = transactionType ?: com.h2.wellspend.data.TransactionType.EXPENSE,
            accountId = accountId,
            transferTargetAccountId = transferTargetAccountId,
            feeAmount = feeAmount ?: 0.0,
            feeConfigName = feeConfigName
        )
    }
}

class MainViewModelFactory(
    private val repository: WellSpendRepository,
    private val initialThemeMode: String? = null,
    private val initialDynamicColor: Boolean = false,
    private val initialOnboardingCompleted: Boolean = false
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, initialThemeMode, initialDynamicColor, initialOnboardingCompleted) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
