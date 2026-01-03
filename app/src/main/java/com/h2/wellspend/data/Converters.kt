package com.h2.wellspend.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import androidx.annotation.Keep

@Keep
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromFeeConfigList(value: List<FeeConfig>?): String? {
        if (value == null) return null
        val type = object : TypeToken<List<FeeConfig>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toFeeConfigList(value: String?): List<FeeConfig>? {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<FeeConfig>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category = try {
        Category.valueOf(value)
    } catch (e: Exception) {
        Category.Others
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromLoanType(value: LoanType): String = value.name

    @TypeConverter
    fun toLoanType(value: String): LoanType = try {
        LoanType.valueOf(value)
    } catch (e: Exception) {
        LoanType.LEND
    }
    
    @TypeConverter
    fun fromRecurringFrequency(value: RecurringFrequency): String = value.name

    @TypeConverter
    fun toRecurringFrequency(value: String): RecurringFrequency = try {
        RecurringFrequency.valueOf(value)
    } catch (e: Exception) {
        RecurringFrequency.MONTHLY
    }
}
