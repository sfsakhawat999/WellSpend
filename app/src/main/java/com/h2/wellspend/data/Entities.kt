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
enum class Category {
    Shopping, Snacks, Food, Rent, Internet, Phone, Education,
    Transport, Groceries, Clothing, Health, Entertainment, Utilities, Insurance, Savings, Gifts, Travel, Subscriptions, Work, Pets, Family, Fitness, Beauty, Donations, Investments, Electronics, Hobbies, TransactionFee, Others
}

@Keep
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: Category,
    val description: String,
    val date: String, // ISO String
    val timestamp: Long,
    val isRecurring: Boolean = false,
    
    // New fields for Account & Transaction support
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val accountId: String? = null,
    val transferTargetAccountId: String? = null,
    val feeAmount: Double = 0.0,
    val feeConfigName: String? = null
)

@Keep
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: Category,
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
    val category: Category,
    val description: String,
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
    val recurringConfigs: List<RecurringConfig>? = null
)

@Keep
@Entity(tableName = "category_sort_orders")
data class CategorySortOrder(
    @PrimaryKey val categoryName: String,
    val sortOrder: Int
)
