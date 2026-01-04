package com.h2.wellspend.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Currencies
import com.h2.wellspend.data.Currency
import java.util.UUID
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowForward

enum class OnboardingStep {
    WELCOME,
    THEME,
    CURRENCY,
    START_OPTION,
    CREATE_ACCOUNT,
    RESTORE_BACKUP,
    COMPLETE
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onThemeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onImportData: (android.net.Uri) -> Unit,
    onCreateAccount: (Account) -> Unit,
    existingAccounts: List<Account>, // Receive observed accounts
    initialTheme: String,
    initialDynamicColor: Boolean
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var slideDirection by remember { mutableStateOf(AnimatedContentTransitionScope.SlideDirection.Left) }
    
    // State for selections
    var selectedTheme by remember { mutableStateOf(initialTheme) }
    var isDynamicColor by remember { mutableStateOf(initialDynamicColor) }
    
    // Currency State
    var selectedCurrencySymbol by remember { mutableStateOf("") }
    var selectedCurrencyCode by remember { mutableStateOf("") }
    
    // Start Option State
    var selectedStartOption by remember { mutableStateOf<String?>(null) } // "FRESH" or "RESTORE"

    // Account Creation State
    // Removed local createdAccounts state, using existingAccounts
    var accountName by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    
    // State for Backup
    var importStatus by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importStatus = "Importing..."
            onImportData(uri)
            currentStep = OnboardingStep.COMPLETE
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Navigation Buttons (Skip/Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep != OnboardingStep.WELCOME && currentStep != OnboardingStep.COMPLETE) {
                    TextButton(onClick = {
                        slideDirection = AnimatedContentTransitionScope.SlideDirection.Right
                        currentStep = when (currentStep) {
                            OnboardingStep.THEME -> OnboardingStep.WELCOME
                            OnboardingStep.CURRENCY -> OnboardingStep.THEME
                            OnboardingStep.START_OPTION -> OnboardingStep.CURRENCY
                            OnboardingStep.CREATE_ACCOUNT -> OnboardingStep.START_OPTION
                            OnboardingStep.RESTORE_BACKUP -> OnboardingStep.START_OPTION
                            else -> OnboardingStep.WELCOME
                        }
                    }) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp)) // Placeholder
                }
                
                // Show Next button for most steps, but handle PRE-CONDITIONS
                if (currentStep != OnboardingStep.COMPLETE && currentStep != OnboardingStep.RESTORE_BACKUP) {
                    val isNextEnabled = when(currentStep) {
                        OnboardingStep.CURRENCY -> selectedCurrencyCode.isNotEmpty()
                        OnboardingStep.START_OPTION -> selectedStartOption != null
                        OnboardingStep.CREATE_ACCOUNT -> existingAccounts.isNotEmpty()
                        else -> true
                    }
                    
                    Button(
                        onClick = {
                            slideDirection = AnimatedContentTransitionScope.SlideDirection.Left
                            currentStep = when (currentStep) {
                                OnboardingStep.WELCOME -> OnboardingStep.THEME
                                OnboardingStep.THEME -> OnboardingStep.CURRENCY
                                OnboardingStep.CURRENCY -> OnboardingStep.START_OPTION
                                OnboardingStep.START_OPTION -> {
                                    if (selectedStartOption == "RESTORE") OnboardingStep.RESTORE_BACKUP 
                                    else OnboardingStep.CREATE_ACCOUNT
                                }
                                OnboardingStep.CREATE_ACCOUNT -> OnboardingStep.COMPLETE
                                else -> OnboardingStep.COMPLETE
                            }
                        },
                        enabled = isNextEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
             // ... existing modifiers
             modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (Progress Indicator and AnimatedContent structure remains same) ...
             val progress = when(currentStep) {
                OnboardingStep.WELCOME -> 0.1f
                OnboardingStep.THEME -> 0.3f
                OnboardingStep.CURRENCY -> 0.5f
                OnboardingStep.START_OPTION -> 0.7f
                OnboardingStep.CREATE_ACCOUNT -> 0.9f
                OnboardingStep.RESTORE_BACKUP -> 0.9f
                OnboardingStep.COMPLETE -> 1.0f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary, 
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideIntoContainer(slideDirection, animationSpec = tween(400)) togetherWith
                    slideOutOfContainer(slideDirection, animationSpec = tween(400))
                },
                label = "OnboardingTransition"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    when (step) {
                        // ... (WELCOME, THEME, CURRENCY, START_OPTION steps unchanged in this tool call scope, will be preserved by replace_file_content if I target correctly?)
                        OnboardingStep.WELCOME -> {
                            // ... content ...
                            Icon(
                                Icons.Default.Wallet, 
                                contentDescription = null, 
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                "Welcome to WellSpend",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Track your expenses, manage budgets, and achieve your financial goals with ease.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OnboardingStep.THEME -> {
                             // ... content ...
                            Text("Choose your Style", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            val themes = listOf("LIGHT", "DARK", "SYSTEM")
                            val icons = listOf(Icons.Default.LightMode, Icons.Default.DarkMode, Icons.Default.SettingsSuggest)
                            val labels = listOf("Light", "Dark", "System Default")
                            
                            themes.forEachIndexed { index, theme ->
                                val isSelected = selectedTheme == theme
                                Card(
                                    onClick = { 
                                        selectedTheme = theme
                                        onThemeChange(theme)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icons[index], contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(labels[index], style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            // Dynamic Color Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        isDynamicColor = !isDynamicColor
                                        onDynamicColorChange(isDynamicColor)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Dynamic Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Use wallpaper colors (Material You)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isDynamicColor,
                                    onCheckedChange = { 
                                        isDynamicColor = it
                                        onDynamicColorChange(it)
                                    }
                                )
                            }
                        }
                        OnboardingStep.CURRENCY -> {
                            // ... content ...
                            Text("Select Currency", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            var searchQuery by remember { mutableStateOf("") }
                            
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search Currency") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            val filteredCurrencies = remember(searchQuery) {
                                if (searchQuery.isBlank()) Currencies
                                else Currencies.filter { 
                                    it.name.contains(searchQuery, ignoreCase = true) || 
                                    it.code.contains(searchQuery, ignoreCase = true) || 
                                    it.symbol.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredCurrencies.size) { index ->
                                    val currency = filteredCurrencies[index]
                                    val isSelected = selectedCurrencyCode == currency.code // Strict comparison by CODE
                                    
                                    Card(
                                        onClick = {
                                            selectedCurrencyCode = currency.code
                                            selectedCurrencySymbol = currency.symbol
                                            onCurrencyChange(currency.symbol)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                             containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        ),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                         Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                             Text(
                                                 text = "${currency.name} (${currency.code})", 
                                                 style = MaterialTheme.typography.bodyLarge,
                                                 modifier = Modifier.weight(1f)
                                             )
                                             Text(
                                                 text = currency.symbol,
                                                 style = MaterialTheme.typography.titleMedium,
                                                 fontWeight = FontWeight.Bold,
                                                 color = MaterialTheme.colorScheme.primary
                                             )
                                              if (isSelected) {
                                                  Spacer(modifier = Modifier.width(16.dp))
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                                
                                if (filteredCurrencies.isEmpty()) {
                                    item {
                                        Text(
                                            "No currencies found.",
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        OnboardingStep.START_OPTION -> {
                             // ... content ...
                            Text("Let's Get Started", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            val isFreshSelected = selectedStartOption == "FRESH"
                            val isRestoreSelected = selectedStartOption == "RESTORE"

                            Card(
                                onClick = { selectedStartOption = "FRESH" },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFreshSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isFreshSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(32.dp), tint = if (isFreshSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Start Fresh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isFreshSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                        Text("Create a new account", style = MaterialTheme.typography.bodyMedium, color = if (isFreshSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isFreshSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                             Card(
                                onClick = { selectedStartOption = "RESTORE" },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isRestoreSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isRestoreSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(32.dp), tint = if (isRestoreSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Restore Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isRestoreSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                        Text("Import data from file", style = MaterialTheme.typography.bodyMedium, color = if (isRestoreSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                     if (isRestoreSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                        OnboardingStep.CREATE_ACCOUNT -> {
                            Text("Create Accounts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Create at least one account to get started (e.g., Bank, Cash, Savings).",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            OutlinedTextField(
                                value = accountName,
                                onValueChange = { accountName = it },
                                label = { Text("Account Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = initialBalance,
                                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) initialBalance = it },
                                label = { Text("Initial Balance") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Text(selectedCurrencySymbol, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Add Account Button
                            Button(
                                onClick = {
                                    if (accountName.isNotBlank()) {
                                        val account = Account(
                                            id = UUID.randomUUID().toString(),
                                            name = accountName,
                                            initialBalance = initialBalance.toDoubleOrNull() ?: 0.0,
                                            feeConfigs = emptyList(),
                                            sortOrder = existingAccounts.size // Use existingAccounts size
                                        )
                                        onCreateAccount(account)
                                        // removed local list update
                                        // Reset fields
                                        accountName = ""
                                        initialBalance = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = accountName.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Account")
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // List of Created Accounts
                            if (existingAccounts.isNotEmpty()) { // Use existingAccounts
                                Text("Added Accounts:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(existingAccounts.size) { index ->
                                        val acc = existingAccounts[index]
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(acc.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text("$selectedCurrencySymbol${acc.initialBalance}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        OnboardingStep.RESTORE_BACKUP -> {
                            Text("Restore Data", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Select a valid backup JSON file to restore your expenses, accounts, and settings.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = { importLauncher.launch(arrayOf("application/json")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Backup File")
                            }
                            
                            if (importStatus != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(importStatus!!, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        OnboardingStep.COMPLETE -> {
                            Icon(
                                Icons.Default.CheckCircle, 
                                contentDescription = null, 
                                modifier = Modifier.size(120.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("You're All Set!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Your app is ready to use. Start tracking your expenses now!",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(48.dp))
                            
                            Button(
                                onClick = onComplete,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Go to Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
