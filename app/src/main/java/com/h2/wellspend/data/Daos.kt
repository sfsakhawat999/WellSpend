package com.h2.wellspend.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesOneShot(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Delete
    suspend fun deleteExpense(expense: Expense)
    
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsOneShot(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_configs")
    fun getAllRecurringConfigs(): Flow<List<RecurringConfig>>
    
    @Query("SELECT * FROM recurring_configs")
    suspend fun getAllRecurringConfigsOneShot(): List<RecurringConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringConfig(config: RecurringConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringConfigs(configs: List<RecurringConfig>)
    
    @Update
    suspend fun updateRecurringConfig(config: RecurringConfig)
    
    @Update
    suspend fun updateRecurringConfigs(configs: List<RecurringConfig>)
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE key = :key")
    fun getSettingFlow(key: String): Flow<String?>

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: Setting)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category_sort_orders ORDER BY sortOrder ASC")
    fun getAllCategorySortOrders(): Flow<List<CategorySortOrder>>

    @Query("SELECT * FROM category_sort_orders ORDER BY sortOrder ASC")
    suspend fun getAllCategorySortOrdersOneShot(): List<CategorySortOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategorySortOrders(orders: List<CategorySortOrder>)

    @Update
    suspend fun updateCategorySortOrder(order: CategorySortOrder)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsOneShot(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Delete
    suspend fun deleteAccount(account: Account)
}
