package com.yourpax.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun InfoCard(text: String, modifier: Modifier = Modifier) {
    val appColors = rememberAppColors()
    SemanticCard(text = text, icon = Icons.Default.Info, containerColor = appColors.infoContainer, contentColor = appColors.info, modifier = modifier)
}

@Composable
fun WarningCard(text: String, modifier: Modifier = Modifier) {
    val appColors = rememberAppColors()
    SemanticCard(text = text, icon = Icons.Default.Warning, containerColor = appColors.warningContainer, contentColor = appColors.warning, modifier = modifier)
}

@Composable
fun SuccessCard(text: String, modifier: Modifier = Modifier) {
    val appColors = rememberAppColors()
    SemanticCard(text = text, icon = Icons.Default.CheckCircle, containerColor = appColors.successContainer, contentColor = appColors.success, modifier = modifier)
}

@Composable
fun ErrorCard(text: String, modifier: Modifier = Modifier) {
    SemanticCard(text = text, icon = Icons.Default.Error, containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error, modifier = modifier)
}

@Composable
private fun SemanticCard(
    text: String, icon: ImageVector, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = contentColor)
        }
    }
}
