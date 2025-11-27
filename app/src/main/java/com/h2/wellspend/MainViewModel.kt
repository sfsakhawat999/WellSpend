package com.h2.wellspend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2.wellspend.data.Budget
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.RecurringConfig
import com.h2.wellspend.data.RecurringFrequency
import com.h2.wellspend.data.WellSpendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class MainViewModel(private val repository: WellSpendRepository) : ViewModel() {

    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recurringConfigs = repository.recurringConfigs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val currency: StateFlow<String> = repository.currency
        .map { it ?: "$" } // Default to $ if null
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    val categoryOrder: StateFlow<List<Category>> = repository.categoryOrder
        .map { orderString ->
            if (orderString.isNullOrEmpty()) {
                Category.values().toList()
            } else {
                val savedOrder = orderString.split(",").mapNotNull { name ->
                    try {
                        Category.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
                // Append any missing categories (e.g. newly added ones)
                val missing = Category.values().filter { !savedOrder.contains(it) }
                savedOrder + missing
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Category.values().toList())

    val themeMode: StateFlow<String> = repository.themeMode
        .map { it ?: "DARK" } // Default to DARK
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")

    val dynamicColor: StateFlow<Boolean> = repository.dynamicColor
        .map { it?.toBoolean() ?: false } // Default to false
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkRecurringExpenses()
    }

    private fun checkRecurringExpenses() {
        viewModelScope.launch {
            val configs = repository.getRecurringConfigsOneShot()
            val now = LocalDate.now()
            val newExpenses = mutableListOf<Expense>()
            val updatedConfigs = mutableListOf<RecurringConfig>()

            configs.forEach { config ->
                var nextDate = LocalDate.parse(config.nextDueDate.substring(0, 10))
                var configChanged = false

                while (nextDate <= now) {
                    configChanged = true
                    newExpenses.add(
                        Expense(
                            id = UUID.randomUUID().toString(),
                            amount = config.amount,
                            category = config.category,
                            description = "${config.description} (Recurring)",
                            date = nextDate.atStartOfDay().toString(), // ISO-ish
                            timestamp = System.currentTimeMillis(),
                            isRecurring = true
                        )
                    )

                    nextDate = if (config.frequency == RecurringFrequency.WEEKLY) {
                        nextDate.plusWeeks(1)
                    } else {
                        nextDate.plusMonths(1)
                    }
                }

                if (configChanged) {
                    updatedConfigs.add(config.copy(nextDueDate = nextDate.toString()))
                }
            }

            if (newExpenses.isNotEmpty()) {
                repository.addExpenses(newExpenses)
                repository.updateRecurringConfigs(updatedConfigs)
            }
        }
    }

    fun addExpense(amount: Double, description: String, category: Category, date: String, isRecurring: Boolean, frequency: RecurringFrequency) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                description = description,
                category = category,
                date = date, // Should be ISO string
                timestamp = System.currentTimeMillis(),
                isRecurring = false // The manual entry itself
            )
            repository.addExpense(expense)

            if (isRecurring) {
                val startDate = LocalDate.parse(date.substring(0, 10))
                val nextDate = if (frequency == RecurringFrequency.WEEKLY) startDate.plusWeeks(1) else startDate.plusMonths(1)
                
                val config = RecurringConfig(
                    amount = amount,
                    category = category,
                    description = description,
                    frequency = frequency,
                    nextDueDate = nextDate.toString()
                )
                repository.addRecurringConfig(config)
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }

    fun updateBudgets(newBudgets: List<Budget>, newCurrency: String) {
        viewModelScope.launch {
            newBudgets.forEach { repository.setBudget(it) }
            repository.setCurrency(newCurrency)
        }
    }

    fun updateCategoryOrder(newOrder: List<Category>) {
        viewModelScope.launch {
            val orderString = newOrder.joinToString(",") { it.name }
            repository.setCategoryOrder(orderString)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColor(enabled)
        }
    }

    fun exportData(uri: android.net.Uri, contentResolver: android.content.ContentResolver, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val expenses = repository.getAllExpensesOneShot()
                val budgets = repository.getAllBudgetsOneShot()
                val appData = com.h2.wellspend.data.AppData(expenses, budgets)
                
                val gson = com.google.gson.Gson()
                val jsonString = gson.toJson(appData)
                
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true, "Export successful")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importData(uri: android.net.Uri, contentResolver: android.content.ContentResolver, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    
                    val gson = com.google.gson.GsonBuilder()
                        .registerTypeAdapter(Category::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
                            try {
                                Category.valueOf(json.asString)
                            } catch (e: Exception) {
                                Category.Others
                            }
                        })
                        .create()
                        
                    val appData = gson.fromJson(jsonString, com.h2.wellspend.data.AppData::class.java)
                    
                    if (appData != null) {
                        repository.importData(appData.expenses, appData.budgets)
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, "Import successful")
                        }
                    } else {
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(false, "Import failed: Invalid data structure")
                        }
                    }
                }
            } catch (e: com.google.gson.JsonSyntaxException) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Import failed: Invalid JSON format")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.localizedMessage}")
                }
            }
        }
    }
}

class MainViewModelFactory(private val repository: WellSpendRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
