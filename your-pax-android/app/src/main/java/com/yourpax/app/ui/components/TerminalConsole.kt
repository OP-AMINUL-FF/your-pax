package com.yourpax.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

@Composable
fun TerminalConsole(
    text: String, modifier: Modifier = Modifier, maxHeight: Int = 150
) {
    val colors = rememberAppColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = if (maxHeight > 0) maxHeight.dp else Dp.Unspecified)
            .background(color = colors.terminalBackground, shape = MaterialTheme.shapes.small)
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = text.ifEmpty { "No output yet..." },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = colors.terminalText
        )
    }
}
