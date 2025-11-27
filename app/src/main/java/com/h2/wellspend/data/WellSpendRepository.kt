package com.h2.wellspend.data

import kotlinx.coroutines.flow.Flow

class WellSpendRepository(private val database: AppDatabase) {
    val expenses: Flow<List<Expense>> = database.expenseDao().getAllExpenses()
    val budgets: Flow<List<Budget>> = database.budgetDao().getAllBudgets()
    val recurringConfigs: Flow<List<RecurringConfig>> = database.recurringDao().getAllRecurringConfigs()
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

    val categoryOrder: Flow<String?> = database.settingDao().getSettingFlow("category_order")

    suspend fun setCategoryOrder(order: String) {
        database.settingDao().insertSetting(Setting("category_order", order))
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

    suspend fun importData(expenses: List<Expense>, budgets: List<Budget>) {
        database.expenseDao().insertExpenses(expenses)
        database.budgetDao().insertBudgets(budgets)
    }
}
