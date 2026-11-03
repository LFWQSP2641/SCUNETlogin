package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.ui.model.ServiceDisplay
import com.lfwqsp2641.scunet_login.viewmodel.AccountEditorViewModel
import com.lfwqsp2641.scunet_login.viewmodel.AccountField
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AccountEditorScreen(
    accountId: String?,
    navController: NavController,
) {
    val viewModel: AccountEditorViewModel = viewModel()
    val account by viewModel.uiState.collectAsStateWithLifecycle()
    val fieldErrors by viewModel.fieldErrors.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val selectedIndex =
        ServiceDisplay.entries.indexOfFirst { it.serviceType.code == account.service }
            .takeIf { it != -1 } ?: 0

    LaunchedEffect(accountId) {
        if (accountId != null) viewModel.loadAccount(accountId)
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            navController.pop()
        }
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            SmallTopAppBar(
                title = if (accountId == null) stringResource(R.string.new_account) else stringResource(
                    R.string.edit_account
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.pop() }) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = account.remark,
                            onValueChange = {
                                viewModel.onFieldChange(
                                    account.copy(remark = it),
                                    touchedField = AccountField.Remark
                                )
                            },
                            label = stringResource(R.string.remark),
                            singleLine = true
                        )
                        if (fieldErrors.remarkError != null) {
                            Text(
                                text = stringResource(fieldErrors.remarkError!!),
                                color = MiuixTheme.colorScheme.error,
                                style = MiuixTheme.textStyles.footnote1,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = account.username,
                            onValueChange = {
                                viewModel.onFieldChange(
                                    account.copy(username = it),
                                    touchedField = AccountField.Username
                                )
                            },
                            label = stringResource(R.string.student_id),
                            singleLine = true
                        )
                        if (fieldErrors.usernameError != null) {
                            Text(
                                text = stringResource(fieldErrors.usernameError!!),
                                color = MiuixTheme.colorScheme.error,
                                style = MiuixTheme.textStyles.footnote1,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = account.password,
                            onValueChange = {
                                viewModel.onFieldChange(
                                    account.copy(password = it),
                                    touchedField = AccountField.Password
                                )
                            },
                            label = stringResource(R.string.password),
                            singleLine = true
                        )
                        if (fieldErrors.passwordError != null) {
                            Text(
                                text = stringResource(fieldErrors.passwordError!!),
                                color = MiuixTheme.colorScheme.error,
                                style = MiuixTheme.textStyles.footnote1,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        OverlayDropdownPreference(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.service),
                            items = ServiceDisplay.entries.map { stringResource(it.label) },
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = { index ->
                                viewModel.onFieldChange(
                                    account.copy(service = ServiceDisplay.entries[index].serviceType.code),
                                    touchedField = AccountField.Service
                                )
                            }
                        )
                        if (fieldErrors.serviceError != null) {
                            Text(
                                text = stringResource(fieldErrors.serviceError!!),
                                color = MiuixTheme.colorScheme.error,
                                style = MiuixTheme.textStyles.footnote1,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.save() },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    cornerRadius = 16.dp
                ) {
                    Text(stringResource(R.string.save_configuration))
                }
            }
        }
    }
}
