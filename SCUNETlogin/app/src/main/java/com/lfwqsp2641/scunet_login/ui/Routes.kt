package com.lfwqsp2641.scunet_login.ui

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed interface Routes : NavKey {
    @Serializable
    data object MainShell : Routes

    @Serializable
    data class AccountEditor(val id: String? = null) : Routes
}