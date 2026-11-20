package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.ui.Routes
import com.lfwqsp2641.scunet_login.ui.TabRoutes
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RootScreen() {
    val backStack = rememberNavBackStack<Routes>(Routes.MainShell)
    val navController = NavController(backStack)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        entry<Routes.MainShell> {
            MainShell(rootNavController = navController)
        }

        entry<Routes.AccountEditor> { route ->
            AccountEditorScreen(
                accountId = route.id,
                navController = navController
            )
        }
    }
}

@Composable
fun MainShell(rootNavController: NavController) {
    val tabBackStack = rememberNavBackStack<TabRoutes>(TabRoutes.Home)
    val tabNavController = NavController(tabBackStack)

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val selected = tabBackStack.lastOrNull() == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                when (tab.route) {
                                    TabRoutes.Home -> {
                                        while (tabBackStack.size > 1) {
                                            tabBackStack.removeLastOrNull()
                                        }
                                    }

                                    TabRoutes.Log -> {
                                        tabNavController.push(TabRoutes.Log)
                                    }
                                }
                            }
                        },
                        icon = ImageVector.vectorResource(tab.icon),
                        label = stringResource(tab.label),
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = tabBackStack,
            onBack = { tabBackStack.removeLastOrNull() },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(MiuixTheme.colorScheme.background)
        ) {
            entry<TabRoutes.Home> {
                HomeScreen(Modifier, rootNavController)
            }

            entry<TabRoutes.Log> {
                LogScreen()
            }
        }
    }
}

enum class AppTab(
    val label: Int,
    val icon: Int,
    val route: TabRoutes
) {
    HOME(R.string.home, R.drawable.ic_home, TabRoutes.Home),
    LOG(R.string.log, R.drawable.ic_description, TabRoutes.Log),
}
