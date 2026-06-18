package com.yourpax.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    data object Home : Screen("home", "Home", Icons.Outlined.Home)
    data object Network : Screen("network", "Network", Icons.Outlined.Cloud)
    data object WiFi : Screen("wifi", "WiFi", Icons.Outlined.Wifi)
    data object Loot : Screen("loot", "Loot", Icons.Outlined.Description)
    data object More : Screen("more", "More", Icons.Outlined.MoreHoriz)

    // Non-bottom-nav routes
    data object EvilAp : Screen("evil_ap", "Evil Twin AP")
    data object Store : Screen("store", "Store Dashboard")
    data object Config : Screen("config", "Configuration")
    data object ManualMode : Screen("manual_mode", "Manual Mode")
    data object WifiConnect : Screen("wifi_connect", "WiFi Connect")
    data object NetKB : Screen("netkb", "NetKB")
    data object BackupRestore : Screen("backup_restore", "Backup & Restore")
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Network, WiFi, Loot, Settings)
    }
}
