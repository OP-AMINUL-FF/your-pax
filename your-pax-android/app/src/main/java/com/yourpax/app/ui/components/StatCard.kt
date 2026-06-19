package com.yourpax.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun StatCard(
    label: String, value: String, accentColor: Color, modifier: Modifier = Modifier
) {
    val appColors = rememberAppColors()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = accentColor.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = accentColor))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = appColors.subtleText)
            }
        }
    }
}

@Composable
fun StatCardSm(
    label: String, value: String, accentColor: Color, modifier: Modifier = Modifier
) {
    val appColors = rememberAppColors()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = accentColor.copy(alpha = 0.06f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = accentColor))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = appColors.subtleText)
        }
    }
}
