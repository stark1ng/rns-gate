package com.rnsgate.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rnsgate.app.R
import com.rnsgate.app.data.LxmfMessenger
import com.rnsgate.app.data.RnsNode
import com.rnsgate.app.data.SettingsStore
import com.rnsgate.app.ui.chat.ChatScreen
import com.rnsgate.app.ui.chat.ChatViewModel
import com.rnsgate.app.ui.gate.GateScreen
import com.rnsgate.app.ui.gate.GateViewModel
import com.rnsgate.app.ui.settings.SettingsScreen
import com.rnsgate.app.ui.settings.SettingsViewModel
import com.rnsgate.app.ui.tools.ToolsScreen
import com.rnsgate.app.ui.tools.ToolsViewModel

sealed class TopDest(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Gate : TopDest("gate", R.string.nav_gate, Icons.Filled.Hub, Icons.Outlined.Hub)
    data object Chat : TopDest("chat", R.string.nav_chat, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat)
    data object Tools : TopDest("tools", R.string.nav_tools, Icons.Filled.Build, Icons.Outlined.Build)
    data object Settings : TopDest("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val topDestinations = listOf(
    TopDest.Gate,
    TopDest.Chat,
    TopDest.Tools,
    TopDest.Settings
)

@Composable
fun RnsGateRoot(
    rnsNode: RnsNode,
    messenger: LxmfMessenger,
    settingsStore: SettingsStore
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                topDestinations.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = stringResource(dest.labelRes)
                            )
                        },
                        label = { Text(stringResource(dest.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Gate.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopDest.Gate.route) {
                val vm: GateViewModel = viewModel(
                    factory = GateViewModel.factory(rnsNode)
                )
                GateScreen(vm)
            }
            composable(TopDest.Chat.route) {
                val vm: ChatViewModel = viewModel(
                    factory = ChatViewModel.factory(messenger)
                )
                ChatScreen(vm)
            }
            composable(TopDest.Tools.route) {
                val vm: ToolsViewModel = viewModel(
                    factory = ToolsViewModel.factory(rnsNode)
                )
                ToolsScreen(vm)
            }
            composable(TopDest.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(settingsStore)
                )
                SettingsScreen(vm)
            }
        }
    }
}
