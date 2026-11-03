package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.ui.Routes
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
    var currentTab by remember { mutableStateOf(AppTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = ImageVector.vectorResource(tab.icon),
                        label = stringResource(tab.label),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(MiuixTheme.colorScheme.background)
        ) {
            when (currentTab) {
                AppTab.HOME -> HomeScreen(Modifier, rootNavController)
                AppTab.LOG -> LogScreen()
            }
        }
    }
}

enum class AppTab(
    val label: Int,
    val icon: Int
) {
    HOME(R.string.home, R.drawable.ic_home),
    LOG(R.string.log, R.drawable.ic_description),
}
