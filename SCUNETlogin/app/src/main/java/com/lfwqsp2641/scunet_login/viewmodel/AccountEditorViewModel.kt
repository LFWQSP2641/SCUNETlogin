package com.lfwqsp2641.scunet_login.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lfwqsp2641.scunet_login.data.dto.Account
import com.lfwqsp2641.scunet_login.data.enums.ServiceType
import com.lfwqsp2641.scunet_login.data.utils.configDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class AccountEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.configDataStore

    private val _uiState =
        MutableStateFlow(Account("", "", "", "", ServiceType.EduNet.code))
    val uiState: StateFlow<Account> = _uiState.asStateFlow()

    private val _fieldErrors = MutableStateFlow(AccountFieldErrors())
    val fieldErrors = _fieldErrors.asStateFlow()

    private val _canSave = MutableStateFlow(false)
    val canSave = _canSave.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    private val touchedFields = mutableSetOf<AccountField>()
    private var submitAttempted = false

    private fun computeErrors(account: Account): AccountFieldErrors {
        return AccountFieldErrors(
            remarkError = if (account.remark.isBlank()) "备注不能为空" else null,
            serviceError = if (account.service.isBlank()) "请选择服务" else null,
            usernameError = if (account.username.isBlank()) "用户名不能为空" else null,
            passwordError = if (account.password.isBlank()) "密码不能为空" else null
        )
    }

    private fun refreshValidation() {
        val allErrors = computeErrors(_uiState.value)
        _canSave.value = !allErrors.hasErrors
        _fieldErrors.value = allErrors.visibleFor(touchedFields, submitAttempted)
    }

    fun onFieldChange(updated: Account, touchedField: AccountField? = null) {
        _uiState.value = updated
        if (touchedField != null) touchedFields.add(touchedField)
        refreshValidation()
    }

    fun loadAccount(id: String) {
        viewModelScope.launch {
            dataStore.data.firstOrNull()?.accounts?.find { it.id == id }?.let { found ->
                _uiState.value = found
                touchedFields.clear()
                submitAttempted = false
                refreshValidation()
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            submitAttempted = true
            refreshValidation()
            if (!_canSave.value) return@launch

            val currentAccount = _uiState.value
            dataStore.updateData { config ->
                val newList = config.accounts.toMutableList()
                val index = newList.indexOfFirst { it.id == currentAccount.id }

                if (index != -1) {
                    newList[index] = currentAccount
                } else {
                    newList.add(currentAccount.copy(id = UUID.randomUUID().toString()))
                }
                config.copy(accounts = newList)
            }
            _saveSuccess.emit(Unit)
        }
    }
}

enum class AccountField {
    Remark,
    Service,
    Username,
    Password
}

data class AccountFieldErrors(
    val remarkError: String? = null,
    val serviceError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
) {
    val hasErrors: Boolean
        get() = remarkError != null || serviceError != null || usernameError != null || passwordError != null

    fun visibleFor(touched: Set<AccountField>, submitAttempted: Boolean): AccountFieldErrors {
        val showRemark = submitAttempted || AccountField.Remark in touched
        val showService = submitAttempted || AccountField.Service in touched
        val showUsername = submitAttempted || AccountField.Username in touched
        val showPassword = submitAttempted || AccountField.Password in touched

        return AccountFieldErrors(
            remarkError = if (showRemark) remarkError else null,
            serviceError = if (showService) serviceError else null,
            usernameError = if (showUsername) usernameError else null,
            passwordError = if (showPassword) passwordError else null
        )
    }
}