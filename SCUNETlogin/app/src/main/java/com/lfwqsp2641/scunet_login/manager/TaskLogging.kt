package com.lfwqsp2641.scunet_login.manager

import com.lfwqsp2641.scunet_login.data.model.TaskLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TaskLogging {
    const val MAX_LOGS = 1000
    private val logBuffer = java.util.ArrayDeque<TaskLog>(MAX_LOGS)
    private val _logs = MutableStateFlow<List<TaskLog>>(emptyList())
    val logs = _logs.asStateFlow()

    fun addLog(message: String, level: TaskLog.LogLevel = TaskLog.LogLevel.INFO) {
        synchronized(this) {
            if (logBuffer.size >= MAX_LOGS) {
                logBuffer.removeFirst()
            }
            logBuffer.addLast(
                TaskLog(
                    timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    message = message,
                    level = level
                )
            )
            _logs.update { logBuffer.toList() }
        }
    }

    fun clearLogs() {
        synchronized(this) {
            logBuffer.clear()
            _logs.value = emptyList()
        }
    }
}