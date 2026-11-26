package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PowerCurvePoint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalVitroColors
import kotlin.math.max

@Composable
fun PowerCurveChart(
    points: List<PowerCurvePoint>,
    targetHeatWatts: Int,
    localWindMs: Double,
    cutInMs: Double,
    modifier: Modifier = Modifier
) {
    val vitroColors = LocalVitroColors.current
    val windColor = vitroColors.windAccent
    val thermalColor = vitroColors.thermalAccent
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Крива потужності вітряка (Вт від швидкості вітру)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(color = windColor, label = "Генерація (Вт)")
                LegendChip(color = thermalColor, label = "Потреба кімнати ($targetHeatWatts Вт)")
            }
            Spacer(modifier = Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                if (points.isEmpty()) return@Canvas

                val leftPad = 48.dp.toPx()
                val rightPad = 14.dp.toPx()
                val topPad = 12.dp.toPx()
                val bottomPad = 28.dp.toPx()

                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - topPad - bottomPad

                val maxCurvePower = points.maxOfOrNull { it.powerWatts } ?: 1000.0
                val maxPower = max(maxCurvePower, targetHeatWatts * 1.25).coerceAtLeast(500.0)
                val maxWind = 14.0

                val labelStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = labelColor
                )

                // Horizontal grid lines (4 levels)
                for (i in 0..4) {
                    val ratio = i / 4f
                    val y = topPad + chartH * (1f - ratio)
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPad, y),
                        end = Offset(leftPad + chartW, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    val wVal = (maxPower * ratio).toInt()
                    val measured = textMeasurer.measure("${wVal}W", labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                            y = y - measured.size.height / 2f
                        )
                    )
                }

                // X-axis wind speed labels (0, 3, 6, 9, 12, 14 m/s)
                listOf(0, 3, 6, 9, 12, 14).forEach { w ->
                    val x = leftPad + (w / maxWind).toFloat() * chartW
                    drawLine(
                        color = gridColor.copy(alpha = 0.5f),
                        start = Offset(x, topPad),
                        end = Offset(x, topPad + chartH),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    val measured = textMeasurer.measure("${w}м/с", labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = (x - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                            y = topPad + chartH + 6.dp.toPx()
                        )
                    )
                }

                // Build curve path and gradient fill
                val linePath = Path()
                val fillPath = Path()

                points.forEachIndexed { index, pt ->
                    val x = leftPad + ((pt.windSpeedMs / maxWind).toFloat().coerceIn(0f, 1f)) * chartW
                    val y = topPad + chartH * (1f - (pt.powerWatts / maxPower).toFloat().coerceIn(0f, 1f))
                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, topPad + chartH)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    if (index == points.lastIndex) {
                        fillPath.lineTo(x, topPad + chartH)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(windColor.copy(alpha = 0.28f), windColor.copy(alpha = 0.02f)),
                        startY = topPad,
                        endY = topPad + chartH
                    )
                )

                drawPath(
                    path = linePath,
                    color = windColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Target heat threshold dashed line
                val heatY = topPad + chartH * (1f - (targetHeatWatts / maxPower).toFloat().coerceIn(0f, 1f))
                drawLine(
                    color = thermalColor,
                    start = Offset(leftPad, heatY),
                    end = Offset(leftPad + chartW, heatY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )

                // Local effective wind speed vertical marker
                val localX = leftPad + ((localWindMs / maxWind).toFloat().coerceIn(0f, 1f)) * chartW
                drawLine(
                    color = windColor.copy(alpha = 0.85f),
                    start = Offset(localX, topPad),
                    end = Offset(localX, topPad + chartH),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
                val markerLabel = textMeasurer.measure(
                    String.format("Вітер %.1f", localWindMs),
                    labelStyle.copy(color = windColor)
                )
                drawText(
                    textLayoutResult = markerLabel,
                    topLeft = Offset(
                        x = (localX + 4.dp.toPx()).coerceAtMost(size.width - markerLabel.size.width),
                        y = topPad + 2.dp.toPx()
                    )
                )

                // Cut-in speed subtle dot
                val cutInX = leftPad + ((cutInMs / maxWind).toFloat().coerceIn(0f, 1f)) * chartW
                drawCircle(
                    color = windColor,
                    radius = 4.dp.toPx(),
                    center = Offset(cutInX, topPad + chartH)
                )
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
