package com.h2.wellspend.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.Typeface

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin

data class ChartData(
    val name: String,
    val value: Double,
    val color: Color
)

data class LabelInfo(
    val name: String,
    val percentage: Int,
    val rawPercentage: Double,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float,
    val midAngle: Double,
    val isRightSide: Boolean
)

@Composable
fun DonutChart(
    data: List<ChartData>,
    totalAmount: Double,
    currency: String,
    centerLabel: String = "Total Spend",
    additionalLabel: String? = null
) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            // Increased chart size
            val chartSize = min(size.width, size.height) * 0.7f
            val radius = (chartSize - strokeWidth) / 2
            val lineStartRadius = radius + strokeWidth / 2 + 2.dp.toPx()
            val labelSpacing = 24.dp.toPx()

            if (data.isEmpty()) {
                drawCircle(
                    color = emptyColor,
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                val total = data.sumOf { it.value }

                // Winged Zipper Sorting: Winged Triangle
                // 1. Center: Rank 1
                // 2. Split remaining into Clockwise (C) and Anti-Clockwise (A) pools
                // 3. Sort pools customized: Smallest, Largest, 2nd Smallest...
                val sortedData = data.sortedByDescending { it.value }
                val displayData = mutableListOf<ChartData>()
                
                if (sortedData.isNotEmpty()) {
                    // Center Piece (Largest)
                    displayData.add(sortedData[0])
                    
                    val remaining = sortedData.drop(1)
                    val clockwisePool = mutableListOf<ChartData>()
                    val antiClockwisePool = mutableListOf<ChartData>()
                    
                    // Distribute to wings
                    remaining.forEachIndexed { index, item ->
                        if (index % 2 == 0) clockwisePool.add(item) 
                        else antiClockwisePool.add(item)
                    }
                    
                    // Triangle Sorting: Small items at Top (near Center), Large at Bottom
                    // Right Wing (Clockwise): Small -> Large
                    // Left Wing (Anti-Clockwise): Large -> Small (visual: Bottom -> Top is Large -> Small)
                    
                    displayData.addAll(clockwisePool.sortedBy { it.value }) // Ascending
                    displayData.addAll(antiClockwisePool.sortedByDescending { it.value }) // Descending
                }

                // Minimum angle enforcement
                val minAngle = 2f
                val rawAngles = displayData.map { it to (it.value / total * 360f).toFloat() }
                
                val smallSlices = rawAngles.filter { it.second < minAngle }
                val largeSlices = rawAngles.filter { it.second >= minAngle }
                
                val totalDeficit = smallSlices.sumOf { (minAngle - it.second).toDouble() }.toFloat()
                val totalLargeAngles = largeSlices.sumOf { it.second.toDouble() }.toFloat()
                
                val scaleFactor = if (totalLargeAngles > 0) (totalLargeAngles - totalDeficit) / totalLargeAngles else 1f
                
                val adjustedAngles = rawAngles.associate { (item, rawAngle) ->
                    val finalAngle = if (rawAngle < minAngle) minAngle else rawAngle * scaleFactor
                    item to finalAngle
                }

                // Center the first item at 12 o'clock (-90 degrees)
                val firstItem = displayData.firstOrNull()
                val firstItemAngle = firstItem?.let { adjustedAngles[it] } ?: 0f
                var startAngle = -90f - (firstItemAngle / 2)

                // First pass: draw arcs and collect info
                val labelInfoList = mutableListOf<LabelInfo>()
                
                displayData.forEachIndexed { index, item ->
                    val finalSweepAngle = adjustedAngles[item] ?: 0f
                    val sweepAngle = finalSweepAngle // No animation
                    val gap = if (data.size > 1) 0.5f else 0f
                    
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - gap,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    
                    val midAngle = (startAngle + finalSweepAngle / 2).toDouble()
                    val midAngleRad = Math.toRadians(midAngle)
                    val rawPercentage = item.value / total * 100
                    val percentage = rawPercentage.toInt()
                    
                    // Initial side assignment
                    // Top item (Index 0) goes to the side with fewer items
                    val isTopItem = index == 0
                    val isRightSide = if (isTopItem) {
                        val remainingCount = data.size - 1
                        val rightCount = (remainingCount + 1) / 2
                        val leftCount = remainingCount / 2
                        rightCount <= leftCount
                    } else {
                        cos(midAngleRad) >= 0
                    }
                    
                    labelInfoList.add(LabelInfo(
                        name = item.name,
                        percentage = percentage,
                        rawPercentage = rawPercentage,
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = finalSweepAngle,
                        midAngle = midAngle,
                        isRightSide = isRightSide
                    ))
                    
                    // Draw percentage
                    if (finalSweepAngle >= 15) {
                        val percentX = center.x + radius * cos(midAngleRad).toFloat()
                        val percentY = center.y + radius * sin(midAngleRad).toFloat()
                        
                        val percentPaint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 9.sp.toPx()
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                            isAntiAlias = true
                        }
                        if (percentage >= 1) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "$percentage%",
                                percentX,
                                percentY + 4.dp.toPx(),
                                percentPaint
                            )
                        }
                    }
                    
                    startAngle += sweepAngle
                }

                // Draw configured labels and lines
                run {
                    // Native uses geometric sort (Y), Shifted uses Stack (Y Descending)
                    fun getSortY(info: LabelInfo): Float {
                        return (center.y + lineStartRadius * sin(Math.toRadians(info.midAngle)).toFloat())
                    }
                    
                    // Strict geometric side assignment (no balancing)
                    val rightLabels = labelInfoList.filter { it.isRightSide }
                        .sortedBy { getSortY(it) }
                    
                    val leftLabels = labelInfoList.filter { !it.isRightSide }
                        .sortedBy { getSortY(it) }
                    
                    val rightLabelX = center.x + radius + strokeWidth / 2 + 40.dp.toPx()
                    val leftLabelX = center.x - radius - strokeWidth / 2 - 40.dp.toPx()
                    
                    // Percentage-based vertical alignment rules
                    val topItemValue = displayData.firstOrNull()?.value ?: 0.0
                    val topItemPercentage = if (total > 0) (topItemValue / total * 100) else 0.0

                    fun getStartY(count: Int): Float {
                        val totalHeight = if (count > 0) (count - 1) * labelSpacing else 0f
                        
                        // Rule 1: < 40% -> Center
                        // Rule 2: 40% - 60% -> Bottom of Chart
                        // Rule 3: > 60% -> Bottom of Section (View)
                        
                        return if (topItemPercentage >= 60) {
                             // Bottom of Section
                            val viewBottom = size.height - 16.dp.toPx()
                            viewBottom - totalHeight
                        } else if (topItemPercentage >= 40) {
                            // Bottom of Chart
                            val chartVisualBottom = center.y + radius + strokeWidth / 2
                            chartVisualBottom - totalHeight
                        } else {
                            // Center (< 50%)
                            center.y - totalHeight / 2
                        }
                    }

                    val rightStartY = getStartY(rightLabels.size)
                    val leftStartY = getStartY(leftLabels.size)
                    
                    fun drawConnector(
                        info: LabelInfo, 
                        targetX: Float, 
                        targetY: Float, 
                        isRight: Boolean
                    ) {
                        // Smart Anchor Calculation
                        val rangePad = min(2f, info.sweepAngle / 2f)
                        val validStart = info.startAngle + rangePad
                        // info.sweepAngle is already the adjusted visible angle
                        val validEnd = info.startAngle + info.sweepAngle - rangePad
                        
                        // Check multiple candidate points along the arc to find the one closest to target
                        fun distToTarget(angle: Float): Double {
                            val rad = Math.toRadians(angle.toDouble())
                            val px = center.x + lineStartRadius * cos(rad).toFloat()
                            val py = center.y + lineStartRadius * sin(rad).toFloat()
                            val dx = targetX - px
                            val dy = targetY - py
                            return dx*dx + dy*dy.toDouble()
                        }
                        
                        val candidates = mutableListOf(validStart, validEnd, info.midAngle.toFloat())
                        for (i in 1..4) {
                            candidates.add(validStart + (info.sweepAngle - 2*rangePad) * (i/5f))
                        }
                        
                        val bestAnchorAngle = candidates.minByOrNull { distToTarget(it) } ?: info.midAngle.toFloat()
                        
                        val bestRad = Math.toRadians(bestAnchorAngle.toDouble())
                        val startX = center.x + lineStartRadius * cos(bestRad).toFloat()
                        val startY = center.y + lineStartRadius * sin(bestRad).toFloat()
                        
                        val path = Path()
                        path.moveTo(startX, startY)

                        // Standard Radial/Smart Routing for all items
                        val controlPointOffset = 30.dp.toPx()
                        val c1Radius = 40.dp.toPx() 
                        val c1X = startX + c1Radius * cos(bestRad).toFloat()
                        val c1Y = startY + c1Radius * sin(bestRad).toFloat()
                        
                        path.cubicTo(
                            c1X, c1Y,
                            if (isRight) targetX - controlPointOffset else targetX + controlPointOffset, targetY,
                            if (isRight) targetX - 6.dp.toPx() else targetX + 6.dp.toPx(), targetY
                        )
                        
                        drawPath(
                            path = path,
                            color = info.color,
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val labelPaint = Paint().apply {
                            color = onSurfaceColor.hashCode()
                            textSize = 7.sp.toPx()
                            textAlign = if (isRight) Paint.Align.LEFT else Paint.Align.RIGHT
                            typeface = Typeface.DEFAULT
                            isAntiAlias = true
                        }
                        val percentString = if (info.rawPercentage < 1.0) {
                             String.format("%.1f%%", info.rawPercentage)
                        } else {
                             String.format("%.0f%%", info.rawPercentage)
                        }

                        val labelText = if (isRight) {
                             "$percentString - ${info.name}"
                        } else {
                             "${info.name} - $percentString"
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            targetX,
                            targetY + 4.dp.toPx(),
                            labelPaint
                        )
                    }

                    rightLabels.forEachIndexed { index, info ->
                        val targetY = rightStartY + index * labelSpacing
                        drawConnector(info, rightLabelX, targetY, true)
                    }

                    leftLabels.forEachIndexed { index, info ->
                        val targetY = leftStartY + index * labelSpacing
                        drawConnector(info, leftLabelX, targetY, false)
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
        ) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$currency${String.format("%.2f", totalAmount)}",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (additionalLabel != null) {
                Text(
                    text = additionalLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
