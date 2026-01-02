package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.h2.wellspend.ui.getCategoryIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseForm(
    currency: String,
    accounts: List<com.h2.wellspend.data.Account>,
    categories: List<Category>,
    onAdd: (Double, String, Category?, String, Boolean, RecurringFrequency, com.h2.wellspend.data.TransactionType, String?, String?, Double, String?) -> Unit,
    onCancel: () -> Unit,
    onReorder: (List<Category>) -> Unit,
    initialExpense: Expense? = null
) {
    var amount by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(initialExpense?.description ?: "") }
    var category by remember { mutableStateOf(initialExpense?.category ?: Category.Food) }
    var date by remember { mutableStateOf(initialExpense?.date?.substring(0, 10) ?: LocalDate.now().toString()) } // YYYY-MM-DD
    var isRecurring by remember { mutableStateOf(initialExpense?.isRecurring ?: false) }
    var frequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // New State
    var transactionType by remember { mutableStateOf(initialExpense?.transactionType ?: com.h2.wellspend.data.TransactionType.EXPENSE) }
    var accountId by remember { mutableStateOf(if (initialExpense != null) initialExpense.accountId else null) }
    var targetAccountId by remember { mutableStateOf(if (initialExpense != null) initialExpense.transferTargetAccountId else null) }
    
    // Fee State
    var selectedFeeConfigName by remember { mutableStateOf<String?>(initialExpense?.feeConfigName) } // "Custom" or config name
    var feeAmount by remember { mutableStateOf(initialExpense?.feeAmount?.toString() ?: "0.0") }
    var isCustomFee by remember { mutableStateOf(initialExpense?.feeConfigName == "Custom") }

    // Helper to calculate fee based on account rule
    val currentAccount = accounts.find { it.id == accountId }
    
    // Auto-update fee when account or amount changes, unless custom
    LaunchedEffect(amount, accountId, selectedFeeConfigName) {
        if (!isCustomFee && selectedFeeConfigName != null && selectedFeeConfigName != "None" && selectedFeeConfigName != "Custom") {
            val config = currentAccount?.feeConfigs?.find { it.name == selectedFeeConfigName }
            if (config != null) {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val calculated = if (config.isPercentage) (amt * config.value / 100) else config.value
                feeAmount = String.format("%.2f", calculated)
            }
        } else if (selectedFeeConfigName == "None") {
            feeAmount = "0.0"
        }
    }

    // Filter out TransactionFee and Loan from selection
    val filteredCategories = remember(categories) {
        categories.filter { it != Category.TransactionFee && it != Category.Loan }
    }

    // Ensure selected category is valid
    LaunchedEffect(filteredCategories) {
        if (!filteredCategories.contains(category) && filteredCategories.isNotEmpty()) {
            category = filteredCategories.first()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (initialExpense != null) "Edit Transaction" else "New Transaction",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(32.dp))
        }

        // Transaction Type Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            com.h2.wellspend.data.TransactionType.values().forEach { type ->
                val isSelected = transactionType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable(enabled = initialExpense == null) { transactionType = type }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.name.lowercase().capitalize(),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (initialExpense != null) 0.3f else 1f),
                        style = MaterialTheme.typography.labelLarge
                    )
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
            // Amount Input
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currency,
                        style = TextStyle(fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    TextField(
                        value = amount,
                        onValueChange = { amount = it },
                        textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                        placeholder = { Text("0", style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
            }

            // Account Selection
            Text(if (transactionType == com.h2.wellspend.data.TransactionType.INCOME) "To Account" else "From Account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accountId == null || (initialExpense != null && initialExpense.accountId == null)) {
                    FilterChip(
                        selected = accountId == null,
                        onClick = { accountId = null },
                        label = { Text("Deleted Account", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                    )
                }
                accounts.forEach { acc ->
                    FilterChip(
                        selected = accountId == acc.id,
                        onClick = { accountId = acc.id },
                        label = { Text(acc.name) },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (transactionType == com.h2.wellspend.data.TransactionType.TRANSFER) {
                Text("To Account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (targetAccountId == null || (initialExpense != null && initialExpense.transferTargetAccountId == null)) {
                        FilterChip(
                            selected = targetAccountId == null,
                            onClick = { targetAccountId = null },
                            label = { Text("Deleted Target", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    accounts.filter { it.id != accountId }.forEach { acc ->
                        FilterChip(
                            selected = targetAccountId == acc.id,
                            onClick = { targetAccountId = acc.id },
                            label = { Text(acc.name) },
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if (transactionType == com.h2.wellspend.data.TransactionType.EXPENSE) {
                // Category Selection only for Expense
                Text("Category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
                CategoryGrid(
                    categories = filteredCategories,
                    selectedCategory = category,
                    onCategorySelected = { category = it },
                    expanded = expanded,
                    onExpandChange = { expanded = it },
                    onReorder = onReorder
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fees (For Expense and Transfer)
            if (transactionType != com.h2.wellspend.data.TransactionType.INCOME) {
                Text("Transaction Fees", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedFeeConfigName == "None" || selectedFeeConfigName == null, onClick = { selectedFeeConfigName = "None"; isCustomFee = false }, label = { Text("None") })
                    
                    currentAccount?.feeConfigs?.forEach { config ->
                        FilterChip(
                            selected = selectedFeeConfigName == config.name,
                            onClick = { selectedFeeConfigName = config.name; isCustomFee = false },
                            label = { Text("${config.name} (${if(config.isPercentage) "${config.value}%" else currency + config.value})") }
                        )
                    }
                    
                    FilterChip(selected = isCustomFee, onClick = { selectedFeeConfigName = "Custom"; isCustomFee = true }, label = { Text("Custom") })
                }
                
                if (isCustomFee || (feeAmount.toDoubleOrNull() ?: 0.0) > 0) {
                     Spacer(modifier = Modifier.height(8.dp))
                     OutlinedTextField(
                        value = feeAmount,
                        onValueChange = { feeAmount = it; if(!isCustomFee) isCustomFee = true; selectedFeeConfigName = "Custom" },
                        label = { Text("Fee Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                     )
                }
                
                val total = (amount.toDoubleOrNull() ?: 0.0) + (feeAmount.toDoubleOrNull() ?: 0.0)
                if (total > 0 && (feeAmount.toDoubleOrNull() ?: 0.0) > 0) {
                     Text(
                        text = "Total: $currency${String.format("%.2f", amount.toDoubleOrNull() ?: 0.0)} + $currency${String.format("%.2f", feeAmount.toDoubleOrNull() ?: 0.0)} (Fee) = $currency${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.padding(top = 8.dp)
                     )
                } else if (total > 0) {
                     Text("Total Deduction: $currency${String.format("%.2f", total)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Description & Date
            Text("Description", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text(if(transactionType == com.h2.wellspend.data.TransactionType.INCOME) "Income Source" else "Description") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            Box(modifier = Modifier.clickable { showDatePicker = true }) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recurring (Only for Expenses/Income? Maybe Transfers too? Let's allow all)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text("Recurring", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Switch(checked = isRecurring, onCheckedChange = { isRecurring = it }, modifier = Modifier.scale(0.8f))
                    }
                    if (isRecurring) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FrequencyButton("Weekly", frequency == RecurringFrequency.WEEKLY, { frequency = RecurringFrequency.WEEKLY }, Modifier.weight(1f))
                            FrequencyButton("Monthly", frequency == RecurringFrequency.MONTHLY, { frequency = RecurringFrequency.MONTHLY }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null) {
                        val feeVal = feeAmount.toDoubleOrNull() ?: 0.0
                        onAdd(amountVal, description,
                            if(transactionType == com.h2.wellspend.data.TransactionType.EXPENSE) category else null,
                            date, isRecurring, frequency,
                            transactionType, accountId, targetAccountId, feeVal, selectedFeeConfigName)
                    }
                },
                enabled = amount.isNotEmpty() && accountId != null && (transactionType != com.h2.wellspend.data.TransactionType.TRANSFER || targetAccountId != null),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Save Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onReorder: (List<Category>) -> Unit
) {
    val visibleCount = if (expanded) categories.size else 8
    val visibleCategories = categories.take(visibleCount)
    
    val itemsPerRow = 4
    val itemHeight = 65.dp
    val spacing = 10.dp
    
    // Drag state
    var draggedItem by remember { mutableStateOf<Category?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    
    val rows = (visibleCategories.size + itemsPerRow - 1) / itemsPerRow
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
                val draggedIndex = visibleCategories.indexOf(draggedItem)
                var hoverIndex by remember { mutableIntStateOf(-1) }
                
                if (draggedItem != null && draggedIndex != -1) {
                    val startPos = getPosition(draggedIndex)
                    val currentCenterX = startPos.x + dragOffset.x + itemWidthPx / 2
                    val currentCenterY = startPos.y + dragOffset.y + itemHeightPx / 2
                    
                    // Find closest slot
                    var closestIndex = -1
                    var minDistance = Float.MAX_VALUE
                    
                    for (i in visibleCategories.indices) {
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

                visibleCategories.forEachIndexed { index, cat ->
                    key(cat) {
                        val isDragged = draggedItem == cat
                        val isSelected = selectedCategory == cat
                        
                        // Calculate visual target position
                        var targetIndex = index
                        if (draggedItem != null && draggedIndex != -1 && hoverIndex != -1) {
                            if (index == draggedIndex) {
                                targetIndex = index 
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
                                val startPos = getPosition(index)
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
                                .pointerInput(visibleCategories) {
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
                                            val currentDraggedIndex = visibleCategories.indexOf(cat)
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
                                    imageVector = getCategoryIcon(cat),
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
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
