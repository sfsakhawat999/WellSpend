package com.h2.wellspend.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.h2.wellspend.ui.getSystemCategoryColor
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.combine
import com.h2.wellspend.data.SystemCategory

class WellSpendRepository(private val database: AppDatabase) {
    val expenses: Flow<List<Expense>> = database.expenseDao().getAllExpenses()
    val budgets: Flow<List<Budget>> = database.budgetDao().getAllBudgets()
    val recurringConfigs: Flow<List<RecurringConfig>> = database.recurringDao().getAllRecurringConfigs()
    val accounts: Flow<List<Account>> = database.accountDao().getAllAccounts()
    val loans: Flow<List<Loan>> = database.loanDao().getAllLoans()

    val currency: Flow<String?> = database.settingDao().getSettingFlow("currency")


    suspend fun addExpense(expense: Expense) {
        database.expenseDao().insertExpense(expense)
    }

    suspend fun addExpenses(expenses: List<Expense>) {
        database.expenseDao().insertExpenses(expenses)
    }

    suspend fun deleteExpense(id: String) {
        database.expenseDao().deleteExpenseById(id)
    }

    suspend fun deleteExpenses(expenses: List<Expense>) {
        database.expenseDao().deleteExpenses(expenses)
    }

    suspend fun setBudget(budget: Budget) {
        database.budgetDao().insertBudget(budget)
    }

    suspend fun addRecurringConfig(config: RecurringConfig) {
        database.recurringDao().insertRecurringConfig(config)
    }
    
    suspend fun updateRecurringConfigs(configs: List<RecurringConfig>) {
        database.recurringDao().updateRecurringConfigs(configs)
    }
    
    suspend fun getRecurringConfigsOneShot(): List<RecurringConfig> {
        return database.recurringDao().getAllRecurringConfigsOneShot()
    }
    suspend fun setCurrency(symbol: String) {
        database.settingDao().insertSetting(Setting("currency", symbol))
    }

    suspend fun addAccount(account: Account) {
        database.accountDao().insertAccount(account)
    }

    suspend fun deleteAccount(account: Account) {
        database.accountDao().deleteAccount(account)
    }

    suspend fun updateAccountOrders(accounts: List<Account>) {
        database.accountDao().insertAccounts(accounts)
    }

    suspend fun getAccountById(id: String): Account? {
        return database.accountDao().getAccountById(id)
    }

    suspend fun addLoan(loan: Loan) {
        database.loanDao().insertLoan(loan)
    }

    suspend fun deleteLoan(loan: Loan) {
        database.loanDao().deleteLoan(loan)
    }
    
    // Category Management
    suspend fun addCategory(category: Category) {
        database.categoryDao().insertCategory(category)
    }
    
    suspend fun deleteCategory(category: Category) {
        database.categoryDao().deleteCategory(category)
    }

    // Category Sorting and Listing
    val sortedCategories: Flow<List<Category>> = database.categoryDao().getAllCategories()
        .combine(database.categoryDao().getAllCategorySortOrders()) { categories, sortOrders ->
             if (categories.isEmpty()) {
                 emptyList()
             } else {
                 val orderMap = sortOrders.associate { it.categoryName to it.sortOrder }
                 categories.sortedBy { category ->
                     orderMap[category.name] ?: Int.MAX_VALUE
                 }
             }
        }

    suspend fun updateCategoryOrder(categories: List<Category>) {
        val sortOrders = categories.mapIndexed { index, category ->
            CategorySortOrder(category.name, index)
        }
        database.categoryDao().insertCategorySortOrders(sortOrders)
    }

    suspend fun ensureCategoriesInitialized() {
        // Check if categories table is empty (not just sort orders)
        val existingCategories = database.categoryDao().getAllCategoriesOneShot()
        if (existingCategories.isEmpty()) {
            val systemInternalCategories = setOf(
                SystemCategory.TransactionFee, SystemCategory.Loan, SystemCategory.BalanceAdjustment, SystemCategory.Others
            )
            
            val defaultCategories = SystemCategory.values().filter { 
                systemInternalCategories.contains(it)
            }.map { sysCat ->
                Category(
                    name = sysCat.name,
                    iconName = sysCat.name, // Use nameKey which maps to SystemCategory icon lookup
                    color = getSystemCategoryColor(sysCat).toArgb().toLong(),
                    isSystem = true
                )
            }
            database.categoryDao().insertCategories(defaultCategories)
            
            // Also initialize sort order
            val defaultOrders = defaultCategories.mapIndexed { index, category ->
                CategorySortOrder(category.name, index)
            }
            database.categoryDao().insertCategorySortOrders(defaultOrders)
        }
    }

    val themeMode: Flow<String?> = database.settingDao().getSettingFlow("theme_mode")
    val dynamicColor: Flow<String?> = database.settingDao().getSettingFlow("dynamic_color")

    suspend fun setThemeMode(mode: String) {
        database.settingDao().insertSetting(Setting("theme_mode", mode))
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        database.settingDao().insertSetting(Setting("dynamic_color", enabled.toString()))
    }

    val excludeLoanTransactions: Flow<String?> = database.settingDao().getSettingFlow("exclude_loan_transactions")
    val onboardingCompleted: Flow<String?> = database.settingDao().getSettingFlow("onboarding_completed")
    
    suspend fun setExcludeLoanTransactions(exclude: Boolean) {
        database.settingDao().insertSetting(Setting("exclude_loan_transactions", exclude.toString()))
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        database.settingDao().insertSetting(Setting("onboarding_completed", completed.toString()))
    }

    val showAccountsOnHomepage: Flow<String?> = database.settingDao().getSettingFlow("show_accounts_on_homepage")

    suspend fun setShowAccountsOnHomepage(show: Boolean) {
        database.settingDao().insertSetting(Setting("show_accounts_on_homepage", show.toString()))
    }

    suspend fun getAllExpensesOneShot(): List<Expense> {
        return database.expenseDao().getAllExpensesOneShot()
    }

    suspend fun getAllBudgetsOneShot(): List<Budget> {
        return database.budgetDao().getAllBudgetsOneShot()
    }

    suspend fun getAllAccountsOneShot(): List<Account> {
        return database.accountDao().getAllAccountsOneShot()
    }

    suspend fun getAllLoansOneShot(): List<Loan> {
        return database.loanDao().getAllLoansOneShot()
    }

    suspend fun importData(
        expenses: List<Expense>, 
        budgets: List<Budget>, 
        accounts: List<Account>?, 
        recurringConfigs: List<RecurringConfig>?,
        loans: List<Loan>?,
        categories: List<Category>?
    ) {
        if (categories != null) {
            // We use INSERT OR IGNORE or REPLACE strategy in DAO usually. 
            // If checking collision is needed, we might want to be careful.
            // Assuming Replace is fine or Ignore.
            // But custom categories should be added.
            database.categoryDao().insertCategories(categories)
        }
        if (accounts != null) {
            database.accountDao().insertAccounts(accounts)
        }
        database.expenseDao().insertExpenses(expenses)
        database.budgetDao().insertBudgets(budgets)
        if (recurringConfigs != null) {
            database.recurringDao().insertRecurringConfigs(recurringConfigs)
        }
        if (loans != null) {
            database.loanDao().insertLoans(loans)
        }
    }
}
