package com.lfwqsp2641.scunet_login.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lfwqsp2641.scunet_login.data.dto.Config
import com.lfwqsp2641.scunet_login.data.dto.Account
import com.lfwqsp2641.scunet_login.data.utils.configDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.configDataStore

    private val configState: StateFlow<Config> = dataStore.data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Config()
        )

    val accountsState: StateFlow<List<Account>> = configState
        .map { it.accounts }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activatedIdState: StateFlow<String?> = configState
        .map { it.activatedId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun activateAccount(accountId: String) {
        viewModelScope.launch {
            dataStore.updateData { currentConfig ->
                val accounts = currentConfig.accounts
                val index = accounts.indexOfFirst { it.id == accountId }
                if (index == -1) {
                    return@updateData currentConfig
                }

                val reorderedAccounts = accounts.toMutableList().apply {
                    add(0, removeAt(index))
                }

                currentConfig.copy(
                    accounts = reorderedAccounts,
                    activatedId = accountId
                )
            }
        }
    }


    @Suppress("unused")
    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            dataStore.updateData { currentConfig ->
                val updatedAccounts = currentConfig.accounts.filterNot { it.id == accountId }
                val updatedActivatedId = when {
                    currentConfig.activatedId == accountId -> updatedAccounts.firstOrNull()?.id
                    currentConfig.activatedId != null &&
                            updatedAccounts.none { it.id == currentConfig.activatedId } -> updatedAccounts.firstOrNull()?.id

                    else -> currentConfig.activatedId
                }

                currentConfig.copy(
                    accounts = updatedAccounts,
                    activatedId = updatedActivatedId
                )
            }
        }
    }
}