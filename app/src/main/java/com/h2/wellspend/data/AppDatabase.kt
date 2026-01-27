package com.h2.wellspend.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Expense::class, Budget::class, RecurringConfig::class, Setting::class, CategorySortOrder::class, Account::class, Loan::class, Category::class], version = 10, exportSchema = true)
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
                .addMigrations(MIGRATION_5_6, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                //.fallbackToDestructiveMigration() // Removed to prevent data loss
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
