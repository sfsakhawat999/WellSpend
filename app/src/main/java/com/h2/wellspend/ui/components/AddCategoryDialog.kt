package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.h2.wellspend.data.Category
import com.h2.wellspend.ui.AllIcons
import com.h2.wellspend.ui.getIconByName

@Composable
fun CategoryDialog(
    initialCategory: Category?,
    usedCategoryNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var color by remember { mutableStateOf(initialCategory?.let { Color(it.color) } ?: Color.Gray) }
    var iconName by remember { mutableStateOf(initialCategory?.iconName ?: "Label") }
    
    // Icon Picker State
    var showIconPicker by remember { mutableStateOf(false) }
    // Color Picker State
    var showColorPicker by remember { mutableStateOf(false) }
    
    val colors = listOf(
        Color(0xFFef4444), Color(0xFFf97316), Color(0xFFf59e0b), Color(0xFFeab308),
        Color(0xFF84cc16), Color(0xFF22c55e), Color(0xFF10b981), Color(0xFF14b8a6),
        Color(0xFF06b6d4), Color(0xFF0ea5e9), Color(0xFF3b82f6), Color(0xFF6366f1),
        Color(0xFF8b5cf6), Color(0xFFa855f7), Color(0xFFd946ef), Color(0xFFec4899),
        Color(0xFFf43f5e), Color(0xFF64748b), Color(0xFF71717a), Color(0xFF000000)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCategory == null) "Add Category" else "Edit Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    enabled = initialCategory == null
                )
                
                // Color Picker Trigger
                Text("Color", style = MaterialTheme.typography.labelLarge)
                
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colors) { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    if (color == c) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .clickable { color = c }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showColorPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (color.luminance() > 0.5) Color.Black else Color.White,
                        containerColor = color
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Select Custom Color", fontWeight = FontWeight.SemiBold)
                }
                
                // Icon Picker Trigger
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    onClick = { showIconPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(getIconByName(iconName), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(iconName)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(
                        Category(
                            name = name,
                            iconName = iconName,
                            color = color.toArgb().toLong(),
                            isSystem = initialCategory?.isSystem ?: false
                        )
                    ) 
                },
                enabled = name.isNotBlank() && (initialCategory?.name == name || !usedCategoryNames.contains(name))
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
    
    if (showIconPicker) {
        IconPickerDialog(
            onDismiss = { showIconPicker = false },
            onIconSelected = { 
                iconName = it
                showIconPicker = false 
            }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = color,
            onDismiss = { showColorPicker = false },
            onColorSelected = { 
                color = it
                showColorPicker = false 
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    // Convert initial color to HSL
    val initialHsl = FloatArray(3)
    ColorUtils.colorToHSL(initialColor.toArgb(), initialHsl)
    
    var hue by remember { mutableStateOf(initialHsl[0]) }
    var saturation by remember { mutableStateOf(initialHsl[1]) }
    var lightness by remember { mutableStateOf(initialHsl[2].coerceIn(0.4f, 0.8f)) }
    
    val selectedColor = remember(hue, saturation, lightness) {
        Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness)))
    }
    
    var hexString by remember { mutableStateOf(String.format("%06X", (initialColor.toArgb() and 0xFFFFFF))) }
    
    // Update hex when sliders change
    LaunchedEffect(selectedColor) {
        hexString = String.format("%06X", (selectedColor.toArgb() and 0xFFFFFF))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Color Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(selectedColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "#$hexString", 
                        color = if (selectedColor.luminance() > 0.5) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Hue Slider with gradient
                Column {
                    Text("Hue", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = selectedColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        track = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.hsl(0f, 1f, 0.5f),
                                                Color.hsl(60f, 1f, 0.5f),
                                                Color.hsl(120f, 1f, 0.5f),
                                                Color.hsl(180f, 1f, 0.5f),
                                                Color.hsl(240f, 1f, 0.5f),
                                                Color.hsl(300f, 1f, 0.5f),
                                                Color.hsl(360f, 1f, 0.5f)
                                            )
                                        )
                                    )
                            )
                        }
                    )
                }
                
                // Saturation Slider
                Column {
                    Text("Saturation", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = selectedColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        track = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.hsl(hue, 0f, lightness),
                                                Color.hsl(hue, 1f, lightness)
                                            )
                                        )
                                    )
                            )
                        }
                    )
                }
                
                // Lightness Slider
                Column {
                    Text("Lightness", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = lightness,
                        onValueChange = { lightness = it },
                        valueRange = 0.4f..0.8f, // Limit lightness to 40%-80% to ensure visibility
                        colors = SliderDefaults.colors(
                            thumbColor = selectedColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        track = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.hsl(hue, saturation, 0f),
                                                Color.hsl(hue, saturation, 0.5f),
                                                Color.hsl(hue, saturation, 1f)
                                            )
                                        )
                                    )
                            )
                        }
                    )
                }

                // Hex Input
                OutlinedTextField(
                    value = hexString,
                    onValueChange = { input ->
                        val filtered = input.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                        hexString = filtered
                        if (filtered.length == 6) {
                            try {
                                val colorInt = android.graphics.Color.parseColor("#$filtered")
                                val hsl = FloatArray(3)
                                ColorUtils.colorToHSL(colorInt, hsl)
                                hue = hsl[0]
                                saturation = hsl[1]
                                lightness = hsl[2].coerceIn(0.4f, 0.8f)
                            } catch (e: Exception) {}
                        }
                    },
                    label = { Text("HEX Code") },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(selectedColor) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun IconPickerDialog(
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    // Flatten the AllIcons map to a list for grid
    val iconList = remember { AllIcons.toList() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Icon") },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp)
            ) {
                // simple grid logic
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                       iconList.forEach { (name, vector) ->
                           IconButton(onClick = { onIconSelected(name) }) {
                               Icon(vector, contentDescription = name)
                           }
                       }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Helper for FlowRow if not available in Material3 yet (it is experimental usually)
// Using a simplified valid imports if needed, but M3 might have it or use ContextualFlowRow
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxItemsInEachRow = maxItemsInEachRow,
        content = { content() }
    )
}
