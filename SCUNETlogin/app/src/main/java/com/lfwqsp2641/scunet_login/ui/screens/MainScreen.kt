package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.ui.Routes

@Preview(locale = "zh-rCN", showBackground = true)
@Composable
fun RootScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MainShell,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        composable<Routes.MainShell> {
            MainShell(rootNavController = navController)
        }

        composable<Routes.AccountEditor> { backStackEntry ->
            val route: Routes.AccountEditor = backStackEntry.toRoute()

            AccountEditorScreen(
                accountId = route.id,
                navController = navController
            )
        }
    }
}

@Composable
fun MainShell(rootNavController: NavHostController) {
    val innerNavController = rememberNavController()
    val backStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { dest ->
                item(
                    icon = { Icon(painterResource(dest.icon), contentDescription = null) },
                    label = { Text(stringResource(dest.label)) },
                    selected = currentRoute == dest.route,
                    onClick = {
                        innerNavController.navigate(dest.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) {
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
            navController = innerNavController,
            startDestination = AppDestinations.HOME.route,
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            composable(AppDestinations.HOME.route) { HomeScreen(Modifier, rootNavController) }
            composable(AppDestinations.LOG.route) { LogScreen() }
            // composable(AppDestinations.PROFILE.route) { SettingsScreen() }
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
    // PROFILE("settings", R.string.settings, R.drawable.ic_settings),
}
