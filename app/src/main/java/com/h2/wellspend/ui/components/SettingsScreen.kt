package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.h2.wellspend.data.Currencies
import com.h2.wellspend.data.Currency
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem

// Removed AVAILABLE_CURRENCIES as we now use the full list


@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentCurrency: String,
    currentThemeMode: String,
    currentDynamicColor: Boolean,
    excludeLoanTransactions: Boolean,
    showAccountsOnHomepage: Boolean,
    onCurrencyChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExcludeLoanTransactionsChange: (Boolean) -> Unit,
    onShowAccountsOnHomepageChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    var selectedCurrency by remember(currentCurrency) { mutableStateOf(currentCurrency) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
            // Currency Settings
            SectionHeader("CURRENCY")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                var showCurrencyDialog by remember { mutableStateOf(false) }
                
                // Find current currency object or default
                val currencyObj = remember(selectedCurrency) {
                    if (selectedCurrency == "$") Currencies.find { it.code == "USD" }
                    else Currencies.find { it.symbol == selectedCurrency } ?: Currency("???", selectedCurrency, "Custom")
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showCurrencyDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Currency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currencyObj?.name ?: "Unknown"} (${currencyObj?.symbol ?: selectedCurrency})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }

                if (showCurrencyDialog) {
                    Dialog(
                        onDismissRequest = { showCurrencyDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(600.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            ) {
                                Text(
                                    "Select Currency", 
                                    style = MaterialTheme.typography.headlineSmall, 
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                var searchQuery by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Search") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val filteredCurrencies = remember(searchQuery) {
                                    if (searchQuery.isBlank()) Currencies
                                    else Currencies.filter { 
                                        it.name.contains(searchQuery, ignoreCase = true) || 
                                        it.code.contains(searchQuery, ignoreCase = true) || 
                                        it.symbol.contains(searchQuery, ignoreCase = true)
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredCurrencies) { currency ->
                                        val isSelected = selectedCurrency == currency.symbol
                                        Card(
                                            onClick = {
                                                selectedCurrency = currency.symbol
                                                onCurrencyChange(currency.symbol)
                                                showCurrencyDialog = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(currency.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    Text(currency.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text(currency.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { showCurrencyDialog = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            // Theme Settings
            SectionHeader("THEME")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Theme Mode
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Theme Mode", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    
                    var themeExpanded by remember { mutableStateOf(false) }
                    val themeOptions = listOf("System Default" to "SYSTEM", "Light" to "LIGHT", "Dark" to "DARK")
                    val selectedThemeLabel = themeOptions.find { it.second == currentThemeMode }?.first ?: "System Default"

                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = it }
                    ) {
                        Row(
                            modifier = Modifier
                                .menuAnchor()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { themeExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = "System Default",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Transparent
                                )
                                Text(
                                    text = selectedThemeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded)
                        }

                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            themeOptions.forEach { (label, mode) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onThemeModeChange(mode)
                                        themeExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                // Dynamic Color
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dynamic Color", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text("Use wallpaper colors", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = currentDynamicColor,
                        onCheckedChange = onDynamicColorChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }



            // Summary Configuration
            SectionHeader("SUMMARY CONFIGURATION")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Show Accounts on Homepage
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowAccountsOnHomepageChange(!showAccountsOnHomepage) }
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Accounts in Homepage", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text(
                            "Display your accounts and balances on the dashboard",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = showAccountsOnHomepage,
                        onCheckedChange = onShowAccountsOnHomepageChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exclude loan transactions", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text(
                            "Exclude loan transactions from total expense and income calculations. Total balance will remain unaffected.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = excludeLoanTransactions,
                        onCheckedChange = onExcludeLoanTransactionsChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Data Management
            SectionHeader("DATA MANAGEMENT")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        onClick = onExport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Data", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = onImport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import Data", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}
