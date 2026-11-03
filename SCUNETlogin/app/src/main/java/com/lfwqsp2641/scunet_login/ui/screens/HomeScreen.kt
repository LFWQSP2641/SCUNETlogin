package com.lfwqsp2641.scunet_login.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.data.dto.Account
import com.lfwqsp2641.scunet_login.ui.Routes
import com.lfwqsp2641.scunet_login.ui.model.ServiceDisplay
import com.lfwqsp2641.scunet_login.viewmodel.HomeViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    val viewModel: HomeViewModel = viewModel()
    val accounts by viewModel.accountsState.collectAsStateWithLifecycle()
    val activatedId by viewModel.activatedIdState.collectAsStateWithLifecycle()
    val activatedAccount = accounts.firstOrNull { it.id == activatedId } ?: accounts.firstOrNull()

    var accountListBottomSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentSsid by viewModel.currentSsid.collectAsStateWithLifecycle()
    val networkStatus = currentSsid ?: stringResource(R.string.network_fetching)

    val isShizukuEnabled by viewModel.isShizukuEnabled.collectAsStateWithLifecycle()
    var autoManageNetworkChecked by rememberSaveable { mutableStateOf(isShizukuEnabled) }

    // Update autoManageNetworkChecked when isShizukuEnabled changes
    LaunchedEffect(isShizukuEnabled) {
        autoManageNetworkChecked = isShizukuEnabled
    }

    // Collect toast messages and show them as snackbars
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(state = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HomeAccountSection(
                activatedAccount = activatedAccount,
                navController = navController,
                onSelectAccount = { accountListBottomSheet = true }
            )
            Spacer(modifier = Modifier.weight(1f))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.current_network, networkStatus),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        modifier = Modifier.clickable(onClick = { viewModel.fetchSsid() })
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = autoManageNetworkChecked,
                                role = Role.Checkbox,
                                onValueChange = { autoManageNetworkChecked = it }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            state = ToggleableState(autoManageNetworkChecked),
                            onClick = null
                        )
                        Text(
                            text = stringResource(R.string.auto_manage_network_shizuku),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceContainer
                        )
                    }
                }
            }
            HomeLoginButton(
                viewModel = viewModel,
                autoManageNetworkChecked = autoManageNetworkChecked
            )
        }
        OverlayBottomSheet(
            show = accountListBottomSheet,
            onDismissRequest = { accountListBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.account),
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
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
                            onSelect = {
                                viewModel.activateAccount(account.id)
                                accountListBottomSheet = false
                            },
                            onEdit = {
                                accountListBottomSheet = false
                                navController.push(Routes.AccountEditor(id = account.id))
                            },
                            onDelete = { viewModel.deleteAccount(account.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAccountSection(
    activatedAccount: Account?,
    navController: NavController,
    onSelectAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeAccountHeader(
                onAddAccount = {
                    navController.push(Routes.AccountEditor(id = null))
                }
            )

            if (activatedAccount == null) {
                NoAccountPlaceholder()
            } else {
                AccountContent(
                    activatedAccount = activatedAccount,
                    onSelectAccount = onSelectAccount
                )
            }
        }
    }
}

@Composable
private fun HomeAccountHeader(
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_description),
            contentDescription = stringResource(R.string.account),
            tint = MiuixTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.account),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            fontWeight = FontWeight.Bold,
            style = MiuixTheme.textStyles.title2,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onAddAccount,
            cornerRadius = 12.dp,
            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.add_account),
                tint = MiuixTheme.colorScheme.primary,
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
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
    }
}

@Composable
private fun AccountContent(
    activatedAccount: Account,
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
            onSelect = onSelectAccount
        )
        AccountDetailsCard(
            account = activatedAccount
        )
    }
}

@Composable
private fun AccountSelectionButton(
    account: Account,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MiuixTheme.colorScheme.surfaceContainerHigh,
        contentColor = MiuixTheme.colorScheme.onSurfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        onClick = onSelect,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_draft),
                contentDescription = stringResource(R.string.configuration),
                tint = MiuixTheme.colorScheme.primary
            )
            Text(
                text = account.remark,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_unfold_more),
                contentDescription = stringResource(R.string.toggle_account),
                tint = MiuixTheme.colorScheme.onSurfaceSecondary
            )
        }
    }
}

@Composable
private fun AccountDetailsCard(
    account: Account,
    modifier: Modifier = Modifier
) {
    val selectedOptionText =
        ServiceDisplay.entries.find { it.serviceType.code == account.service }
            ?: ServiceDisplay.EduNet

    Surface(
        color = MiuixTheme.colorScheme.surfaceContainerHigh,
        contentColor = MiuixTheme.colorScheme.onSurfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurfaceContainerHigh
        )
    }
}

@Composable
private fun HomeLoginButton(
    viewModel: HomeViewModel,
    autoManageNetworkChecked: Boolean,
    modifier: Modifier = Modifier
) {
    val isAccessLocalNetworkGranted by viewModel.isAccessLocalNetworkPermissionGrantedFlow.collectAsStateWithLifecycle()
    val isShizukuEnabled by viewModel.isShizukuEnabled.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            permissionsMap[Manifest.permission.ACCESS_LOCAL_NETWORK]
        } else {
            true
        }?.let { viewModel.setAccessLocalNetworkPermissionGranted(it) }
    }

    val entry = DropdownEntry(
        items = listOf(
            DropdownItem(
                text = stringResource(R.string.logout),
                onClick = {
                    if (!isAccessLocalNetworkGranted
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
                    ) {
                        val permissions = listOf(Manifest.permission.ACCESS_LOCAL_NETWORK)
                        if (isShizukuEnabled) {
                            viewModel.grantPermissionsByShizuku(permissions)
                            return@DropdownItem
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                        return@DropdownItem
                    }
                    viewModel.startLogout(autoManageNetworkChecked)
                }
            )
        )
    )

    LifecycleResumeEffect(Unit) {
        viewModel.checkAccessLocalNetworkPermission()
        onPauseOrDispose {}
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                if (!isAccessLocalNetworkGranted
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
                ) {
                    val permissions = listOf(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    if (isShizukuEnabled) {
                        viewModel.grantPermissionsByShizuku(permissions)
                        return@Button
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                    return@Button
                }
                viewModel.startLogin(autoManageNetworkChecked)
            },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            cornerRadius = 16.dp
        ) {
            Text(stringResource(R.string.login))
        }
        OverlayIconDropdownMenu(
            entry = entry,
            modifier = Modifier
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.more_action),
                tint = MiuixTheme.colorScheme.onSurface
            )
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
    Card(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_draft),
                contentDescription = stringResource(R.string.configuration),
                tint = MiuixTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.remark,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = "${account.service} - ${account.username}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
            AccountListItemMenu(
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun AccountListItemMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entry = DropdownEntry(
        items = listOf(
            DropdownItem(
                text = stringResource(R.string.edit_action),
                onClick = onEdit
            ),
            DropdownItem(
                text = stringResource(R.string.delete),
                onClick = onDelete
            )
        )
    )

    OverlayIconDropdownMenu(
        entry = entry,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.more_action),
            tint = MiuixTheme.colorScheme.onSurfaceSecondary
        )
    }
}
