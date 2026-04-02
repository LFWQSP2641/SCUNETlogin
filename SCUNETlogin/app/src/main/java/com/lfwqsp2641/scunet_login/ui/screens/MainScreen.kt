package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lfwqsp2641.scunet_login.R

@Preview(locale = "zh-rCN", showBackground = true)
@Composable
fun SCUNETloginApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { dest ->
                item(
                    icon = { Icon(painterResource(dest.icon), contentDescription = null) },
                    label = { Text(stringResource(dest.label)) },
                    selected = currentRoute == dest.route,
                    onClick = {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestinations.HOME.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestinations.HOME.route) { HomeScreen() }
                composable(AppDestinations.LOG.route) { LogScreen() }
                composable(AppDestinations.PROFILE.route) { SettingsScreen() }
            }
        }
    }
}

enum class AppDestinations(
    val route: String,
    val label: Int,
    val icon: Int
) {
    HOME("home", R.string.home, R.drawable.ic_home),
    LOG("log", R.string.log, R.drawable.ic_description),
    PROFILE("settings", R.string.settings, R.drawable.ic_settings),
}
