package com.h2.wellspend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Close
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.FeeConfig
import java.util.UUID
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountScreen(
    accounts: List<Account>,
    balances: Map<String, Double>,
    currency: String,
    onDeleteAccount: (Account) -> Unit,
    isAccountUsed: (String) -> Boolean,
    onReorder: (List<Account>) -> Unit,
    onAdjustBalance: (String, Double) -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (Account) -> Unit
) {
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    
    // Reorder mode state
    var isReorderMode by remember { mutableStateOf(false) }
    
    // Internal dialog state removed - hoisted to MainScreen

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No accounts yet. Add one to track balances.", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Reorder mode toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilterChip(
                        selected = isReorderMode,
                        onClick = { isReorderMode = !isReorderMode },
                        label = { Text(if (isReorderMode) "Done" else "Reorder") },
                        leadingIcon = if (isReorderMode) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else {
                            { Icon(Icons.Default.SwapVert, null, modifier = Modifier.size(16.dp)) }
                        }
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                        Box(modifier = Modifier.animateItemPlacement()) {
                            AccountItem(
                                account = account,
                                balance = balances[account.id] ?: account.initialBalance,
                                currency = currency,
                                isReorderMode = isReorderMode,
                                canMoveUp = index > 0,
                                canMoveDown = index < accounts.size - 1,
                                onEdit = { onEditAccount(account) },
                                onDelete = { accountToDelete = account },
                                onMoveUp = {
                                    if (index > 0) {
                                        val newList = accounts.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index - 1, item)
                                        onReorder(newList)
                                    }
                                },
                                onMoveDown = {
                                    if (index < accounts.size - 1) {
                                        val newList = accounts.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index + 1, item)
                                        onReorder(newList)
                                    }
                                }
                            )
                        }
                    }
                    item {
                         Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isReorderMode) "Use arrows to reorder" else "Swipe to edit/delete • Tap Reorder to change order",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        if (accountToDelete != null) {
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                title = { Text("Delete Account") },
                text = { Text("Are you sure you want to delete '${accountToDelete?.name}'?\n\nExisting expenses will not be deleted, but they will be unlinked from this account.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            accountToDelete?.let { onDeleteAccount(it) }
                            accountToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Composable
fun AccountItem(
    account: Account,
    balance: Double,
    currency: String,
    isReorderMode: Boolean = false,
    canMoveUp: Boolean = true,
    canMoveDown: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Reset swipe when entering reorder mode
    LaunchedEffect(isReorderMode) {
        if (isReorderMode) {
            offsetX.animateTo(0f)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Background (Actions) - only visible when NOT in reorder mode
        if (!isReorderMode) {
            Box(modifier = Modifier.matchParentSize()) {
                // Left Action (EDIT)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { 
                            scope.launch { offsetX.animateTo(0f) }
                            onEdit() 
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                // Right Action (DELETE)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { 
                            scope.launch { offsetX.animateTo(0f) }
                            onDelete() 
                        },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }

        // Foreground (Content)
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .then(
                    if (!isReorderMode) {
                        Modifier.draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                scope.launch {
                                    val newValue = (offsetX.value + delta).coerceIn(-actionWidthPx, actionWidthPx)
                                    offsetX.snapTo(newValue)
                                }
                            },
                            onDragStopped = {
                                val targetOffset = when {
                                    offsetX.value > actionWidthPx / 2 -> actionWidthPx
                                    offsetX.value < -actionWidthPx / 2 -> -actionWidthPx
                                    else -> 0f
                                }
                                scope.launch { offsetX.animateTo(targetOffset) }
                            }
                        )
                    } else Modifier
                )
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "$currency${String.format("%.2f", balance)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Reorder buttons - only visible in reorder mode
                if (isReorderMode) {
                    Row {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp, 
                                "Move Up",
                                tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown, 
                                "Move Down",
                                tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInputScreen(
    account: Account?,
    currentBalance: Double? = null,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Account, Double?) -> Unit
) {
    BackHandler(onBack = onDismiss)
    var name by remember { mutableStateOf(account?.name ?: "") }
    // If account exists (Edit), use currentBalance. If new, use initialBalance (empty).
    // but we only track 'displayBalance' for editing.
    // For saving:
    // New Account: initialBalance = displayBalance, adjustment = null
    // Edit Account: initialBalance = account.initialBalance (unchanged), adjustment = displayBalance - currentBalance
    
    var displayBalance by remember { 
        mutableStateOf(
            if (account != null) {
                val bal = currentBalance ?: account.initialBalance
                String.format("%.2f", bal).trimEnd('0').trimEnd('.')
            } else "" 
        ) 
    }
    
    // Fee Config State
    var feeConfigs by remember { mutableStateOf(account?.feeConfigs ?: emptyList()) }
    var showFeeInput by remember { mutableStateOf(false) }
    var newFeeName by remember { mutableStateOf("") }
    var newFeeValue by remember { mutableStateOf("") }
    var newFeeIsPercent by remember { mutableStateOf(true) }

    // Layout similar to AddExpenseForm
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
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (account == null) "Add Account" else "Edit Account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(32.dp))
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


                // Big Balance Input
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Text(
                        text = if (account == null) "Initial Balance" else "Current Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currency,
                            style = TextStyle(fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = displayBalance,
                            onValueChange = { newValue ->
                                // Allow only valid decimal input with max 2 decimal places
                                val filtered = newValue.filter { it.isDigit() || it == '.' || it == '-' }
                                val parts = filtered.removePrefix("-").split(".")
                                val prefix = if (filtered.startsWith("-")) "-" else ""
                                displayBalance = when {
                                    parts.size == 1 -> prefix + parts[0]
                                    parts.size == 2 -> prefix + "${parts[0]}.${parts[1].take(2)}"
                                    else -> displayBalance
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (displayBalance.isEmpty()) {
                                        Text(
                                            "0",
                                            style = TextStyle(
                                                fontSize = 56.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Start,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.widthIn(min = 200.dp)
                        )
                    }
                    if (account != null) {
                        Text(
                            text = "Editing this will create a balance adjustment transaction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Account Name",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()
                Text("Transaction Fees", style = MaterialTheme.typography.titleMedium)
                
                feeConfigs.forEachIndexed { index, config ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${config.name}: ${config.value}${if(config.isPercentage) "%" else ""}")
                        IconButton(onClick = { 
                            feeConfigs = feeConfigs.toMutableList().also { it.removeAt(index) } 
                        }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (showFeeInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         OutlinedTextField(
                            value = newFeeName,
                            onValueChange = { newFeeName = it },
                            label = { Text("Fee Name") },
                            modifier = Modifier.weight(1f)
                         )
                         Spacer(modifier = Modifier.width(8.dp))
                         OutlinedTextField(
                            value = newFeeValue,
                            onValueChange = { newFeeValue = it },
                            label = { Text("Value") },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                         )
                         Spacer(modifier = Modifier.width(8.dp))
                         // Toggle % / Fixed
                         Box(modifier = Modifier.clickable { newFeeIsPercent = !newFeeIsPercent }.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)).padding(8.dp)) {
                             Text(if(newFeeIsPercent) "%" else currency, color = MaterialTheme.colorScheme.onSecondaryContainer)
                         }
                         Spacer(modifier = Modifier.width(8.dp))
                         IconButton(onClick = {
                             if (newFeeName.isNotBlank() && newFeeValue.toDoubleOrNull() != null) {
                                 val newConfig = FeeConfig(newFeeName, newFeeValue.toDouble(), newFeeIsPercent)
                                 feeConfigs = feeConfigs + newConfig
                                 newFeeName = ""
                                 newFeeValue = ""
                                 showFeeInput = false
                             }
                         }) {
                             Icon(Icons.Default.Check, "Add")
                         }
                     }
                    }
                } else {
                    TextButton(onClick = { showFeeInput = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Fee Rule")
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
                    if (name.isNotBlank()) {
                        val bal = displayBalance.toDoubleOrNull() ?: 0.0
                        if (account == null) {
                            onSave(Account(id = UUID.randomUUID().toString(), name = name, initialBalance = bal, feeConfigs = feeConfigs), null)
                        } else {
                            val current = currentBalance ?: account.initialBalance
                            val roundedCurrent = (kotlin.math.round(current * 100) / 100.0)
                            val roundedNew = (kotlin.math.round(bal * 100) / 100.0)
                            
                            val adjustment = if (kotlin.math.abs(roundedNew - roundedCurrent) > 0.009) {
                                roundedNew - roundedCurrent
                            } else null
                            
                            onSave(account.copy(name = name, feeConfigs = feeConfigs), adjustment)
                        }
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Save Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

