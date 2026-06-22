package com.h2.wellspend.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Expense::class, Budget::class, RecurringConfig::class, Setting::class, CategorySortOrder::class, Account::class, Loan::class, Category::class], version = 13, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun settingDao(): SettingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val systemCategories = setOf("Loan", "TransactionFee", "BalanceAdjustment", "Others")
                val categoryMap = mutableMapOf<String, String>()

                // 1. Read existing categories to generate UUIDs
                val cursor = database.query("SELECT name FROM categories")
                if (cursor.moveToFirst()) {
                    do {
                        val oldName = cursor.getString(0)
                        val trimmedName = oldName.trim()
                        val newId = if (systemCategories.contains(trimmedName)) {
                            trimmedName
                        } else {
                            java.util.UUID.randomUUID().toString()
                        }
                        categoryMap[oldName] = newId
                    } while (cursor.moveToNext())
                }
                cursor.close()

                // Also find all categories in other tables that might be missing from categories table
                val existingRefCategories = mutableSetOf<String>()
                fun addRefsFromTable(tableName: String) {
                    val refCursor = database.query("SELECT DISTINCT category FROM $tableName")
                    if (refCursor.moveToFirst()) {
                        do {
                            val cat = refCursor.getString(0)
                            if (cat != null) {
                                existingRefCategories.add(cat)
                            }
                        } while (refCursor.moveToNext())
                    }
                    refCursor.close()
                }
                try { addRefsFromTable("transactions") } catch(e: Exception) {}
                try { addRefsFromTable("budgets") } catch(e: Exception) {}
                try { addRefsFromTable("recurring_configs") } catch(e: Exception) {}

                existingRefCategories.forEach { oldName ->
                    if (!categoryMap.containsKey(oldName)) {
                        val trimmedName = oldName.trim()
                        val newId = if (systemCategories.contains(trimmedName)) {
                            trimmedName
                        } else {
                            java.util.UUID.randomUUID().toString()
                        }
                        categoryMap[oldName] = newId
                    }
                }

                // 2. Recreate categories table
                database.execSQL("CREATE TABLE IF NOT EXISTS `categories_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `iconName` TEXT NOT NULL, `color` INTEGER NOT NULL, `isSystem` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                val catCursor = database.query("SELECT name, iconName, color, isSystem FROM categories")
                if (catCursor.moveToFirst()) {
                    do {
                        val oldName = catCursor.getString(0)
                        val trimmedName = oldName.trim()
                        val iconName = catCursor.getString(1)
                        val color = catCursor.getLong(2)
                        val isSystem = catCursor.getInt(3)
                        val newId = categoryMap[oldName] ?: trimmedName
                        database.execSQL(
                            "INSERT INTO `categories_new` (id, name, iconName, color, isSystem) VALUES (?, ?, ?, ?, ?)",
                            arrayOf(newId, trimmedName, iconName, color, isSystem)
                        )
                    } while (catCursor.moveToNext())
                }
                catCursor.close()

                // 3. Recreate category_sort_orders table
                database.execSQL("CREATE TABLE IF NOT EXISTS `category_sort_orders_new` (`categoryId` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`categoryId`))")
                val sortCursor = database.query("SELECT categoryName, sortOrder FROM category_sort_orders")
                if (sortCursor.moveToFirst()) {
                    do {
                        val categoryName = sortCursor.getString(0)
                        val sortOrder = sortCursor.getInt(1)
                        val newId = categoryMap[categoryName] ?: categoryName.trim()
                        database.execSQL(
                            "INSERT INTO `category_sort_orders_new` (categoryId, sortOrder) VALUES (?, ?)",
                            arrayOf(newId, sortOrder)
                        )
                    } while (sortCursor.moveToNext())
                }
                sortCursor.close()

                // 4. Drop old tables and rename new ones
                database.execSQL("DROP TABLE categories")
                database.execSQL("ALTER TABLE categories_new RENAME TO categories")
                database.execSQL("DROP TABLE category_sort_orders")
                database.execSQL("ALTER TABLE category_sort_orders_new RENAME TO category_sort_orders")

                // 5. Update references in transactions, budgets, and recurring_configs
                categoryMap.forEach { (oldName, newId) ->
                    database.execSQL("UPDATE transactions SET category = ? WHERE category = ?", arrayOf(newId, oldName))
                    database.execSQL("UPDATE budgets SET category = ? WHERE category = ?", arrayOf(newId, oldName))
                    database.execSQL("UPDATE recurring_configs SET category = ? WHERE category = ?", arrayOf(newId, oldName))
                }
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `loans` ADD COLUMN `excludeFromSummary` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Rename expenses table to transactions
                database.execSQL("ALTER TABLE `expenses` RENAME TO `transactions`")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create Loan Table
                database.execSQL("CREATE TABLE IF NOT EXISTS `loans` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                
                // Add loanId to expenses
                database.execSQL("ALTER TABLE `expenses` ADD COLUMN `loanId` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
               database.execSQL("ALTER TABLE `expenses` ADD COLUMN `note` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
               database.execSQL("ALTER TABLE `expenses` RENAME COLUMN `description` TO `title`")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Recurring Config Changes
                database.execSQL("ALTER TABLE `recurring_configs` RENAME COLUMN `description` TO `title`")
                database.execSQL("ALTER TABLE `recurring_configs` ADD COLUMN `note` TEXT DEFAULT NULL")

                // 2. Remove isRecurring from Expenses
                // SQLite doesn't support DROP COLUMN directly, so we must recreate the table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expenses_new` (
                        `id` TEXT NOT NULL, 
                        `amount` REAL NOT NULL, 
                        `category` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `date` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `transactionType` TEXT NOT NULL, 
                        `accountId` TEXT DEFAULT NULL, 
                        `transferTargetAccountId` TEXT DEFAULT NULL, 
                        `feeAmount` REAL NOT NULL, 
                        `feeConfigName` TEXT DEFAULT NULL, 
                        `loanId` TEXT DEFAULT NULL, 
                        `note` TEXT DEFAULT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)

                database.execSQL("""
                    INSERT INTO `expenses_new` (
                        id, amount, category, title, date, timestamp, transactionType, 
                        accountId, transferTargetAccountId, feeAmount, feeConfigName, loanId, note
                    )
                    SELECT 
                        id, amount, category, title, date, timestamp, transactionType, 
                        accountId, transferTargetAccountId, feeAmount, feeConfigName, loanId, note
                    FROM `expenses`
                """)

                database.execSQL("DROP TABLE `expenses`")
                database.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wellspend_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                //.fallbackToDestructiveMigration() // Removed to prevent data loss
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
