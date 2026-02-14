package com.h2.wellspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.h2.wellspend.data.Account

@Composable
fun FeeSelector(
    account: Account?,
    transactionAmount: Double,
    currency: String,
    selectedConfigName: String?,
    currentFeeAmount: String,
    isCustomFee: Boolean,
    onFeeChanged: (String?, String, Boolean) -> Unit, // configName, amount, isCustom
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isCustomFee) {
        if (isCustomFee) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(transactionAmount, selectedConfigName, account) {
        if (!isCustomFee && selectedConfigName != null && selectedConfigName != "None") {
            val config = account?.feeConfigs?.find { it.name == selectedConfigName }
            if (config != null && config.isPercentage) {
                val updatedFee = transactionAmount * config.value / 100
                onFeeChanged(selectedConfigName, String.format("%.2f", updatedFee), false)
            }
        }
    }


    Column(modifier = modifier) {
        Text(
            text = "Transaction Fees",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // None Option
            FeeOptionItem(
                label = "None",
                subLabel = "${currency}0.00",
                icon = Icons.Default.Block,
                isSelected = selectedConfigName == "None",
                onClick = { onFeeChanged("None", "0.0", false) }
            )

            // Preset Options
            account?.feeConfigs?.forEach { config ->
                val isSelected = selectedConfigName == config.name
                val icon = if (config.isPercentage) Icons.Default.Percent else Icons.Default.AttachMoney
                
                FeeOptionItem(
                    label = config.name,
                    subLabel = if (config.isPercentage) "${config.value}%" else "$currency${config.value}",
                    icon = icon,
                    isSelected = isSelected,
                    onClick = {
                        val calculatedFee = if (config.isPercentage) {
                            (transactionAmount * config.value / 100)
                        } else {
                            config.value
                        }
                        onFeeChanged(config.name, String.format("%.2f", calculatedFee), false)
                    }
                )
            }

            // Custom Option
            val customSubLabel = if (isCustomFee && (currentFeeAmount.toDoubleOrNull() ?: 0.0) > 0) {
                "$currency$currentFeeAmount"
            } else {
                "${currency}xx"
            }
            FeeOptionItem(
                label = "Custom",
                subLabel = customSubLabel,
                icon = Icons.Default.Tune,
                isSelected = isCustomFee,
                onClick = {
                    if (!isCustomFee) {
                        onFeeChanged("Custom", "", true)
                    } else {
                        focusRequester.requestFocus()
                    }
                }
            )
        }
        
        if (isCustomFee || (currentFeeAmount.toDoubleOrNull() ?: 0.0) > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = currentFeeAmount,
                onValueChange = { 
                    // When text changes, we switch to Custom mode implicitly
                    onFeeChanged("Custom", it, true) 
                },
                label = { Text("Fee Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                prefix = { Text(currency) }
            )
        }
    }
}

@Composable
fun FeeOptionItem(
    label: String,
    subLabel: String? = null,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .background(containerColor, CircleShape)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
                
                Text(
                    text = subLabel ?: " ", // Space ensures it takes up line height
                    style = MaterialTheme.typography.labelSmall,
                    color = if (subLabel != null) contentColor.copy(alpha = 0.8f) else Color.Transparent
                )
            }
        }
    }
}
