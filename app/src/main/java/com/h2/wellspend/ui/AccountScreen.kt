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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.FeeConfig
import java.util.UUID
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
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
    onAddAccount: (Account) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,

    isAccountUsed: (String) -> Boolean,
    onReorder: (List<Account>) -> Unit,
    onAdjustBalance: (String, Double) -> Unit // NEW
) {
    var showDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    
    // Reorder mode state
    var isReorderMode by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                accountToEdit = null
                showDialog = true
            }) {
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
                                onEdit = {
                                    accountToEdit = account
                                    showDialog = true
                                },
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

        if (showDialog) {
            AccountDialog(
                account = accountToEdit,
                currentBalance = if (accountToEdit != null) balances[accountToEdit!!.id] else null,
                onDismiss = { showDialog = false },
                onSave = { account, adjustment ->
                    onUpdateAccount(account)
                    if (adjustment != null && adjustment != 0.0) {
                        onAdjustBalance(account.id, adjustment)
                    }
                    showDialog = false
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
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Action (EDIT)
                Box(
                    modifier = Modifier
                        .width(actionWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { 
                            scope.launch { offsetX.animateTo(0f) }
                            onEdit() 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onPrimary)
                }

                // Right Action (DELETE)
                Box(
                    modifier = Modifier
                        .width(actionWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { 
                            scope.launch { offsetX.animateTo(0f) }
                            onDelete() 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onError)
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

@Composable
fun AccountDialog(
    account: Account?,
    currentBalance: Double? = null,
    onDismiss: () -> Unit,
    onSave: (Account, Double?) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    // If account exists (Edit), use currentBalance. If new, use initialBalance (empty).
    // but we only track 'displayBalance' for editing.
    // For saving:
    // New Account: initialBalance = displayBalance, adjustment = null
    // Edit Account: initialBalance = account.initialBalance (unchanged), adjustment = displayBalance - currentBalance
    
    var displayBalance by remember { 
        mutableStateOf(
            if (account != null) (currentBalance?.toString() ?: account.initialBalance.toString()) 
            else "" 
        ) 
    }
    
    // Fee Config State
    var feeConfigs by remember { mutableStateOf(account?.feeConfigs ?: emptyList()) }
    var showFeeInput by remember { mutableStateOf(false) }
    var newFeeName by remember { mutableStateOf("") }
    var newFeeValue by remember { mutableStateOf("") }
    var newFeeIsPercent by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (account == null) "Add Account" else "Edit Account",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayBalance,
                    onValueChange = { displayBalance = it },
                    label = { Text(if (account == null) "Initial Balance" else "Current Balance") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    supportingText = if (account != null) { { Text("Editing this will create a balance adjustment transaction") } } else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                Divider()
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
                        OutlinedTextField(value = newFeeName, onValueChange = { newFeeName = it }, label = { Text("Fee Name") }, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newFeeValue, 
                                onValueChange = { newFeeValue = it }, 
                                label = { Text("Value") }, 
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = newFeeIsPercent,
                                onClick = { newFeeIsPercent = !newFeeIsPercent },
                                label = { Text(if(newFeeIsPercent) "%" else "Fixed") }
                            )
                        }
                        Button(onClick = {
                            if (newFeeName.isNotBlank() && newFeeValue.toDoubleOrNull() != null) {
                                feeConfigs = feeConfigs + FeeConfig(newFeeName, newFeeValue.toDouble(), newFeeIsPercent)
                                newFeeName = ""
                                newFeeValue = ""
                                showFeeInput = false
                            }
                        }) {
                            Text("Add Fee Rule")
                        }
                    }
                } else {
                    OutlinedButton(onClick = { showFeeInput = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add Fee Configuration")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            val newBalanceVal = displayBalance.toDoubleOrNull() ?: 0.0
                            
                            if (account == null) {
                                // NEW Account
                                onSave(
                                    Account(
                                        id = UUID.randomUUID().toString(),
                                        name = name,
                                        initialBalance = newBalanceVal,
                                        feeConfigs = feeConfigs
                                    ),
                                    null // No adjustment transaction for new account
                                )
                            } else {
                                // EDIT Account
                                val oldBalance = currentBalance ?: account.initialBalance
                                val adjustment = newBalanceVal - oldBalance
                                
                                onSave(
                                    account.copy(
                                        name = name,
                                        feeConfigs = feeConfigs
                                        // initialBalance remains unchanged!
                                    ),
                                    adjustment
                                )
                            }
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
