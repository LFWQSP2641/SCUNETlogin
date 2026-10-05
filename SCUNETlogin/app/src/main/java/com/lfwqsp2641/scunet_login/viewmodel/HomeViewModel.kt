package com.lfwqsp2641.scunet_login.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.data.dto.Account
import com.lfwqsp2641.scunet_login.data.dto.Config
import com.lfwqsp2641.scunet_login.data.model.TaskLog
import com.lfwqsp2641.scunet_login.data.utils.configDataStore
import com.lfwqsp2641.scunet_login.manager.ShizukuManager
import com.lfwqsp2641.scunet_login.manager.TaskLogging
import com.lfwqsp2641.scunet_login.service.LoginException
import com.lfwqsp2641.scunet_login.service.LoginService
import com.lfwqsp2641.scunet_login.utils.hasShizukuPermission
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.configDataStore

    private val _currentSsid = MutableStateFlow<String?>(null)
    val currentSsid: StateFlow<String?> = _currentSsid.asStateFlow()

    private val _isShizukuEnabled = MutableStateFlow(false)
    val isShizukuEnabled: StateFlow<Boolean> = _isShizukuEnabled.asStateFlow()

    private val _isAccessLocalNetworkPermissionGrantedFlow = MutableStateFlow(false)
    val isAccessLocalNetworkPermissionGrantedFlow = _isAccessLocalNetworkPermissionGrantedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
        fetchShizukuStatus()
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

    fun startLogin(autoManageNetworkChecked: Boolean) {
        viewModelScope.launch {
            val data = dataStore.data.firstOrNull()
            if (data == null || data.accounts.isEmpty()) {
                TaskLogging.addLog("No account is activated", TaskLog.LogLevel.WARN)
                _toastMessage.emit(getApplication<Application>().getString(R.string.no_account_activated))
                return@launch
            }

            val currentAccount = if (data.activatedId != null) {
                data.accounts.firstOrNull { it.id == data.activatedId }
            } else {
                data.accounts.firstOrNull()
            }

            if (currentAccount == null) {
                TaskLogging.addLog("No account is activated", TaskLog.LogLevel.WARN)
                _toastMessage.emit(getApplication<Application>().getString(R.string.no_account_activated))
                return@launch
            }

            val autoManageNetwork = autoManageNetworkChecked && hasShizukuPermission()

            var targetWifiNetwork: Network? = null
            val appContext = getApplication<Application>()
            val connectivityManager =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            try {
                if (autoManageNetwork) {
                    ShizukuManager.connectToOpenWifi("SCUNET")
                    // Delay, wait for network connection
                    kotlinx.coroutines.delay(1000.milliseconds)
                    for (attempt in 1..5) {
                        val ssid = ShizukuManager.getSsid()
                        if (ssid == "SCUNET") {
                            break
                        }
                        if (attempt < 5) {
                            kotlinx.coroutines.delay(500.milliseconds)
                        }
                    }
                    val ssid = ShizukuManager.getSsid()
                    TaskLogging.addLog("Current SSID: $ssid", TaskLog.LogLevel.INFO)
                    val network = tryGetWifiNetwork(connectivityManager)
                    if (network != null) {
                        targetWifiNetwork = network
                    }
                    // update ui
                    _currentSsid.value = ssid
                }
                // Try setting socketFactory to wifi
                val customSocketFactory = targetWifiNetwork?.socketFactory

                if (customSocketFactory != null) {
                    TaskLogging.addLog("WiFi socketFactory acquired", TaskLog.LogLevel.INFO)
                } else {
                    TaskLogging.addLog(
                        "Failed to acquire WiFi socketFactory, fallback to default network",
                        TaskLog.LogLevel.WARN
                    )
                }

                val loginService = LoginService(customSocketFactory)

                try {
                    loginService.startLogin(currentAccount)
                } catch (_: LoginException.AlreadyLoggedIn) {
                    // Ignored
                }
                TaskLogging.addLog("Try reportNetworkConnectivity", TaskLog.LogLevel.INFO)
                val networkToReport = targetWifiNetwork ?: connectivityManager.activeNetwork
                connectivityManager.reportNetworkConnectivity(networkToReport, true)
                if (autoManageNetwork) {
                    TaskLogging.addLog("Try shizuku wifiRevalidate", TaskLog.LogLevel.INFO)
                    val wifiRevalidateResult = ShizukuManager.wifiRevalidate()
                    if (!wifiRevalidateResult) {
                        TaskLogging.addLog("wifiRevalidate failed, try reconnecting", TaskLog.LogLevel.WARN)
                        ShizukuManager.disconnectWifi()
                        ShizukuManager.connectToOpenWifi("SCUNET")
                    }
                }
                TaskLogging.addLog(
                    "Login successful for account: ${currentAccount.username}",
                    TaskLog.LogLevel.SUCCESS
                )
                _toastMessage.emit(getApplication<Application>().getString(R.string.login_success))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // failed
                if (autoManageNetwork) {
                    try {
                        ShizukuManager.disconnectWifi()
                    } catch (_: Exception) {
                        // ignored
                    }
                }
                TaskLogging.addLog(
                    "Login failed for account: ${currentAccount.username}",
                    TaskLog.LogLevel.ERROR
                )
                TaskLogging.addLog("Error details: ${e.message}", TaskLog.LogLevel.ERROR)
                _toastMessage.emit(getApplication<Application>().getString(R.string.login_failed))
            }
            // delay, and update ui
            kotlinx.coroutines.delay(1000.milliseconds)
            fetchSsid()
        }
    }

    private suspend fun tryGetWifiNetwork(
        connectivityManager: ConnectivityManager,
        timeoutMillis: Long = 3000L,
    ): Network? {
        val wifiRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        return withTimeoutOrNull(timeoutMillis.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean(false)

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        completeOnce(network)
                    }

                    override fun onUnavailable() {
                        completeOnce(null)
                    }

                    private fun completeOnce(result: Network?) {
                        if (completed.compareAndSet(false, true)) {
                            runCatching { connectivityManager.unregisterNetworkCallback(this) }
                            if (continuation.isActive) {
                                continuation.resume(result)
                            }
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    if (completed.compareAndSet(false, true)) {
                        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                    }
                }

                try {
                    connectivityManager.registerNetworkCallback(wifiRequest, callback)
                } catch (_: Exception) {
                    if (completed.compareAndSet(false, true)) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }
        }
    }

    fun startLogout(autoManageNetworkChecked: Boolean) {
        viewModelScope.launch {
            val appContext = getApplication<Application>()
            val connectivityManager =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiNetwork = tryGetWifiNetwork(connectivityManager)
            // Try setting socketFactory to wifi
            val customSocketFactory = wifiNetwork?.socketFactory

            if (customSocketFactory != null) {
                TaskLogging.addLog("WiFi socketFactory acquired", TaskLog.LogLevel.INFO)
            } else {
                TaskLogging.addLog(
                    "Failed to acquire WiFi socketFactory, fallback to default network",
                    TaskLog.LogLevel.WARN
                )
            }

            val loginService = LoginService(customSocketFactory)

            try {
                loginService.startLogout()
                TaskLogging.addLog("Logout successful", TaskLog.LogLevel.SUCCESS)

                val autoManageNetwork = autoManageNetworkChecked && hasShizukuPermission()

                if (autoManageNetwork) {
                    ShizukuManager.disconnectWifi()
                    TaskLogging.addLog("Disconnected from SCUNET WiFi", TaskLog.LogLevel.INFO)
                }

                _toastMessage.emit(getApplication<Application>().getString(R.string.logout_success))
            } catch (e: Exception) {
                TaskLogging.addLog("Logout failed: ${e.message}", TaskLog.LogLevel.ERROR)
                _toastMessage.emit(getApplication<Application>().getString(R.string.logout_failed))
            }
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
                kotlinx.coroutines.delay(1000.milliseconds)
            }
        }
    }

    fun fetchShizukuStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            repeat(5) {
                val isEnabled = hasShizukuPermission()
                _isShizukuEnabled.value = isEnabled
                if (isEnabled) {
                    return@launch
                }
                kotlinx.coroutines.delay(1000.milliseconds)
            }
        }
    }

    fun checkAccessLocalNetworkPermission() {
        viewModelScope.launch {
            val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                ContextCompat.checkSelfPermission(
                    getApplication(), Manifest.permission.ACCESS_LOCAL_NETWORK
                ) == PackageManager.PERMISSION_GRANTED
            } else true
            _isAccessLocalNetworkPermissionGrantedFlow.value = granted
        }
    }

    fun setAccessLocalNetworkPermissionGranted(isGranted: Boolean) {
        _isAccessLocalNetworkPermissionGrantedFlow.value = isGranted
    }

    fun grantPermissionsByShizuku(permissions: List<String>) {
        viewModelScope.launch {
            if (!hasShizukuPermission()) {
                return@launch
            }
            for (permission in permissions) {
                ShizukuManager.grantPermission("com.lfwqsp2641.scunet_login", permission)
            }
        }
    }
}