package com.h2.wellspend.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Expense::class, Budget::class, RecurringConfig::class, Setting::class, CategorySortOrder::class, Account::class, Loan::class, Category::class], version = 7, exportSchema = true)
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wellspend_database"
                )
                .addMigrations(MIGRATION_5_6)
                //.fallbackToDestructiveMigration() // Removed to prevent data loss
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
