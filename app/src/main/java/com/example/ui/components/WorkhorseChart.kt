package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.data.WorkDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WorkhorseChart(
    workDays: List<WorkDay>,
    modifier: Modifier = Modifier
) {
    // Filter out nonwork days and sort chronologically
    val activeDays = workDays
        .filter { !it.checkIfNonWorkDay() && (it.actualStartMillis != null || it.actualEndMillis != null || it.totalBreakMinutes > 0) }
        .sortedBy { it.date }

    if (activeDays.isEmpty()) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No work data recorded yet to show chart.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    // Calculate cumulative debt sum over time
    var cumulativeSum = 0
    val chartData = activeDays.map { day ->
        cumulativeSum += day.getTotalDebt()
        Pair(day.date, cumulativeSum)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val zeroLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BALANCE HISTORY (ALL DAYS)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val isOverworked = (chartData.lastOrNull()?.second ?: 0) <= 0
            Text(
                text = if (isOverworked) "TREND: CREDIT" else "TREND: OWING",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isOverworked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val paddingLeft = 60f
                val paddingBottom = 40f
                val paddingTop = 20f
                val paddingRight = 20f

                val drawableWidth = width - paddingLeft - paddingRight
                val drawableHeight = height - paddingTop - paddingBottom

                val yValues = chartData.map { it.second }
                val minY = yValues.minOrNull() ?: 0
                val maxY = yValues.maxOrNull() ?: 0

                // Add some margin to Y bounds to look nicer
                val yRange = (maxY - minY).coerceAtLeast(1)
                val chartMinY = minY - (yRange * 0.1f).toInt()
                val chartMaxY = maxY + (yRange * 0.1f).toInt()
                val finalYRange = (chartMaxY - chartMinY).coerceAtLeast(1)

                // --- Draw Grid & Y Labels ---
                val gridLines = 4
                for (i in 0..gridLines) {
                    val ratio = i.toFloat() / gridLines
                    val y = paddingTop + drawableHeight * (1f - ratio)

                    // Horizontal line
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f
                    )

                    // Y Label
                    val labelValue = chartMinY + (ratio * finalYRange).toInt()
                    val hrs = labelValue.toFloat() / 60f
                    val formattedHr = if (hrs % 1f == 0f) "${hrs.toInt()}h" else String.format(java.util.Locale.US, "%.1fh", hrs)
                    // Draw text is not directly supported in Compose Canvas easily without native canvas,
                    // but we can draw a small line or use simple labels.
                    // Wait! To be extremely precise, we can use native canvas drawText to draw beautiful, accurate labels.
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        drawText(
                            formattedHr,
                            paddingLeft - 10f,
                            y + 8f,
                            paint
                        )
                    }
                }

                // --- Plot Data Line & Points ---
                val pointsCount = chartData.size
                val xStep = if (pointsCount > 1) drawableWidth / (pointsCount - 1) else drawableWidth

                // Draw a bold 0-line if 0 is within the chart bounds
                if (0 in chartMinY..chartMaxY) {
                    val yZero = paddingTop + drawableHeight * (1f - (0 - chartMinY).toFloat() / finalYRange)
                    drawLine(
                        color = zeroLineColor,
                        start = Offset(paddingLeft, yZero),
                        end = Offset(width - paddingRight, yZero),
                        strokeWidth = 3f
                    )
                }

                val coordinates = chartData.mapIndexed { index, pair ->
                    val x = paddingLeft + index * xStep
                    val y = paddingTop + drawableHeight * (1f - (pair.second - chartMinY).toFloat() / finalYRange)
                    Offset(x, y)
                }

                // Draw background gradient under the curve
                if (coordinates.size > 1) {
                    val path = Path().apply {
                        moveTo(coordinates.first().x, paddingTop + drawableHeight)
                        for (coord in coordinates) {
                            lineTo(coord.x, coord.y)
                        }
                        lineTo(coordinates.last().x, paddingTop + drawableHeight)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                            startY = paddingTop,
                            endY = paddingTop + drawableHeight
                        )
                    )
                }

                // Draw the line
                if (coordinates.size > 1) {
                    val linePath = Path().apply {
                        moveTo(coordinates.first().x, coordinates.first().y)
                        for (i in 1 until coordinates.size) {
                            lineTo(coordinates[i].x, coordinates[i].y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw dots on data points
                for (coord in coordinates) {
                    drawCircle(
                        color = primaryColor,
                        radius = 6f,
                        center = coord
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 2.5f,
                        center = coord
                    )
                }

                // --- Draw X Labels (Dates) ---
                // Render up to 5 date labels to avoid overlap
                val labelStep = (pointsCount / 4).coerceAtLeast(1)
                chartData.forEachIndexed { index, pair ->
                    if (index % labelStep == 0 || index == pointsCount - 1) {
                        val x = paddingLeft + index * xStep
                        val dateLabel = try {
                            val parsedDate = LocalDate.parse(pair.first)
                            parsedDate.format(DateTimeFormatter.ofPattern("yyyy"))
                        } catch (e: Exception) {
                            if (pair.first.length >= 4) pair.first.substring(0, 4) else pair.first
                        }

                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = labelColor.toArgb()
                                textSize = 22f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(
                                dateLabel,
                                x,
                                height - 5f,
                                paint
                            )
                        }
                    }
                }
            }
        }
    }
}
