package com.lfwqsp2641.scunet_login.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val accounts: List<Account> = emptyList(),
    val activatedId: String? = null
)
