package com.yourpax.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FadeScaleIn(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(0.8f) }
    val animAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        }
        launch {
            animAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = animAlpha.value
        }
    ) {
        content()
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier.background(
            color = Color.Gray.copy(alpha = alpha),
            shape = shape
        )
    )
}

@Composable
fun BreathingGlow(
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    minAlpha: Float = 0.4f,
    maxAlpha: Float = 1f,
    duration: Int = 1500
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val animAlpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathAlpha"
    )

    Box(
        modifier = modifier.background(color.copy(alpha = animAlpha), CircleShape)
    )
}

@Composable
fun StatusGlowIndicator(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color = Color.Gray,
    size: Dp = 12.dp
) {
    val pulseScale = remember { Animatable(1f) }
    val pulseAlpha = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                launch { pulseScale.animateTo(1.4f, tween(600)) }
                launch { pulseAlpha.animateTo(0.6f, tween(600)) }
                delay(200)
                launch { pulseScale.animateTo(1f, tween(600)) }
                launch { pulseAlpha.animateTo(0f, tween(600)) }
                delay(1200)
            }
        } else {
            pulseScale.snapTo(1f)
            pulseAlpha.snapTo(0f)
        }
    }

    val color = if (isActive) activeColor else inactiveColor
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(pulseScale.value)
                .graphicsLayer { alpha = pulseAlpha.value }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(color)
                .shadow(4.dp, CircleShape)
        )
    }
}