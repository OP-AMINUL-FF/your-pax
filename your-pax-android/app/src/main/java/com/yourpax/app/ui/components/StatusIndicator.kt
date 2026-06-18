package com.yourpax.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.Error
import com.yourpax.app.ui.theme.SubtleText
import com.yourpax.app.ui.theme.Success

@Composable
fun StatusIndicator(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp
) {
    val dotColor by animateColorAsState(
        targetValue = if (isActive) Success else Error,
        label = "dotColor"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SubtleText
        )
    }
}
