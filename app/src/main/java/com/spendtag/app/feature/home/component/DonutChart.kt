package com.spendtag.app.feature.home.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.feature.home.model.CategorySpendingUiModel
import com.spendtag.app.ui.theme.AmountMediumStyle

/**
 * Custom Donut Chart drawn completely with Jetpack Compose Canvas.
 * No third-party chart libraries used.
 * Features animated stroke sweep, rounded caps, and center total.
 */
@Composable
fun DonutChart(
    categories: List<CategorySpendingUiModel>,
    totalSpentPaise: Long,
    modifier: Modifier = Modifier,
    chartSize: Dp = 190.dp,
    strokeWidth: Dp = 22.dp
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categories, totalSpentPaise) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(chartSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(chartSize)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)

            if (categories.isEmpty() || totalSpentPaise == 0L) {
                // Empty state track
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                var currentStartAngle = -90f // Start from 12 o'clock

                categories.forEach { item ->
                    val targetSweep = (item.percentage / 100f) * 360f
                    // Apply animated sweep progress
                    val animatedSweep = targetSweep * animationProgress.value

                    // Add a tiny gap between segments
                    val gap = if (categories.size > 1) 3f else 0f
                    val sweepWithGap = (animatedSweep - gap).coerceAtLeast(0f)

                    if (sweepWithGap > 0f) {
                        drawArc(
                            color = item.category.color,
                            startAngle = currentStartAngle + (gap / 2f),
                            sweepAngle = sweepWithGap,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    currentStartAngle += targetSweep
                }
            }
        }

        // Center Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.formatPaise(totalSpentPaise),
                style = AmountMediumStyle,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (categories.isNotEmpty()) {
                Text(
                    text = "${categories.size} categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}
