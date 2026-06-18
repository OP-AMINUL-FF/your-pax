package com.yourpax.app.ui.screens.wifi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.WiFiNetwork
import com.yourpax.app.ui.theme.Primary
import com.yourpax.app.ui.theme.SubtleText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiTargetSelect(
    networks: List<WiFiNetwork>,
    onSelect: (WiFiNetwork) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select Target",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(networks) { network ->
                    Card(
                        onClick = { onSelect(network) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Primary)
                            Column {
                                Text(network.ssid.ifEmpty { "<Hidden>" }, fontWeight = FontWeight.Medium)
                                Text(
                                    "${network.bssid} · Ch ${network.channel} · ${network.signal}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SubtleText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
