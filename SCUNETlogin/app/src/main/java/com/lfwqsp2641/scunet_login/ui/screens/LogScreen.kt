package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val viewModel: LogViewModel = viewModel()
    val logs by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.log),
                actions = {
                    IconButton(
                        onClick = {
                            if (logs.isNotEmpty()) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(logs.lastIndex)
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_double_arrow_down),
                            contentDescription = stringResource(R.string.scroll_to_bottom),
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearLogs() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clear_all),
                            contentDescription = stringResource(R.string.clear),
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(padding)
        ) {
            LogContent(logs = logs, listState = listState)
        }
    }
}

@Composable
private fun LogContent(logs: List<TaskLog>, listState: LazyListState) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(logs) { index, log ->
            val backgroundColor = if (index % 2 == 0) {
                Color.Transparent
            } else {
                MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
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
    val isDark = isSystemInDarkTheme()
    val contentColor = when (log.level) {
        TaskLog.LogLevel.ERROR -> MiuixTheme.colorScheme.error
        TaskLog.LogLevel.SUCCESS -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
        TaskLog.LogLevel.WARN -> if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
        else -> MiuixTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = log.timestamp,
            style = MiuixTheme.textStyles.footnote2.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal
            ),
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            modifier = Modifier.width(75.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = log.message,
            style = MiuixTheme.textStyles.footnote1.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            ),
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
    }
}
