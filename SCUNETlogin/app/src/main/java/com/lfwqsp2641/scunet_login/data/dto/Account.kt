package com.lfwqsp2641.scunet_login.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val remark: String,
    val username: String,
    val password: String,
    val service: String
)
