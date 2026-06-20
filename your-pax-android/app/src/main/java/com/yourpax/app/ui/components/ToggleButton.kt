package com.yourpax.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun ToggleButton(
    label: String, isActive: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    activeColor: Color? = null, inactiveColor: Color? = null
) {
    val colors = rememberAppColors()
    val bg = if (isActive) (activeColor ?: MaterialTheme.colorScheme.primary)
    else (inactiveColor ?: colors.subtleText.copy(alpha = 0.2f))
    val textColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = MaterialTheme.shapes.small,
        color = bg,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}
