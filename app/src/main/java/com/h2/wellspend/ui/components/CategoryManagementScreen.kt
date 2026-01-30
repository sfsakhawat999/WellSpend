package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import com.h2.wellspend.data.Category
import androidx.core.graphics.ColorUtils
import com.h2.wellspend.ui.getIconByName
import com.h2.wellspend.ui.AllIcons
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch
import com.h2.wellspend.ui.performWiggle
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Shape
import com.h2.wellspend.ui.getGroupedItemShape
import com.h2.wellspend.ui.getGroupedItemBackgroundShape
import com.h2.wellspend.ui.theme.cardBackgroundColor
import com.h2.wellspend.ui.components.CategoryDialog

private val SYSTEM_CATEGORY_NAMES = setOf("Others", "Loan", "TransactionFee", "BalanceAdjustment")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    categories: List<Category>,
    onAddCategory: (Category) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    usedCategoryNames: Set<String>,
    showAddDialog: Boolean,
    onDismissAddDialog: () -> Unit
) {
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(categories) { index, category ->
                val shape = getGroupedItemShape(index, categories.size)
                val backgroundShape = getGroupedItemBackgroundShape(index, categories.size)
                
                Box(modifier = Modifier.padding(vertical = 1.dp)) {
                    CategoryItem(
                        category = category,
                        onEdit = { categoryToEdit = category },
                        onDelete = { categoryToDelete = category },
                        shape = shape,
                        backgroundShape = backgroundShape
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
                        text = "Swipe left/right to edit or delete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

    if (showAddDialog) {
        CategoryDialog(
            initialCategory = null,
            usedCategoryNames = usedCategoryNames,
            onDismiss = onDismissAddDialog,
            onConfirm = { cat ->
                onAddCategory(cat)
                onDismissAddDialog()
            }
        )
    }
    
    if (categoryToEdit != null) {
        CategoryDialog(
            initialCategory = categoryToEdit,
            usedCategoryNames = usedCategoryNames,
            onDismiss = { categoryToEdit = null },
            onConfirm = { cat ->
                onUpdateCategory(cat)
                categoryToEdit = null
            }
        )
    }
    
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { 
                Text("Are you sure you want to delete '${categoryToDelete?.name}'? This may affect existing transactions.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { onDeleteCategory(it) }
                        categoryToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundShape: Shape = shape
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    val isSystemCategory = category.isSystem || SYSTEM_CATEGORY_NAMES.contains(category.name)
    val maxSwipeLeft = if (isSystemCategory) 0f else -actionWidthPx

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
    ) {
        // Background Actions
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(cardBackgroundColor())
                .clip(backgroundShape)
        ) {
            // Left Action (Edit) - Revealed by swiping RIGHT
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
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Right Action (Delete) - Revealed by swiping LEFT - Only if NOT system
            if (!isSystemCategory) {
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
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        // Foreground (Content)
        // Replaced Card with Box/Row
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newValue = (offsetX.value + delta).coerceIn(maxSwipeLeft, actionWidthPx)
                            offsetX.snapTo(newValue)
                        }
                    },
                    onDragStopped = {
                        val targetOffset = when {
                            offsetX.value > actionWidthPx / 2 -> actionWidthPx // Snap Open (Right/Edit)
                            offsetX.value < -actionWidthPx / 2 && !isSystemCategory -> -actionWidthPx // Snap Open (Left/Delete)
                            else -> 0f
                        }
                        scope.launch { offsetX.animateTo(targetOffset) }
                    }
                )
                .fillMaxWidth()
                .background(cardBackgroundColor(), shape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // Disable ripple to avoid interference
                    onClick = { 
                        if (offsetX.value != 0f) {
                            scope.launch { offsetX.animateTo(0f) }
                        } else {
                            scope.launch {
                                performWiggle(offsetX, actionWidthPx, context)
                            }
                        }
                    },
                    onLongClick = {
                        scope.launch {
                            performWiggle(offsetX, actionWidthPx, context)
                        }
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(category.color)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(category.iconName),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (category.isSystem || SYSTEM_CATEGORY_NAMES.contains(category.name)) {
                           Spacer(modifier = Modifier.height(6.dp))
                           Text(
                                text = "System Default",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                           ) 
                        }
                    }
                }
                
                // Removed inline buttons
            }
        }
    }
}


