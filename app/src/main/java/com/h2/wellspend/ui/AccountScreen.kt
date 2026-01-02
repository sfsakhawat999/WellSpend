package com.h2.wellspend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountBalanceWallet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    accounts: List<Account>,
    balances: Map<String, Double>,
    currency: String,
    onAddAccount: (Account) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    isAccountUsed: (String) -> Boolean,
    onReorder: (List<Account>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    
    // Drag state for reordering
    var draggedAccount by remember { mutableStateOf<Account?>(null) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var hoverIndex by remember { mutableIntStateOf(-1) }

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
            // Compute display order based on drag state
            val displayAccounts = remember(accounts, draggedIndex, hoverIndex) {
                if (draggedIndex != -1 && hoverIndex != -1 && draggedIndex != hoverIndex) {
                    val mutableList = accounts.toMutableList()
                    val item = mutableList.removeAt(draggedIndex)
                    mutableList.add(hoverIndex, item)
                    mutableList
                } else {
                    accounts
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayAccounts, key = { it.id }) { account ->
                    val isDragged = draggedAccount == account
                    AccountItem(
                        account = account,
                        balance = balances[account.id] ?: account.initialBalance,
                        currency = currency,
                        isDragged = isDragged,
                        onEdit = {
                            accountToEdit = account
                            showDialog = true
                        },
                        onDelete = { accountToDelete = account },
                        onDragStart = {
                            draggedAccount = account
                            draggedIndex = accounts.indexOf(account)
                            hoverIndex = draggedIndex
                        },
                        onDrag = { deltaY ->
                            // Calculate hover index based on drag delta
                            val itemHeightPx = 80f // Approximate item height in pixels
                            val indexDelta = (deltaY / itemHeightPx).toInt()
                            val newHoverIndex = (draggedIndex + indexDelta).coerceIn(0, accounts.size - 1)
                            if (newHoverIndex != hoverIndex) {
                                hoverIndex = newHoverIndex
                            }
                        },
                        onDragEnd = {
                            if (draggedIndex != -1 && hoverIndex != -1 && draggedIndex != hoverIndex) {
                                val mutableList = accounts.toMutableList()
                                val item = mutableList.removeAt(draggedIndex)
                                mutableList.add(hoverIndex, item)
                                onReorder(mutableList)
                            }
                            draggedAccount = null
                            draggedIndex = -1
                            hoverIndex = -1
                        }
                    )
                }
                item {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Long-press and drag to reorder • Swipe to edit/delete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
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
                onDismiss = { showDialog = false },
                onSave = { account ->
                    onAddAccount(account)
                    showDialog = false
                },
                isInitialBalanceEditable = accountToEdit?.let { !isAccountUsed(it.id) } ?: true
            )
        }
    }
}


@Composable
fun AccountItem(
    account: Account,
    balance: Double,
    currency: String,
    isDragged: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    // Track cumulative vertical drag for index calculation
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer {
                // Visual feedback when dragged
                alpha = if (isDragged) 0.8f else 1f
                scaleX = if (isDragged) 1.02f else 1f
                scaleY = if (isDragged) 1.02f else 1f
            }
    ) {
        // Background (Actions)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action (Visible when swiped right -> EDIT)
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
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Right Action (Visible when swiped left -> DELETE)
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
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }

        // Foreground (Content)
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 8.dp else 2.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            cumulativeDragY = 0f
                            onDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            cumulativeDragY += dragAmount.y
                            onDrag(cumulativeDragY)
                        },
                        onDragEnd = {
                            cumulativeDragY = 0f
                            onDragEnd()
                        },
                        onDragCancel = {
                            cumulativeDragY = 0f
                            onDragEnd()
                        }
                    )
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newValue = (offsetX.value + delta).coerceIn(-actionWidthPx, actionWidthPx)
                            offsetX.snapTo(newValue)
                        }
                    },
                    onDragStopped = {
                        val targetOffset = if (offsetX.value > actionWidthPx / 2) {
                            actionWidthPx // Snap Open (Right/Edit)
                        } else if (offsetX.value < -actionWidthPx / 2) {
                            -actionWidthPx // Snap Open (Left/Delete)
                        } else {
                            0f
                        }
                        scope.launch { offsetX.animateTo(targetOffset) }
                    }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            }
        }
    }
}

@Composable
fun AccountDialog(
    account: Account?,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit,
    isInitialBalanceEditable: Boolean
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var initialBalance by remember { mutableStateOf(account?.initialBalance?.toString() ?: "") }
    
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
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Initial Balance") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isInitialBalanceEditable,
                    supportingText = if (!isInitialBalanceEditable) { { Text("Cannot edit balance after transactions added") } } else null,
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
                            onSave(
                                Account(
                                    id = account?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    initialBalance = initialBalance.toDoubleOrNull() ?: 0.0,
                                    feeConfigs = feeConfigs
                                )
                            )
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
