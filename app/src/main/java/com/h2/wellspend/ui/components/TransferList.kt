package com.h2.wellspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.h2.wellspend.data.Account
import com.h2.wellspend.data.Expense
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import androidx.compose.animation.core.Animatable
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun TransferList(
    transfers: List<Expense>,
    accounts: List<Account>,
    currency: String,
    onDelete: (String) -> Unit,
    onEdit: (Expense) -> Unit
) {
    if (transfers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transfers recorded for this period.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(transfers) { transfer ->
                TransferItem(
                    transfer = transfer,
                    fromAccountName = accounts.find { it.id == transfer.accountId }?.name ?: "Deleted Account",
                    toAccountName = accounts.find { it.id == transfer.transferTargetAccountId }?.name ?: "Deleted Account",
                    currency = currency,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
            
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Swipe left/right to edit or delete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransferItem(
    transfer: Expense,
    fromAccountName: String,
    toAccountName: String,
    currency: String,
    onEdit: (Expense) -> Unit,
    onDelete: (String) -> Unit
) {
    val date = try {
        LocalDate.parse(transfer.date.take(10))
    } catch (e: Exception) {
        LocalDate.now()
    }
    val formattedDate = date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Delete states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    var deleteConfirmed by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transfer") },
            text = { Text("Are you sure you want to delete this transfer?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmed = true
                        showDeleteDialog = false
                        isVisible = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(isVisible) {
        if (!isVisible && deleteConfirmed) {
            delay(300) 
            onDelete(transfer.id)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically() + fadeOut(),
        enter = expandVertically() + fadeIn()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(16.dp)) 
        ) {
            // Background (Actions)
            Box(modifier = Modifier.matchParentSize()) {
                // Left Action (Edit)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onEdit(transfer) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                // Right Action (Delete)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(actionWidth + 24.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(modifier = Modifier.width(actionWidth), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onError)
                    }
                }
            }

            // Foreground (Content)
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                val newValue = (offsetX.value + delta).coerceIn(-actionWidthPx, actionWidthPx)
                                offsetX.snapTo(newValue)
                            }
                        },
                        onDragStopped = {
                             val targetOffset = if (offsetX.value > actionWidthPx / 2) actionWidthPx else if (offsetX.value < -actionWidthPx / 2) -actionWidthPx else 0f
                             scope.launch { offsetX.animateTo(targetOffset) }
                        }
                    )
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            scope.launch {
                                com.h2.wellspend.ui.performWiggle(offsetX, actionWidthPx, context)
                            }
                        },
                        onLongClick = {
                            scope.launch {
                                com.h2.wellspend.ui.performWiggle(offsetX, actionWidthPx, context)
                            }
                        }
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transfer.description.ifEmpty { "Transfer" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$currency${String.format("%.2f", transfer.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fromAccountName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "To",
                            modifier = Modifier.padding(horizontal = 8.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        
                        Text(
                            text = toAccountName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
