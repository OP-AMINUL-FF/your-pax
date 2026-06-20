package com.yourpax.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FontSizeControl(
    currentSize: Float, onDecrease: () -> Unit, onIncrease: () -> Unit, modifier: Modifier = Modifier,
    minSize: Float = 10f, maxSize: Float = 24f
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrease, enabled = currentSize > minSize) {
            Icon(Icons.Default.TextDecrease, contentDescription = "Decrease font", modifier = Modifier.size(20.dp))
        }
        Text(text = "${currentSize.toInt()}", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onIncrease, enabled = currentSize < maxSize) {
            Icon(Icons.Default.TextIncrease, contentDescription = "Increase font", modifier = Modifier.size(20.dp))
        }
    }
}
