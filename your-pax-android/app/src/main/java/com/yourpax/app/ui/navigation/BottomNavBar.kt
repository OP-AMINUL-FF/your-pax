package com.yourpax.app.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    val items = Screen.bottomNavItems
    val isBottomNavRoute = currentRoute in items.map { it.route }
    val activeIndex = if (isBottomNavRoute) items.indexOfFirst { it.route == currentRoute } else 0
    val activeScreen = if (isBottomNavRoute) items[activeIndex] else items.first()

    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "notch"
    )

    val density = LocalDensity.current
    val appColors = rememberAppColors()
    val barColor = appColors.surfaceContainerHigh
    val bubbleColor = MaterialTheme.colorScheme.primary
    val bubbleIconTint = MaterialTheme.colorScheme.onPrimary
    val inactiveTint = appColors.subtleText
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        val tabWidthDp = maxWidth / items.size
        val bubbleSize = 56.dp
        val tabWidthPx = with(density) { tabWidthDp.toPx() }

        // ── LAYER 1: Fluid curved background ───────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height
            val cx = tabWidthPx * (animatedIndex + 0.5f)
            val notchWidth = tabWidthPx * 0.7f

            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(cx - notchWidth / 2, 0f)

                cubicTo(
                    cx - notchWidth / 4, 0f,
                    cx - notchWidth / 4, h * 0.55f,
                    cx, h * 0.55f
                )
                cubicTo(
                    cx + notchWidth / 4, h * 0.55f,
                    cx + notchWidth / 4, 0f,
                    cx + notchWidth / 2, 0f
                )

                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(path, color = barColor)
            drawPath(path, color = outlineColor.copy(alpha = 0.15f), style = Stroke(1f))
        }

        // ── LAYER 2: Floating active bubble ────────────────────────
        val bubbleAlpha = if (isBottomNavRoute) 1f else 0f
        val bubbleOffsetDp = tabWidthDp * (activeIndex + 0.5f) - bubbleSize / 2
        val animatedBubbleX by animateFloatAsState(
            targetValue = with(density) { bubbleOffsetDp.toPx() },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "bubbleX"
        )

        Box(
            modifier = Modifier
                .offset(y = 4.dp)
                .graphicsLayer { translationX = animatedBubbleX; alpha = bubbleAlpha }
                .size(bubbleSize)
                .shadow(8.dp, CircleShape, clip = false)
                .background(bubbleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = activeScreen.icon ?: Icons.Default.Home,
                contentDescription = null,
                tint = bubbleIconTint,
                modifier = Modifier.size(28.dp)
            )
        }

        // ── LAYER 3: Interactive row + labels ──────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .align(Alignment.BottomCenter)
        ) {
            items.forEachIndexed { index, screen ->
                val isSelected = index == activeIndex

                val alphaAnim by animateFloatAsState(
                    targetValue = if (isSelected && isBottomNavRoute) 0f else 1f,
                    animationSpec = tween(150),
                    label = "itemAlpha"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(screen) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.graphicsLayer { alpha = alphaAnim }
                    ) {
                        Icon(
                            imageVector = screen.icon ?: Icons.Default.Home,
                            contentDescription = screen.title,
                            tint = inactiveTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = screen.title,
                            color = inactiveTint,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
