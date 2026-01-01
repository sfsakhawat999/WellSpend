package com.h2.wellspend.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WellSpendRepository(private val database: AppDatabase) {
    val expenses: Flow<List<Expense>> = database.expenseDao().getAllExpenses()
    val budgets: Flow<List<Budget>> = database.budgetDao().getAllBudgets()
    val recurringConfigs: Flow<List<RecurringConfig>> = database.recurringDao().getAllRecurringConfigs()
    val accounts: Flow<List<Account>> = database.accountDao().getAllAccounts()

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

    suspend fun getAccountById(id: String): Account? {
        return database.accountDao().getAccountById(id)
    }

    // Category Sorting
    val sortedCategories: Flow<List<Category>> = database.categoryDao().getAllCategorySortOrders()
        .map { sortOrders ->
            if (sortOrders.isEmpty()) {
                // Return default order if no sort order is saved
                Category.values().toList()
            } else {
                // Create a map of category name to sort order
                val orderMap = sortOrders.associate { it.categoryName to it.sortOrder }
                
                // Sort categories based on the map, putting those without order at the end
                Category.values().sortedBy { category ->
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

    suspend fun ensureCategoryOrderInitialized() {
        val existingOrders = database.categoryDao().getAllCategorySortOrdersOneShot()
        if (existingOrders.isEmpty()) {
            val defaultOrders = Category.values().mapIndexed { index, category ->
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

    suspend fun getAllExpensesOneShot(): List<Expense> {
        return database.expenseDao().getAllExpensesOneShot()
    }

    suspend fun getAllBudgetsOneShot(): List<Budget> {
        return database.budgetDao().getAllBudgetsOneShot()
    }

    suspend fun getAllAccountsOneShot(): List<Account> {
        return database.accountDao().getAllAccountsOneShot()
    }

    suspend fun importData(
        expenses: List<Expense>, 
        budgets: List<Budget>, 
        accounts: List<Account>?, 
        recurringConfigs: List<RecurringConfig>?
    ) {
        if (accounts != null) {
            database.accountDao().insertAccounts(accounts)
        }
        database.expenseDao().insertExpenses(expenses)
        database.budgetDao().insertBudgets(budgets)
        if (recurringConfigs != null) {
            database.recurringDao().insertRecurringConfigs(recurringConfigs)
        }
    }
}
