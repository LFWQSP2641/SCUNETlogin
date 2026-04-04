package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.ui.Routes
import com.lfwqsp2641.scunet_login.ui.model.ServiceDisplay
import com.lfwqsp2641.scunet_login.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val accounts by viewModel.accountsState.collectAsState()
    val activatedId by viewModel.activatedIdState.collectAsState()
    val activatedAccount = accounts.firstOrNull { it.id == activatedId } ?: accounts.firstOrNull()
    val selectedOptionText =
        ServiceDisplay.entries.find { it.serviceType.code == activatedAccount?.service }
            ?: ServiceDisplay.EduNet

    val showAccountListSheet = remember { mutableStateOf(false) }
    val accountListSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val subtleDeeperContainerColor = lerp(
        MaterialTheme.colorScheme.secondaryContainer,
        Color.Black,
        0.08f
    )

    // Collect toast messages and show them as snackbars
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_description),
                            contentDescription = stringResource(R.string.account),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.account),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        FilledIconButton(
                            onClick = {
                                navController.navigate(Routes.AccountEditor(id = null))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = subtleDeeperContainerColor,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = stringResource(R.string.add_account)
                            )
                        }
                    }
                    if (activatedAccount == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_account),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = subtleDeeperContainerColor,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                onClick = { showAccountListSheet.value = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_draft),
                                        contentDescription = stringResource(R.string.configuration)
                                    )
                                    Text(
                                        text = activatedAccount.remark
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        painter = painterResource(R.drawable.ic_unfold_more),
                                        contentDescription = stringResource(R.string.toggle_account)
                                    )
                                }
                            }
                            Surface(
                                color = subtleDeeperContainerColor,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.student_id_label),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                        Text(
                                            text = activatedAccount.username,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.service_label),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                        Text(
                                            text = stringResource(selectedOptionText.label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        viewModel.startLogin()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.login))
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAccountListSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showAccountListSheet.value = false },
            sheetState = accountListSheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.account),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = 16.dp,
                    ),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 400.dp)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts, key = { account -> account.id }) { account ->
                        val menuExpanded = remember(account.id) { mutableStateOf(false) }

                        Card(
                            onClick = {
                                viewModel.activateAccount(account.id)
                                showAccountListSheet.value = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_draft),
                                    contentDescription = stringResource(R.string.configuration)
                                )
                                Column {
                                    Text(
                                        text = account.remark,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${account.service} - ${account.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Box {
                                    IconButton(
                                        onClick = {
                                            menuExpanded.value = true
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_more_vert),
                                            contentDescription = stringResource(R.string.more_action)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded.value,
                                        onDismissRequest = { menuExpanded.value = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.edit_action)) },
                                            onClick = {
                                                menuExpanded.value = false
                                                showAccountListSheet.value = false
                                                scope.launch {
                                                    yield()
                                                    navController.navigate(Routes.AccountEditor(id = account.id))
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete)) },
                                            onClick = {
                                                menuExpanded.value = false
                                                viewModel.deleteAccount(account.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
