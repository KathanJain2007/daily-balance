package com.spendtag.app.feature.insights.component

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtag.app.core.database.model.MonthlyTrendItem

/**
 * Custom Month-over-Month Trend Line Chart drawn with Compose Canvas.
 * No third-party chart library.
 * Features animated bezier curve, gradient fill, gridlines, and month labels.
 */
@Composable
fun TrendLineChart(
    trendData: List<MonthlyTrendItem>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(trendData) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (trendData.isEmpty()) return@Canvas

            val chartWidth = size.width
            val chartHeight = size.height - 40f // Leave space for X-axis labels
            val maxPaise = trendData.maxOfOrNull { it.totalSpentPaise }?.coerceAtLeast(100000L) ?: 100000L

            // 1. Draw subtle horizontal gridlines (0%, 50%, 100%)
            listOf(0.2f, 0.6f, 1.0f).forEach { ratio ->
                val y = chartHeight * (1f - ratio)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. Compute point coordinates
            val pointSpacing = if (trendData.size > 1) chartWidth / (trendData.size - 1) else chartWidth
            val points = trendData.mapIndexed { index, item ->
                val x = index * pointSpacing
                val normalizedY = (item.totalSpentPaise.toFloat() / maxPaise.toFloat()).coerceIn(0.05f, 1f)
                val targetY = chartHeight * (1f - normalizedY)
                // Apply animation interpolation
                val animatedY = chartHeight - (chartHeight - targetY) * animationProgress.value
                Offset(x, animatedY)
            }

            // 3. Construct Smooth Cubic Bezier Path
            val strokePath = Path()
            val fillPath = Path()

            if (points.isNotEmpty()) {
                strokePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, chartHeight)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]

                    val controlX = (p0.x + p1.x) / 2f
                    strokePath.cubicTo(
                        controlX, p0.y,
                        controlX, p1.y,
                        p1.x, p1.y
                    )
                    fillPath.cubicTo(
                        controlX, p0.y,
                        controlX, p1.y,
                        p1.x, p1.y
                    )
                }

                fillPath.lineTo(points.last().x, chartHeight)
                fillPath.close()

                // 4. Draw Gradient Fill beneath curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.28f * animationProgress.value),
                            lineColor.copy(alpha = 0.0f)
                        ),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                // 5. Draw Curve Line
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // 6. Draw Data Dots and Month Labels
                val textPaint = Paint().apply {
                    color = labelColor
                    textSize = 11.sp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                points.forEachIndexed { i, point ->
                    // Dot on line
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 3.dp.toPx(),
                        center = point
                    )

                    // Month text label beneath X-axis
                    drawContext.canvas.nativeCanvas.drawText(
                        trendData[i].monthLabel,
                        point.x,
                        size.height - 8f,
                        textPaint
                    )
                }
            }
        }
    }
}
