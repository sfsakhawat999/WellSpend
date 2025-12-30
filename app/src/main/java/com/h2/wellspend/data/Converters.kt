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
}
