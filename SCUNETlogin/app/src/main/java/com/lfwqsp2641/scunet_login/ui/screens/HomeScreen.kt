package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.foundation.clickable
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
import com.lfwqsp2641.scunet_login.data.dto.Account
import com.lfwqsp2641.scunet_login.ui.Routes
import com.lfwqsp2641.scunet_login.ui.model.ServiceDisplay
import com.lfwqsp2641.scunet_login.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

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

    val showAccountListSheet = remember { mutableStateOf(false) }
    val accountListSheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentSsid by viewModel.currentSsid.collectAsState()

    // Collect toast messages and show them as snackbars
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeAccountSection(
                activatedAccount = activatedAccount,
                navController = navController,
                onSelectAccount = { showAccountListSheet.value = true }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "当前网络: ${currentSsid ?: "获取中/未连接"}",
                modifier = Modifier.padding(16.dp).clickable(
                    onClick = { viewModel.fetchSsid() }
                )
            )
            HomeLoginButton(viewModel = viewModel)
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAccountListSheet.value) {
        AccountListBottomSheet(
            accounts = accounts,
            sheetState = accountListSheetState,
            onDismiss = { showAccountListSheet.value = false },
            onSelectAccount = { account ->
                viewModel.activateAccount(account.id)
                showAccountListSheet.value = false
            },
            onEditAccount = { accountId ->
                showAccountListSheet.value = false
                navController.navigate(Routes.AccountEditor(id = accountId))
            },
            onDeleteAccount = { accountId -> viewModel.deleteAccount(accountId) }
        )
    }
}

@Composable
private fun HomeAccountSection(
    activatedAccount: Account?,
    navController: NavController,
    onSelectAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtleDeeperContainerColor = lerp(
        MaterialTheme.colorScheme.secondaryContainer,
        Color.Black,
        0.08f
    )

    Surface(
        modifier = modifier.padding(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeAccountHeader(
                subtleDeeperContainerColor = subtleDeeperContainerColor,
                onAddAccount = {
                    navController.navigate(Routes.AccountEditor(id = null))
                }
            )

            if (activatedAccount == null) {
                NoAccountPlaceholder()
            } else {
                AccountContent(
                    activatedAccount = activatedAccount,
                    subtleDeeperContainerColor = subtleDeeperContainerColor,
                    onSelectAccount = onSelectAccount
                )
            }
        }
    }
}

@Composable
private fun HomeAccountHeader(
    subtleDeeperContainerColor: Color,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
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
            onClick = onAddAccount,
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
}

@Composable
private fun NoAccountPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.no_account),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun AccountContent(
    activatedAccount: Account,
    subtleDeeperContainerColor: Color,
    onSelectAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccountSelectionButton(
            account = activatedAccount,
            subtleDeeperContainerColor = subtleDeeperContainerColor,
            onSelect = onSelectAccount
        )
        AccountDetailsCard(
            account = activatedAccount,
            subtleDeeperContainerColor = subtleDeeperContainerColor
        )
    }
}

@Composable
private fun AccountSelectionButton(
    account: Account,
    subtleDeeperContainerColor: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = subtleDeeperContainerColor,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(16.dp),
        onClick = onSelect,
        modifier = modifier.fillMaxWidth()
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
            Text(text = account.remark)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_unfold_more),
                contentDescription = stringResource(R.string.toggle_account)
            )
        }
    }
}

@Composable
private fun AccountDetailsCard(
    account: Account,
    subtleDeeperContainerColor: Color,
    modifier: Modifier = Modifier
) {
    val selectedOptionText =
        ServiceDisplay.entries.find { it.serviceType.code == account.service }
            ?: ServiceDisplay.EduNet

    Surface(
        color = subtleDeeperContainerColor,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountDetailRow(
                label = stringResource(R.string.student_id_label),
                value = account.username
            )
            AccountDetailRow(
                label = stringResource(R.string.service_label),
                value = stringResource(selectedOptionText.label)
            )
        }
    }
}

@Composable
private fun AccountDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HomeLoginButton(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            scope.launch {
                viewModel.startLogin()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(stringResource(R.string.login))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountListBottomSheet(
    accounts: List<Account>,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSelectAccount: (Account) -> Unit,
    onEditAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
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
                    AccountListItem(
                        account = account,
                        onSelect = { onSelectAccount(account) },
                        onEdit = { onEditAccount(account.id) },
                        onDelete = { onDeleteAccount(account.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountListItem(
    account: Account,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuExpanded = remember { mutableStateOf(false) }

    Card(
        onClick = onSelect,
        modifier = modifier
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
            AccountListItemMenu(
                menuExpanded = menuExpanded.value,
                onExpandedChange = { menuExpanded.value = it },
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun AccountListItemMenu(
    menuExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(
            onClick = {
                onExpandedChange(true)
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.more_action)
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_action)) },
                onClick = {
                    onExpandedChange(false)
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    onExpandedChange(false)
                    onDelete()
                }
            )
        }
    }
}
