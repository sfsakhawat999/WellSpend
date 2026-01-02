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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import java.util.UUID

class MainViewModel(
    private val repository: WellSpendRepository,
    initialThemeMode: String? = null,
    initialDynamicColor: Boolean = false
) : ViewModel() {

    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recurringConfigs = repository.recurringConfigs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val loans = repository.loans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// ... (existing code) ...

    fun addLoan(
        name: String,
        amount: Double,
        type: com.h2.wellspend.data.LoanType,
        description: String?,
        accountId: String?,
        feeAmount: Double,
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
                description = "Initial Loan Amount: $name",
                category = Category.Loan,
                date = date.atStartOfDay().toString(),
                timestamp = System.currentTimeMillis(),
                transactionType = transactionType,
                accountId = accountId,
                loanId = loanId,
                feeAmount = feeAmount
            )
            repository.addExpense(expense)
        }
    }

    fun updateLoan(loan: com.h2.wellspend.data.Loan) {
        viewModelScope.launch {
             repository.addLoan(loan)
        }
    }

    fun addLoanTransaction(
        loanId: String,
        amount: Double,
        isPayment: Boolean, // True = Pay/Receive, False = Increase Loan
        accountId: String?,
        loanType: com.h2.wellspend.data.LoanType,
        feeAmount: Double,
        date: java.time.LocalDate
    ) {
        viewModelScope.launch {
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
            
            val desc = if (isPayment) "Loan Repayment" else "Loan Increase"

            val expense = Expense(
                amount = amount,
                description = desc,
                category = Category.Loan,
                date = date.atStartOfDay().toString(),
                timestamp = System.currentTimeMillis(),
                transactionType = transactionType,
                accountId = accountId,
                loanId = loanId,
                feeAmount = feeAmount
            )
            repository.addExpense(expense)
        }
    }

    fun deleteLoan(loan: com.h2.wellspend.data.Loan) {
        viewModelScope.launch {
            // Update linked expenses to remove loanId (orphaned history)
            val allExpenses = repository.getAllExpensesOneShot()
            val expensesToUpdate = allExpenses.filter { it.loanId == loan.id }.map { 
                it.copy(loanId = null, description = "${it.description} (Deleted Loan: ${loan.name})") 
            }
            
            if (expensesToUpdate.isNotEmpty()) {
                repository.addExpenses(expensesToUpdate)
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
        optimistic ?: repo
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        .map { it ?: "DARK" } // Default to DARK
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialThemeMode ?: "DARK")

    val dynamicColor: StateFlow<Boolean> = repository.dynamicColor
        .map { it?.toBoolean() ?: false } // Default to false
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialDynamicColor)

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
                            description = "${config.description} (Recurring)",
                            date = nextDate.atStartOfDay().toString(), // ISO-ish
                            timestamp = System.currentTimeMillis(),
                            isRecurring = true,
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
        description: String, 
        category: Category?, 
        date: String, 
        isRecurring: Boolean, 
        frequency: RecurringFrequency,
        transactionType: com.h2.wellspend.data.TransactionType,
        accountId: String?,
        targetAccountId: String?,
        feeAmount: Double,
        feeConfigName: String?
    ) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                description = description,
                category = category ?: Category.Others,
                date = date, // Should be ISO string
                timestamp = System.currentTimeMillis(),
                isRecurring = false, // The manual entry itself
                transactionType = transactionType,
                accountId = accountId,
                transferTargetAccountId = targetAccountId,
                feeAmount = feeAmount,
                feeConfigName = feeConfigName
            )
            repository.addExpense(expense)

            if (isRecurring) {
                val startDate = LocalDate.parse(date.substring(0, 10))
                val nextDate = if (frequency == RecurringFrequency.WEEKLY) startDate.plusWeeks(1) else startDate.plusMonths(1)
                
                val config = RecurringConfig(
                    amount = amount,
                    category = category ?: Category.Others,
                    description = description,
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
        description: String, 
        category: Category?, 
        date: String, 
        isRecurring: Boolean, 
        frequency: RecurringFrequency,
        transactionType: com.h2.wellspend.data.TransactionType,
        accountId: String?,
        targetAccountId: String?,
        feeAmount: Double,
        feeConfigName: String?,
        loanId: String? = null
    ) {
        viewModelScope.launch {
            val expense = Expense(
                id = id,
                amount = amount,
                description = description,
                category = category ?: Category.Others,
                date = date,
                timestamp = System.currentTimeMillis(), 
                isRecurring = false,
                transactionType = transactionType,
                accountId = accountId,
                transferTargetAccountId = targetAccountId,
                feeAmount = feeAmount,
                feeConfigName = feeConfigName,
                loanId = loanId
            )
            repository.addExpense(expense) // Room's Insert(onConflict = REPLACE) handles update

            if (isRecurring) {
                 val startDate = LocalDate.parse(date.substring(0, 10))
                val nextDate = if (frequency == RecurringFrequency.WEEKLY) startDate.plusWeeks(1) else startDate.plusMonths(1)
                
                val config = RecurringConfig(
                    amount = amount,
                    category = category ?: Category.Others,
                    description = description,
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

    fun exportData(uri: android.net.Uri, contentResolver: android.content.ContentResolver, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val expenses = repository.getAllExpensesOneShot()
                val budgets = repository.getAllBudgetsOneShot()
                val accounts = repository.getAllAccountsOneShot()
                val recurringConfigs = repository.getRecurringConfigsOneShot()
                val appData = com.h2.wellspend.data.AppData(expenses, budgets, accounts, recurringConfigs)
                
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
                        .registerTypeAdapter(Category::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
                            try {
                                Category.valueOf(json.asString)
                            } catch (e: Exception) {
                                Category.Others
                            }
                        })
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

                        repository.importData(expenses, budgets, accounts, recurringConfigs)
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
}

// Import DTOs to handle legacy data (missing fields)
@androidx.annotation.Keep
private data class ImportAppData(
    val expenses: List<ImportExpense>,
    val budgets: List<Budget>,
    val accounts: List<com.h2.wellspend.data.Account>? = null,
    val recurringConfigs: List<ImportRecurringConfig>? = null
)

@androidx.annotation.Keep
private data class ImportExpense(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: Category,
    val description: String,
    val date: String,
    val timestamp: Long,
    val isRecurring: Boolean = false,
    val transactionType: com.h2.wellspend.data.TransactionType? = null,
    val accountId: String? = null,
    val transferTargetAccountId: String? = null,
    val feeAmount: Double? = null,
    val feeConfigName: String? = null
) {
    fun toExpense(): Expense {
        return Expense(
            id = id,
            amount = amount,
            category = category,
            description = description,
            date = date,
            timestamp = timestamp,
            isRecurring = isRecurring,
            transactionType = transactionType ?: com.h2.wellspend.data.TransactionType.EXPENSE,
            accountId = accountId,
            transferTargetAccountId = transferTargetAccountId,
            feeAmount = feeAmount ?: 0.0,
            feeConfigName = feeConfigName
        )
    }
}

@androidx.annotation.Keep
private data class ImportRecurringConfig(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: Category,
    val description: String,
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
            description = description,
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
    private val initialDynamicColor: Boolean = false
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, initialThemeMode, initialDynamicColor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
