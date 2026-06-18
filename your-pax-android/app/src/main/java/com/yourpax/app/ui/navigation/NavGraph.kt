package com.yourpax.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.ui.screens.splash.SplashScreen
import com.yourpax.app.ui.screens.home.HomeScreen
import com.yourpax.app.ui.screens.network.NetworkScreen
import com.yourpax.app.ui.screens.network.NetworkDetailScreen
import com.yourpax.app.ui.screens.wifi.WiFiScreen
import com.yourpax.app.ui.screens.wifi.EvilAPScreen
import com.yourpax.app.ui.screens.wificonnect.WiFiConnectScreen
import com.yourpax.app.ui.screens.loot.LootScreen
import com.yourpax.app.ui.screens.more.MoreScreen
import com.yourpax.app.ui.screens.store.StoreScreen
import com.yourpax.app.ui.screens.config.ConfigScreen
import com.yourpax.app.ui.screens.manual.ManualModeScreen
import com.yourpax.app.ui.screens.epd.EPDScreen
import com.yourpax.app.ui.screens.logs.LogScreen
import com.yourpax.app.ui.screens.backup.BackupRestoreScreen
import com.yourpax.app.ui.screens.netkb.NetKBScreen
import com.yourpax.app.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = Screen.bottomNavItems.map { it.route }
    val showBottomBar = currentRoute in bottomNavRoutes

    var isConnected by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (isConnected && showBottomBar) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { screen ->
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(padding)
        ) {
            composable("splash") {
                SplashScreen(
                    onConnected = {
                        ConnectionState.isDemoMode = false
                        isConnected = true
                        navController.navigate(Screen.Home.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    onSkip = {
                        ConnectionState.isDemoMode = true
                        isConnected = true
                        navController.navigate(Screen.Home.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToStore = { navController.navigate(Screen.Store.route) },
                    onNavigateToConfig = { navController.navigate(Screen.Config.route) },
                    onNavigateToManual = { navController.navigate(Screen.ManualMode.route) },
                    onNavigateToLogs = { navController.navigate("logs") },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToNetwork = { navController.navigate(Screen.Network.route) },
                    onNavigateToMore = { navController.navigate(Screen.More.route) }
                )
            }
            composable(Screen.Network.route) {
                NetworkScreen()
            }
            composable(
                route = "network_detail/{ip}",
                arguments = listOf(navArgument("ip") { type = NavType.StringType })
            ) { backStackEntry ->
                val ip = backStackEntry.arguments?.getString("ip") ?: ""
                NetworkDetailScreen(ip = ip)
            }
            composable(Screen.WiFi.route) {
                WiFiScreen(
                    onNavigateToEvilAp = { navController.navigate(Screen.EvilAp.route) },
                    onNavigateToWifiConnect = { navController.navigate(Screen.WifiConnect.route) }
                )
            }
            composable(Screen.EvilAp.route) {
                EvilAPScreen()
            }
            composable(Screen.WifiConnect.route) {
                WiFiConnectScreen()
            }
            composable(Screen.Store.route) {
                StoreScreen()
            }
            composable(Screen.Config.route) {
                ConfigScreen()
            }
            composable(Screen.ManualMode.route) {
                ManualModeScreen()
            }
            composable(Screen.Loot.route) {
                LootScreen()
            }
            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToStore = { navController.navigate(Screen.Store.route) },
                    onNavigateToConfig = { navController.navigate(Screen.Config.route) },
                    onNavigateToManual = { navController.navigate(Screen.ManualMode.route) },
                    onNavigateToNetKB = { navController.navigate(Screen.NetKB.route) },
                    onNavigateToEpd = { navController.navigate("epd") },
                    onNavigateToLogs = { navController.navigate("logs") },
                    onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable("epd") {
                EPDScreen()
            }
            composable("logs") {
                LogScreen()
            }
            composable(Screen.NetKB.route) {
                NetKBScreen()
            }
            composable(Screen.BackupRestore.route) {
                BackupRestoreScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                    onNavigateToConfig = { navController.navigate(Screen.Config.route) }
                )
            }
        }
    }
}
