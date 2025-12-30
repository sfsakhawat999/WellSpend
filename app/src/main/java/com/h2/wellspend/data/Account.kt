package com.h2.wellspend.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.annotation.Keep
import java.util.UUID

@Keep
data class FeeConfig(
    val name: String,
    val value: Double,
    val isPercentage: Boolean
)

@Keep
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val initialBalance: Double,
    val feeConfigs: List<FeeConfig> // Requires TypeConverter
)
