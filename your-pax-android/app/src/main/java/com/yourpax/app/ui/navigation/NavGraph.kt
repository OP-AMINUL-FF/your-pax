package com.yourpax.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.ui.screens.backup.BackupRestoreScreen
import com.yourpax.app.ui.screens.bluetooth.BluetoothDevicesScreen
import com.yourpax.app.ui.screens.config.ConfigScreen
import com.yourpax.app.ui.screens.epd.EPDScreen
import com.yourpax.app.ui.screens.home.HomeScreen
import com.yourpax.app.ui.screens.logs.LogScreen
import com.yourpax.app.ui.screens.loot.LootScreen
import com.yourpax.app.ui.screens.manual.ManualModeScreen
import com.yourpax.app.ui.screens.more.MoreScreen
import com.yourpax.app.ui.screens.netkb.NetKBScreen
import com.yourpax.app.ui.screens.network.NetworkDetailScreen
import com.yourpax.app.ui.screens.network.NetworkScreen
import com.yourpax.app.ui.screens.settings.SettingsScreen
import com.yourpax.app.ui.screens.splash.SplashScreen
import com.yourpax.app.ui.screens.store.StoreScreen
import com.yourpax.app.ui.screens.wifi.EvilAPScreen
import com.yourpax.app.ui.screens.wifi.WiFiScreen
import com.yourpax.app.ui.screens.wificonnect.WiFiConnectScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val bottomNavRoutes = Screen.bottomNavItems.map { it.route }
    val showBottomBar = currentRoute in bottomNavRoutes

    var isConnected by remember { mutableStateOf(false) }

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
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
                        onOpenDrawer = openDrawer,
                        onNavigateToStore = { navController.navigate(Screen.Store.route) },
                        onNavigateToConfig = { navController.navigate(Screen.Config.route) },
                        onNavigateToManual = { navController.navigate(Screen.ManualMode.route) },
                        onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToNetwork = { navController.navigate(Screen.Network.route) },
                        onNavigateToLoot = { navController.navigate(Screen.Loot.route) },
                        onNavigateToNetKB = { navController.navigate(Screen.NetKB.route) },
                        onNavigateToEpd = { navController.navigate(Screen.Epd.route) },
                        onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                        onNavigateToMore = { navController.navigate(Screen.More.route) },
                        onNavigateToBluetooth = { navController.navigate(Screen.BluetoothDevices.route) }
                    )
                }
                composable(Screen.WiFi.route) {
                    WiFiScreen(
                        onOpenDrawer = openDrawer,
                        onNavigateToEvilAp = { navController.navigate(Screen.EvilAp.route) },
                        onNavigateToWifiConnect = { navController.navigate(Screen.WifiConnect.route) }
                    )
                }
                composable(Screen.EvilAp.route) {
                    EvilAPScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.Network.route) {
                    NetworkScreen(
                        onOpenDrawer = openDrawer,
                        onNavigateToNetworkDetail = { ip -> navController.navigate("network_detail/$ip") }
                    )
                }
                composable(
                    route = "network_detail/{ip}",
                    arguments = listOf(navArgument("ip") { type = NavType.StringType })
                ) { backStackEntry ->
                    NetworkDetailScreen(ip = backStackEntry.arguments?.getString("ip") ?: "", onOpenDrawer = openDrawer)
                }
                composable(Screen.WifiConnect.route) {
                    WiFiConnectScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.Store.route) {
                    StoreScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.Config.route) {
                    ConfigScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.ManualMode.route) {
                    ManualModeScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.Loot.route) {
                    LootScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.More.route) {
                    MoreScreen(onOpenDrawer = openDrawer,
                        onNavigateToStore = { navController.navigate(Screen.Store.route) },
                        onNavigateToConfig = { navController.navigate(Screen.Config.route) },
                        onNavigateToManual = { navController.navigate(Screen.ManualMode.route) },
                        onNavigateToNetKB = { navController.navigate(Screen.NetKB.route) },
                        onNavigateToEpd = { navController.navigate(Screen.Epd.route) },
                        onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                        onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToBluetooth = { navController.navigate(Screen.BluetoothDevices.route) }
                    )
                }
                composable(Screen.Epd.route) {
                    EPDScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.Logs.route) {
                    LogScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.NetKB.route) {
                    NetKBScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.BackupRestore.route) {
                    BackupRestoreScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.BluetoothDevices.route) {
                    BluetoothDevicesScreen(onOpenDrawer = openDrawer)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(onOpenDrawer = openDrawer,
                        onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                        onNavigateToConfig = { navController.navigate(Screen.Config.route) }
                    )
                }
            }
        }
    }
}
