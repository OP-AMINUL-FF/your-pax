package com.yourpax.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

enum class DotState { Inactive, Active, Error }

@Composable
fun StatusDot(state: DotState = DotState.Inactive, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val appColors = rememberAppColors()
    val targetColor = when (state) {
        DotState.Active -> appColors.success
        DotState.Error -> appColors.warning
        DotState.Inactive -> appColors.subtleText
    }
    val color by animateColorAsState(targetValue = targetColor, label = "statusDot")
    Box(modifier = modifier.size(size).clip(CircleShape).background(color))
}
