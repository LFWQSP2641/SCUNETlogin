package com.lfwqsp2641.scunet_login.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun SCUNETloginTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorSchemeMode = when {
        dynamicColor -> if (darkTheme) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
        darkTheme -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.Light
    }

    val controller = remember(dynamicColor, darkTheme) {
        ThemeController(colorSchemeMode)
    }

    MiuixTheme(
        controller = controller,
        content = content
    )
}
