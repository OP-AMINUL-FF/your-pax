package com.yourpax.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun FullscreenToggle(
    isFullscreen: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier
) {
    val colors = rememberAppColors()
    Row(
        modifier = modifier.clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            contentDescription = null, tint = colors.subtleText, modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = if (isFullscreen) "Collapse" else "Fullscreen", style = MaterialTheme.typography.labelSmall, color = colors.subtleText)
    }
}
