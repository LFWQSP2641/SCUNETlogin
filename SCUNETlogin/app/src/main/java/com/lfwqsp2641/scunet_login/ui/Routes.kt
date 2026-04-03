package com.lfwqsp2641.scunet_login.ui

import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable
    object MainShell

    @Serializable
    data class AccountEditor(val id: String? = null)
}