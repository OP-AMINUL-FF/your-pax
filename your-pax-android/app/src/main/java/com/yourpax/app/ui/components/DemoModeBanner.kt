package com.yourpax.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.ui.theme.Warning

@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    if (ConnectionState.isDemoMode) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Warning.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Warning, modifier = Modifier.size(16.dp))
            Text("Demo Mode — showing sample data", style = MaterialTheme.typography.bodySmall, color = Warning)
        }
    }
}
