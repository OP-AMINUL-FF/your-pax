package com.yourpax.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    data object Home : Screen("home", "Home", Icons.Outlined.Home)
    data object WiFi : Screen("wifi", "WiFi", Icons.Outlined.Wifi)

    // Bottom nav: AP (Evil AP)
    data object EvilAp : Screen("evil_ap", "AP", Icons.Outlined.WifiTethering)

    // Non-bottom-nav routes
    data object Network : Screen("network", "Network Scan")
    data object Loot : Screen("loot", "Loot")
    data object More : Screen("more", "More")
    data object Store : Screen("store", "Store Dashboard")
    data object Config : Screen("config", "Configuration")
    data object ManualMode : Screen("manual_mode", "Manual Mode")
    data object WifiConnect : Screen("wifi_connect", "WiFi Connect")
    data object NetKB : Screen("netkb", "NetKB")
    data object BackupRestore : Screen("backup_restore", "Backup & Restore")
    data object Epd : Screen("epd", "EPD Live")
    data object Logs : Screen("logs", "Logs")
    data object BluetoothDevices : Screen("bluetooth_devices", "Bluetooth Devices")
    data object Settings : Screen("settings", "Settings")

    companion object {
        val bottomNavItems = listOf(WiFi, Home, EvilAp)
    }
}
