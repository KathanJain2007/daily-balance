package com.spendtag.app.feature.home.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.spendtag.app.core.common.CurrencyFormatter

/**
 * Animated number count-up for monetary amounts.
 * Interpolates smoothly from 0 (or previous value) to target Long paise.
 */
@Composable
fun AnimatedPaiseText(
    targetPaise: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    val animatedPaise = remember { Animatable(0f) }

    LaunchedEffect(targetPaise) {
        animatedPaise.animateTo(
            targetValue = targetPaise.toFloat(),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Text(
        text = CurrencyFormatter.formatPaise(animatedPaise.value.toLong()),
        style = style,
        color = color,
        modifier = modifier
    )
}
