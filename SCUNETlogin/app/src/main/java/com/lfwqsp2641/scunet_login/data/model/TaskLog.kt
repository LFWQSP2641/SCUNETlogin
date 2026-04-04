package com.lfwqsp2641.scunet_login.data.model

data class TaskLog(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
) {
    enum class LogLevel { INFO, SUCCESS, ERROR, WARN }
}
