package com.yourpax.app.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.ui.theme.rememberAppColors

private data class NavEntry(val label: String, val icon: @Composable () -> Unit, val route: String)

private val drawerEntries = listOf(
    NavEntry("Home", { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.Home.route),
    NavEntry("WiFi", { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.WiFi.route),
    NavEntry("AP", { Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.EvilAp.route),
    NavEntry("Bluetooth", { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.BluetoothDevices.route),
    NavEntry("Network Scan", { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.Network.route),
    NavEntry("Configuration", { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.Config.route),
    NavEntry("Store Dashboard", { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.Store.route),
    NavEntry("Manual Mode", { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.ManualMode.route),
    NavEntry("NetKB", { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.NetKB.route),
    NavEntry("EPD Live", { Icon(Icons.Default.ScreenshotMonitor, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.Epd.route),
    NavEntry("Logs", { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.Logs.route),
    NavEntry("Backup/Restore", { Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.BackupRestore.route),
    NavEntry("Settings", { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.Settings.route),
    NavEntry("Loot", { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.Loot.route),
    NavEntry("All Screens", { Icon(Icons.Default.MoreHoriz, contentDescription = null, modifier = Modifier.size(20.dp)) }, Screen.More.route),
)

@Composable
fun AppDrawerContent(
    currentRoute: String?, onNavigate: (String) -> Unit, onClose: () -> Unit
) {
    val appColors = rememberAppColors()

    Surface(
        modifier = Modifier.fillMaxHeight().width(300.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("your-pax", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Offensive Security Cyberdeck", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                drawerEntries.forEach { entry ->
                    val selected = currentRoute == entry.route
                    NavigationDrawerItem(
                        icon = {
                            entry.icon()
                        },
                        label = {
                            Text(
                                entry.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = { onClose(); onNavigate(entry.route) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = appColors.info.copy(alpha = 0.1f),
                            unselectedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
