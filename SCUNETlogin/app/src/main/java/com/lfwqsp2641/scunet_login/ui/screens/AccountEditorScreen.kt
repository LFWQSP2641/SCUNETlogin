package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.ui.model.ServiceDisplay
import com.lfwqsp2641.scunet_login.viewmodel.AccountEditorViewModel
import com.lfwqsp2641.scunet_login.viewmodel.AccountField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorScreen(
    accountId: String?,
    navController: NavController,
    viewModel: AccountEditorViewModel = viewModel()
) {
    val account by viewModel.uiState.collectAsState()
    val fieldErrors by viewModel.fieldErrors.collectAsState()
    val canSave by viewModel.canSave.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember {
        mutableStateOf(ServiceDisplay.entries.find { it.serviceType.code == account.service }
            ?: ServiceDisplay.EduNet)
    }

    LaunchedEffect(accountId) {
        if (accountId != null) viewModel.loadAccount(accountId)
    }

    LaunchedEffect(account.service) {
        selectedOptionText = ServiceDisplay.entries.find { it.serviceType.code == account.service }
            ?: ServiceDisplay.EduNet
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier, topBar = {
            TopAppBar(
                title = { Text(if (accountId == null) stringResource(R.string.new_account) else stringResource(R.string.edit_account)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                })
        }) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = account.remark,
                    isError = fieldErrors.remarkError != null,
                    supportingText = {
                        fieldErrors.remarkError?.let {
                            Text(stringResource(it))
                        }
                    },
                    onValueChange = {
                        viewModel.onFieldChange(
                            account.copy(remark = it),
                            touchedField = AccountField.Remark
                        )
                    },
                    label = { Text("名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = account.username,
                    isError = fieldErrors.usernameError != null,
                    supportingText = {
                        fieldErrors.usernameError?.let {
                            Text(stringResource(it))
                        }
                    },
                    onValueChange = {
                        viewModel.onFieldChange(
                            account.copy(username = it),
                            touchedField = AccountField.Username
                        )
                    },
                    label = { Text(stringResource(R.string.student_id)) },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = account.password,
                    isError = fieldErrors.passwordError != null,
                    supportingText = {
                        fieldErrors.passwordError?.let {
                            Text(stringResource(it))
                        }
                    },
                    onValueChange = {
                        viewModel.onFieldChange(
                            account.copy(password = it),
                            touchedField = AccountField.Password
                        )
                    },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    modifier = Modifier.fillMaxWidth(),
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                            .fillMaxWidth(),
                        value = stringResource(selectedOptionText.label),
                        isError = fieldErrors.serviceError != null,
                        supportingText = {
                            fieldErrors.serviceError?.let {
                                Text(stringResource(it))
                            }
                        },
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded, onDismissRequest = { expanded = false }) {
                        ServiceDisplay.entries.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(stringResource(selectionOption.label)) },
                                onClick = {
                                    selectedOptionText = selectionOption
                                    viewModel.onFieldChange(
                                        account.copy(service = selectionOption.serviceType.code),
                                        touchedField = AccountField.Service
                                    )
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
                Button(
                    onClick = { viewModel.save() },
                    enabled = canSave,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.save_configuration))
                }
            }
        }
    }
}
