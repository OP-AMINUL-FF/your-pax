package com.yourpax.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun SignalDisplay(signal: Int, modifier: Modifier = Modifier) {
    val appColors = rememberAppColors()
    val bars = when {
        signal >= -50 -> "\u2582\u2584\u2586\u2588"
        signal >= -65 -> "\u2582\u2584\u2586_"
        signal >= -80 -> "\u2582\u2584__"
        else -> "\u2582___"
    }
    val color = when {
        signal >= -50 -> appColors.success
        signal >= -65 -> appColors.warning
        else -> MaterialTheme.colorScheme.error
    }
    Row(modifier = modifier) {
        Text(text = bars, style = MaterialTheme.typography.bodySmall, color = color)
        Spacer(Modifier.width(4.dp))
        Text(text = "$signal dBm", style = MaterialTheme.typography.labelSmall, color = appColors.subtleText)
    }
}
