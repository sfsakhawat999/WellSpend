package com.h2.wellspend.utils

import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.TimeRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

object DateUtils {

    fun filterByTimeRange(transactions: List<Expense>, date: LocalDate, range: TimeRange, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY, customDateRange: Pair<LocalDate, LocalDate>? = null): List<Expense> {
        return transactions.filter {
            val transactionDate = try {
                LocalDate.parse(it.date.take(10))
            } catch (e: Exception) {
                return@filter false
            }

            when (range) {
                TimeRange.DAILY -> transactionDate.isEqual(date)
                TimeRange.WEEKLY -> {
                    val startOfWeek = date.with(WeekFields.of(firstDayOfWeek, 1).dayOfWeek(), 1)
                    val endOfWeek = date.with(WeekFields.of(firstDayOfWeek, 1).dayOfWeek(), 7)
                    !transactionDate.isBefore(startOfWeek) && !transactionDate.isAfter(endOfWeek)
                }
                TimeRange.MONTHLY -> transactionDate.month == date.month && transactionDate.year == date.year
                TimeRange.YEARLY -> transactionDate.year == date.year
                TimeRange.CUSTOM -> {
                    if (customDateRange != null) {
                        !transactionDate.isBefore(customDateRange.first) && !transactionDate.isAfter(customDateRange.second)
                    } else {
                         // Fallback to month if custom range is missing (should verify this design decision)
                         transactionDate.month == date.month && transactionDate.year == date.year
                    }
                }
            }
        }
    }
    
    fun getStartOfRange(date: LocalDate, range: TimeRange, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY, customDateRange: Pair<LocalDate, LocalDate>? = null): LocalDate {
        return when(range) {
            TimeRange.DAILY -> date
            TimeRange.WEEKLY -> date.with(WeekFields.of(firstDayOfWeek, 1).dayOfWeek(), 1)
            TimeRange.MONTHLY -> date.withDayOfMonth(1)
            TimeRange.YEARLY -> date.withDayOfYear(1)
            TimeRange.CUSTOM -> customDateRange?.first ?: date.withDayOfMonth(1)
        }
    }

    fun getEndOfRange(date: LocalDate, range: TimeRange, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY, customDateRange: Pair<LocalDate, LocalDate>? = null): LocalDate {
        return when(range) {
            TimeRange.DAILY -> date
            TimeRange.WEEKLY -> date.with(WeekFields.of(firstDayOfWeek, 1).dayOfWeek(), 7)
            TimeRange.MONTHLY -> date.withDayOfMonth(date.lengthOfMonth())
            TimeRange.YEARLY -> date.withDayOfYear(date.lengthOfYear())
            TimeRange.CUSTOM -> customDateRange?.second ?: date.withDayOfMonth(date.lengthOfMonth())
        }
    }

    fun formatDateForRange(date: LocalDate, range: TimeRange, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY, customDateRange: Pair<LocalDate, LocalDate>? = null): String {
        return when (range) {
            TimeRange.DAILY -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            TimeRange.WEEKLY -> {
                val startOfWeek = date.with(WeekFields.of(firstDayOfWeek, 1).dayOfWeek(), 1)
                val endOfWeek = date.with(WeekFields.of(firstDayOfWeek, 1).dayOfWeek(), 7)
                // If same month: "Jan 01 - 07, 2024", if Diff month: "Jan 29 - Feb 04, 2024"
                if (startOfWeek.month == endOfWeek.month) {
                     "${startOfWeek.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("dd, yyyy"))}"
                } else {
                     "${startOfWeek.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                }
            }
            TimeRange.MONTHLY -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimeRange.YEARLY -> date.format(DateTimeFormatter.ofPattern("yyyy"))
            TimeRange.CUSTOM -> {
                if (customDateRange != null) {
                    val start = customDateRange.first
                    val end = customDateRange.second
                    if (start.year == end.year) {
                         "${start.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${end.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                    } else {
                         "${start.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} - ${end.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                    }
                } else date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            }
        }
    }
    
    fun adjustDate(date: LocalDate, range: TimeRange, increment: Boolean, customDateRange: Pair<LocalDate, LocalDate>? = null): LocalDate {
        val amount = if (increment) 1L else -1L
        return when (range) {
            TimeRange.DAILY -> date.plusDays(amount)
            TimeRange.WEEKLY -> date.plusWeeks(amount)
            TimeRange.MONTHLY -> date.plusMonths(amount)
            TimeRange.YEARLY -> date.plusYears(amount)
            TimeRange.CUSTOM -> {
                // For custom range, we treat "date" as the start date or anchor. 
                // However, navigation in custom range usually means "move the window".
                // But since 'date' is single, and 'customDateRange' holds the window...
                // Strategy: We shift the 'currentDate' (which tracks the anchor) by the duration of the custom range?
                // OR simpler: Shift by 1 Month if it's a custom range, or shift by duration?
                // Let's shift by the duration of the current custom range.
                if (customDateRange != null) {
                    val days = java.time.temporal.ChronoUnit.DAYS.between(customDateRange.first, customDateRange.second) + 1
                    date.plusDays(amount * days)
                } else {
                    date.plusMonths(amount)
                }
            }
        }
    }
}
