package com.yourpax.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun EmptyState(
    title: String, subtitle: String = "", modifier: Modifier = Modifier
) {
    val appColors = rememberAppColors()
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = appColors.subtleText, textAlign = TextAlign.Center)
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = appColors.subtleText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}
