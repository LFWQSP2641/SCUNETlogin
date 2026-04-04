package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.data.model.TaskLog
import com.lfwqsp2641.scunet_login.viewmodel.LogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(modifier: Modifier = Modifier, viewModel: LogViewModel = viewModel()) {
    val logs by viewModel.uiState.collectAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log)) },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearLogs() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clear_all),
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LogContent(logs = logs)
        }
    }
}

@Composable
private fun LogContent(logs: List<TaskLog>) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.isNotEmpty()) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(logs) { index, log ->
            val backgroundColor = if (index % 2 == 0) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            }

            LogItem(
                log = log,
                modifier = Modifier.background(backgroundColor)
            )
        }
    }
}

@Composable
fun LogItem(log: TaskLog, modifier: Modifier = Modifier) {
    val contentColor = when (log.level) {
        TaskLog.LogLevel.ERROR -> MaterialTheme.colorScheme.error
        TaskLog.LogLevel.SUCCESS -> Color(0xFF4CAF50)
        TaskLog.LogLevel.WARN -> Color(0xFFFFB300)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = log.timestamp,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(70.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = log.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp // 紧凑行高
            ),
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
    }
}
