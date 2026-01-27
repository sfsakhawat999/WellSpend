package com.h2.wellspend.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.annotation.Keep
import java.util.UUID

@Keep
enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}


@Keep
enum class SystemCategory {
    Shopping, Snacks, Food, Rent, Internet, Phone, Education,
    Transport, Groceries, Clothing, Health, Entertainment, Utilities, Insurance, Savings, Gifts, Travel, Subscriptions, Work, Pets, Family, Fitness, Beauty, Donations, Investments, Electronics, Hobbies, TransactionFee, Loan, BalanceAdjustment, Others
}

@Keep
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,
    val iconName: String, // Name of the icon in the map
    val color: Long, // Color as Long (ARGB)
    val isSystem: Boolean = false
)

@Keep
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: String, // Changed from Category enum to String
    val title: String,
    val date: String, // ISO String
    val timestamp: Long,
    // isRecurring removed as it's handled by recurring_configs table now
    
    // New fields for Account & Transaction support
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val accountId: String? = null,
    val transferTargetAccountId: String? = null,
    val feeAmount: Double = 0.0,
    val feeConfigName: String? = null,
    
    // Loan Support
    val loanId: String? = null,
    val note: String? = null
)

@Keep
enum class LoanType {
    LEND, BORROW
}

@Keep
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LoanType,
    val amount: Double, // Current balance logic might be derived, but initial amount is useful or we just sum transactions
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String, // Changed from Category enum to String
    val limitAmount: Double
)

@Keep
enum class RecurringFrequency {
    WEEKLY, MONTHLY
}

@Keep
@Entity(tableName = "recurring_configs")
data class RecurringConfig(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: String, // Changed from Category enum to String
    val title: String,
    val note: String? = null,
    val frequency: RecurringFrequency,
    val nextDueDate: String, // ISO String
    
    // New fields
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val accountId: String? = null,
    val transferTargetAccountId: String? = null,
    val feeAmount: Double = 0.0,
    val feeConfigName: String? = null
)

@Keep
@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey val key: String,
    val value: String
)

@Keep
data class AppData(
    val expenses: List<Expense>,
    val budgets: List<Budget>,
    val accounts: List<Account>? = null,
    val recurringConfigs: List<RecurringConfig>? = null,
    val loans: List<Loan>? = null,
    val categories: List<Category>? = null
)

@Keep
@Entity(tableName = "category_sort_orders")
data class CategorySortOrder(
    @PrimaryKey val categoryName: String,
    val sortOrder: Int
)
