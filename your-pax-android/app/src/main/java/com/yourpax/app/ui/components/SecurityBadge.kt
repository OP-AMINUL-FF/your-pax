package com.yourpax.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun SecurityBadge(security: String, modifier: Modifier = Modifier) {
    val appColors = rememberAppColors()
    val (bg, label) = when {
        security.contains("WPA3", ignoreCase = true) -> appColors.badgeWpa3 to "WPA3"
        security.contains("WPA2", ignoreCase = true) -> appColors.badgeWpa2 to "WPA2"
        security.contains("WPA", ignoreCase = true) -> appColors.badgeWpa2 to "WPA"
        security.contains("WEP", ignoreCase = true) -> appColors.badgeWps to "WEP"
        security.contains("OPEN", ignoreCase = true) || security == "Open" -> appColors.badgeOpen to "OPEN"
        security.contains("Enterprise", ignoreCase = true) -> appColors.badgeWpa3 to "ENT"
        else -> appColors.badgeWpa2 to security.take(6)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = bg,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimary))
        }
    }
}
