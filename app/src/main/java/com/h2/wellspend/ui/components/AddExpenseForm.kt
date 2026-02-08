package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.draw.clip
import kotlin.math.roundToInt
import kotlin.math.pow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import com.h2.wellspend.data.Category
import com.h2.wellspend.data.Expense
import com.h2.wellspend.data.RecurringFrequency
import com.h2.wellspend.ui.getIconByName
import com.h2.wellspend.data.SystemCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseForm(
    currency: String,
    accounts: List<com.h2.wellspend.data.Account>,
    accountBalances: Map<String, Double>,
    categories: List<Category>,
    onAdd: (Double, String, Category?, String, Boolean, RecurringFrequency, com.h2.wellspend.data.TransactionType, String?, String?, Double, String?, String?) -> Unit,
    onCancel: () -> Unit,
    onReorder: (List<Category>) -> Unit,
    onAddCategory: (Category) -> Unit,
    initialExpense: Expense? = null,
    initialTransactionType: com.h2.wellspend.data.TransactionType? = null
) {
    var amount by remember { mutableStateOf(initialExpense?.amount?.let { String.format("%.2f", it).trimEnd('0').trimEnd('.') } ?: "") }
    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var note by remember { mutableStateOf(initialExpense?.note ?: "") }
    var category by remember { 
        mutableStateOf(
            categories.find { it.name == initialExpense?.category } 
        ) 
    }
    var date by remember { mutableStateOf(initialExpense?.date?.substring(0, 10) ?: LocalDate.now().toString()) } // YYYY-MM-DD

    var frequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var isRecurring by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(initialExpense != null) } // Expand if editing
    val datePickerState = rememberDatePickerState()

    // New State
    var transactionType by remember { mutableStateOf(initialExpense?.transactionType ?: initialTransactionType ?: com.h2.wellspend.data.TransactionType.EXPENSE) }
    var accountId by remember { mutableStateOf(if (initialExpense != null) initialExpense.accountId else null) }
    var targetAccountId by remember { mutableStateOf(if (initialExpense != null) initialExpense.transferTargetAccountId else null) }
    
    // Fee State
    var selectedFeeConfigName by remember { mutableStateOf<String?>(initialExpense?.feeConfigName) } // "Custom" or config name
    var feeAmount by remember { mutableStateOf(initialExpense?.feeAmount?.toString() ?: "0.0") }
    var isCustomFee by remember { mutableStateOf(initialExpense?.feeConfigName == "Custom") }
    
    // Confirmation dialog state for saving without account
    var showNoAccountConfirmation by remember { mutableStateOf(false) }

    // Helper to calculate fee based on account rule
    val currentAccount = accounts.find { it.id == accountId }
    
    // Auto-update fee when account or amount changes, unless custom
    // Fee calculation moved to FeeSelector interaction

    // Filter out TransactionFee and Loan from selection
    // Filter out System categories from selection (Loan, TransactionFee, BalanceAdjustment, Others)
    // Filter out System categories from selection (Loan, TransactionFee, BalanceAdjustment, Others)
    val filteredCategories = remember(categories) {
        val systemNames = setOf("Others", "Loan", "TransactionFee", "BalanceAdjustment")
        categories.filter { !it.isSystem && !systemNames.contains(it.name) }
    }

    // Ensure selected category is valid
    // Update category if selected category becomes invalid or removed
    LaunchedEffect(filteredCategories) {
        if (category != null && !filteredCategories.contains(category)) {
            category = null
        }
    }

    // Sync DatePicker state with current date (handle arrow navigation)
    LaunchedEffect(date) {
        try {
            val localDate = LocalDate.parse(date)
            datePickerState.selectedDateMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            // Ignore parse errors
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddCategoryDialog) {
        CategoryDialog(
            initialCategory = null,
            usedCategoryNames = categories.map { it.name }.toSet(),
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { newCategory ->
                onAddCategory(newCategory)
                category = newCategory // Auto select
                showAddCategoryDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            
            if (initialExpense != null) {
                Text(
                    text = "Edit ${transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                Text(
                    text = "New",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Transaction Type Dropdown
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .width(140.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(8.dp))
                            .clickable { showTypeDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = transactionType.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    DropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false },
                        modifier = Modifier.width(140.dp)
                    ) {
                        com.h2.wellspend.data.TransactionType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    transactionType = type
                                    showTypeDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }



        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        .padding(top = 16.dp, bottom = 16.dp)
        ) {
            // Date Navigation (Moved Inside Scroll)
            Row(
                 modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 4.dp),
                 verticalAlignment = Alignment.CenterVertically,
                 horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { 
                         val localDate = LocalDate.parse(date)
                         date = localDate.minusDays(1).toString()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Day", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                 Box(
                     modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp, horizontal = 12.dp), // Decreased vertical padding
                    contentAlignment = Alignment.Center
                 ) {
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.SpaceBetween,
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         Text(
                             text = try {
                                 LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
                             } catch (e: Exception) {
                                 date
                             },
                             style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Bold
                         )
                         Icon(
                             Icons.Default.ArrowDropDown, 
                             contentDescription = "Select Date",
                             tint = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     }
                 }

                 IconButton(
                     onClick = { 
                         val localDate = LocalDate.parse(date)
                         date = localDate.plusDays(1).toString()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            // Amount Input (Original Style)
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currency,
                        style = TextStyle(fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Allow only valid decimal input with max 2 decimal places
                            val filtered = newValue.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split(".")
                            amount = when {
                                parts.size == 1 -> filtered // No decimal point
                                parts.size == 2 -> "${parts[0]}.${parts[1].take(2)}" // Limit to 2 decimal places
                                else -> amount // Invalid (multiple dots), keep previous value
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (amount.isEmpty()) {
                                    Text(
                                        "0",
                                        style = TextStyle(
                                            fontSize = 56.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.widthIn(min = 200.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))  

            // Title (Moved here)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if(transactionType == com.h2.wellspend.data.TransactionType.INCOME) "Income Source" else "Title") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Account Selection
            AccountSelector(
                accounts = accounts,
                accountBalances = accountBalances,
                selectedAccountId = accountId,
                onAccountSelected = { accountId = it },
                currency = currency,
                title = if (transactionType == com.h2.wellspend.data.TransactionType.INCOME) "To Account" else "From Account"
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (transactionType == com.h2.wellspend.data.TransactionType.TRANSFER) {
                AccountSelector(
                    accounts = accounts.filter { it.id != accountId },
                    accountBalances = accountBalances,
                    selectedAccountId = targetAccountId,
                    onAccountSelected = { targetAccountId = it },
                    currency = currency,
                    title = "To Account"
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            if (transactionType == com.h2.wellspend.data.TransactionType.EXPENSE) {
                // Category Selection
                CategoryGrid(
                    categories = filteredCategories,
                    selectedCategory = category,
                    onCategorySelected = { 
                        category = if (category == it) null else it 
                    },
                    expanded = expanded,
                    onExpandChange = { expanded = it },
                    onReorder = onReorder,
                    onAddCategoryClick = { showAddCategoryDialog = true }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }



            // Advanced Options Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedOptions = !showAdvancedOptions }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Advanced Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (showAdvancedOptions) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (showAdvancedOptions) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showAdvancedOptions,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                Column {
                    // Fees (only show when account is selected)
                    if (transactionType != com.h2.wellspend.data.TransactionType.INCOME && accountId != null) {
                        FeeSelector(
                            account = currentAccount,
                            transactionAmount = amount.toDoubleOrNull() ?: 0.0,
                            currency = currency,
                            selectedConfigName = selectedFeeConfigName,
                            currentFeeAmount = feeAmount,
                            isCustomFee = isCustomFee,
                            onFeeChanged = { name, amt, isCustom ->
                                selectedFeeConfigName = name
                                feeAmount = amt
                                isCustomFee = isCustom
                            }
                        )
                        
                        val total = (amount.toDoubleOrNull() ?: 0.0) + (feeAmount.toDoubleOrNull() ?: 0.0)
                        if (total > 0 && (feeAmount.toDoubleOrNull() ?: 0.0) > 0) {
                             Text(
                                text = "Total Deduction: $currency${String.format("%.2f", total)}",
                                style = MaterialTheme.typography.bodyMedium, 
                                fontWeight = FontWeight.SemiBold, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                             )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Note
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 300) note = it },
                        label = { Text("Note") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        supportingText = {
                            Text(
                                text = "${note.length}/300",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Recurring Transaction Toggle
                    if (initialExpense == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recurring Transaction",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it }
                            )
                        }
                        
                        // Frequency Selector (visible when recurring is enabled)
                        if (isRecurring) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RecurringFrequency.entries.forEach { freq ->
                                    FilterChip(
                                        selected = frequency == freq,
                                        onClick = { frequency = freq },
                                        label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
        ) {
            // Helper function to perform the save
            val performSave = {
                val amountVal = amount.toDoubleOrNull()
                if (amountVal != null) {
                    val feeVal = if (accountId != null) feeAmount.toDoubleOrNull() ?: 0.0 else 0.0
                    val finalFeeConfigName = if (accountId != null) selectedFeeConfigName else null
                    val finalCategory = if(transactionType == com.h2.wellspend.data.TransactionType.EXPENSE) {
                         category ?: categories.find { it.name == SystemCategory.Others.name }
                    } else null
                    
                    onAdd(amountVal, title,
                        finalCategory,
                        date, isRecurring, frequency,
                        transactionType, accountId, targetAccountId, feeVal, finalFeeConfigName, note)
                }
            }
            
            Button(
                onClick = {
                    if (accountId == null && transactionType != com.h2.wellspend.data.TransactionType.TRANSFER) {
                        showNoAccountConfirmation = true
                    } else {
                        performSave()
                    }
                },
                enabled = amount.isNotEmpty() && (transactionType != com.h2.wellspend.data.TransactionType.TRANSFER || (accountId != null && targetAccountId != null)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Save Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            // Confirmation dialog for no account
            if (showNoAccountConfirmation) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showNoAccountConfirmation = false },
                    title = { Text("No Account Selected") },
                    text = { Text("This transaction will not be linked to any account and won't affect account balances. Continue?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showNoAccountConfirmation = false
                            performSave()
                        }) {
                            Text("Save Anyway")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNoAccountConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun FrequencyButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
        .clip(RoundedCornerShape(8.dp))
        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onReorder: (List<Category>) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    val totalCategories = categories.size
    val totalItems = totalCategories + 1 // +1 for Add button
    val showExpandButton = totalItems > 12
    val visibleCount = if (showExpandButton && !expanded) 8 else totalItems
    
    val itemsPerRow = 4
    val itemHeight = 65.dp
    val spacing = 10.dp
    
    // Drag state
    var draggedItem by remember { mutableStateOf<Category?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    
    val rows = (visibleCount + itemsPerRow - 1) / itemsPerRow
    val gridHeight = (itemHeight * rows) + (spacing * (rows - 1))
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints {
                val itemWidth = (maxWidth - (spacing * (itemsPerRow - 1))) / itemsPerRow
                val itemWidthPx = with(density) { itemWidth.toPx() }
                val itemHeightPx = with(density) { itemHeight.toPx() }
                val spacingPx = with(density) { spacing.toPx() }
                
                // Helper to get position for an index
                fun getPosition(index: Int): Offset {
                    val row = index / itemsPerRow
                    val col = index % itemsPerRow
                    val x = (itemWidthPx + spacingPx) * col
                    val y = (itemHeightPx + spacingPx) * row
                    return Offset(x, y)
                }

                // Calculate hover index
                // Note: We only allow reordering actual categories, not the add button
                val draggedIndex = if (draggedItem != null) categories.indexOf(draggedItem) else -1
                var hoverIndex by remember { mutableIntStateOf(-1) }
                
                if (draggedItem != null && draggedIndex != -1) {
                    val startPos = getPosition(draggedIndex)
                    val currentCenterX = startPos.x + dragOffset.x + itemWidthPx / 2
                    val currentCenterY = startPos.y + dragOffset.y + itemHeightPx / 2
                    

                    // Find closest slot (only consider valid category slots)
                    var closestIndex = -1
                    var minDistance = Float.MAX_VALUE
                    
                    for (i in 0 until categories.size) { // Only iterate through actual categories for drop targets
                        val pos = getPosition(i)
                        val centerX = pos.x + itemWidthPx / 2
                        val centerY = pos.y + itemHeightPx / 2
                        val dist = (currentCenterX - centerX).pow(2) + (currentCenterY - centerY).pow(2)
                        
                        if (dist < minDistance) {
                            minDistance = dist
                            closestIndex = i
                        }
                    }
                    hoverIndex = closestIndex
                } else {
                    hoverIndex = -1
                }



                for (index in 0 until visibleCount) {
                    // Check if it's the Add button
                    if (index == categories.size) {
                        val pos = getPosition(index)
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                                .width(itemWidth)
                                .height(itemHeight)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onAddCategoryClick() }
                                .padding(2.dp, 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Category",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Add",
                                    style = TextStyle(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        val cat = categories[index]
                        key(cat.name) { // Use unique key
                        val isDragged = draggedItem == cat
                        val isSelected = selectedCategory == cat
                        
                        // Calculate visual target position
                        var targetIndex = index
                        if (draggedItem != null && draggedIndex != -1 && hoverIndex != -1) {
                            if (index == draggedIndex) {
                                targetIndex = index // Actually drag handles position, but for logic consistency
                            } else if (draggedIndex < hoverIndex) {
                                if (index > draggedIndex && index <= hoverIndex) {
                                    targetIndex = index - 1
                                }
                            } else if (draggedIndex > hoverIndex) {
                                if (index >= hoverIndex && index < draggedIndex) {
                                    targetIndex = index + 1
                                }
                            }
                        }
                        
                        // Don't animate category items into the add button slot or vice versa automatically
                        // The add button is always at categories.size
                        
                        val targetPos = getPosition(targetIndex)
                        
                        // Use Animatable for smooth transitions and snap support
                        val animatedOffset = remember { androidx.compose.animation.core.Animatable(targetPos, androidx.compose.ui.geometry.Offset.VectorConverter) }
                        
                        LaunchedEffect(targetPos) {
                            if (!isDragged) {
                                animatedOffset.animateTo(targetPos)
                            }
                        }
                        
                        // Snap to drag position while dragging to keep Animatable in sync
                        LaunchedEffect(isDragged, dragOffset) {
                            if (isDragged) {
                                val startPos = getPosition(index) // Use original index for start pos calculation
                                animatedOffset.snapTo(startPos + dragOffset)
                            }
                        }
                        
                        val zIndex = if (isDragged) 1f else 0f
                        
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(animatedOffset.value.x.roundToInt(), animatedOffset.value.y.roundToInt()) }
                                .width(itemWidth)
                                .height(itemHeight)
                                .zIndex(zIndex)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .pointerInput(categories) { // Key on categories list for drag detection updates
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedItem = cat
                                            dragOffset = Offset.Zero
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                            change.consume()
                                            dragOffset += dragAmount
                                        },
                                        onDragEnd = {
                                            val currentDraggedIndex = categories.indexOf(cat) // Re-calculate index from source list
                                            if (currentDraggedIndex != -1 && hoverIndex != -1 && currentDraggedIndex != hoverIndex) {
                                                // Commit reorder
                                                val newList = categories.toMutableList()
                                                val item = newList.removeAt(currentDraggedIndex)
                                                newList.add(hoverIndex, item)
                                                onReorder(newList)
                                            }
                                            draggedItem = null
                                            dragOffset = Offset.Zero
                                            hoverIndex = -1
                                        },
                                        onDragCancel = {
                                            draggedItem = null
                                            dragOffset = Offset.Zero
                                            hoverIndex = -1
                                        }
                                    )
                                }
                                .clickable { 
                                    if (draggedItem == null) {
                                        onCategorySelected(cat) 
                                    }
                                }
                                .padding(2.dp, 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = getIconByName(cat.iconName),
                                    contentDescription = cat.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = cat.name,
                                    style = TextStyle(fontSize = 10.sp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } // End of key
                  } // End of else
                } // End of loop


            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (showExpandButton) {
            TextButton(onClick = { onExpandChange(!expanded) }) {
                Text(
                    text = if (expanded) "Show Less" else "Show More",
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
