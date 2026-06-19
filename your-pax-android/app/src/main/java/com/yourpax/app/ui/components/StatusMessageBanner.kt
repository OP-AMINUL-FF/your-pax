package com.yourpax.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

enum class BannerKind { Info, Success, Warning, Error }

@Composable
fun StatusMessageBanner(
    message: String, kind: BannerKind = BannerKind.Info, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null
) {
    val colors = rememberAppColors()
    val bg: Color; val iconColor: Color; val icon: ImageVector
    when (kind) {
        BannerKind.Info -> { bg = colors.infoContainer; iconColor = colors.info; icon = Icons.Default.Info }
        BannerKind.Success -> { bg = colors.successContainer; iconColor = colors.success; icon = Icons.Default.CheckCircle }
        BannerKind.Warning -> { bg = colors.warningContainer; iconColor = colors.warning; icon = Icons.Default.Warning }
        BannerKind.Error -> { bg = MaterialTheme.colorScheme.errorContainer; iconColor = MaterialTheme.colorScheme.error; icon = Icons.Default.Error }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = bg,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = iconColor)
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = iconColor, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
