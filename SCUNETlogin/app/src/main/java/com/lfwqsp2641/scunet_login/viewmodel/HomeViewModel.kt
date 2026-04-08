package com.lfwqsp2641.scunet_login.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.data.dto.Config
import com.lfwqsp2641.scunet_login.data.dto.Account
import com.lfwqsp2641.scunet_login.data.model.TaskLog
import com.lfwqsp2641.scunet_login.data.utils.configDataStore
import com.lfwqsp2641.scunet_login.manager.ShizukuManager
import com.lfwqsp2641.scunet_login.manager.TaskLogging
import com.lfwqsp2641.scunet_login.service.LoginService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.configDataStore

    private val _currentSsid = MutableStateFlow<String?>(null)
    val currentSsid: StateFlow<String?> = _currentSsid.asStateFlow()

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

    private val _toastMessage = MutableSharedFlow<String>(replay = 0)
    val toastMessage = _toastMessage.asSharedFlow()


    init {
        fetchSsid()
    }


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

    suspend fun startLogin() {
        val data = dataStore.data.firstOrNull()
        if (data == null || data.accounts.isEmpty()) {
            TaskLogging.addLog("No account is activated", TaskLog.LogLevel.WARN)
            _toastMessage.emit(getApplication<Application>().getString(R.string.no_account_activated))
            return
        }

        val currentAccount = if (data.activatedId != null) {
            data.accounts.firstOrNull { it.id == data.activatedId }
        } else {
            data.accounts.firstOrNull()
        }

        if (currentAccount == null) {
            TaskLogging.addLog("No account is activated", TaskLog.LogLevel.WARN)
            _toastMessage.emit(getApplication<Application>().getString(R.string.no_account_activated))
            return
        }

        val loginService = LoginService()
        try {
            loginService.startLogin(currentAccount)
            TaskLogging.addLog(
                "Login successful for account: ${currentAccount.username}",
                TaskLog.LogLevel.SUCCESS
            )
            _toastMessage.emit(getApplication<Application>().getString(R.string.login_success))
        } catch (e: Exception) {
            TaskLogging.addLog(
                "Login failed for account: ${currentAccount.username}",
                TaskLog.LogLevel.ERROR
            )
            TaskLogging.addLog("Error details: ${e.message}", TaskLog.LogLevel.ERROR)
            _toastMessage.emit(getApplication<Application>().getString(R.string.login_failed))
        }
    }

    fun fetchSsid() {
        viewModelScope.launch(Dispatchers.IO) {
            repeat(5) {
                val ssid = ShizukuManager.getSsid()
                _currentSsid.value = ssid
                if (ssid != null) {
                    return@launch
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}