package com.lfwqsp2641.scunet_login.manager

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.os.IBinder
import android.util.Log
import com.lfwqsp2641.scunet_login.IUserService
import com.lfwqsp2641.scunet_login.service.ShizukuUserService
import com.lfwqsp2641.scunet_login.utils.hasShizukuPermission
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuManager {
    private const val TAG = "ShizukuManager"
    private const val PERMISSION_REQUEST_CODE = 10001
    private var userService: IUserService? = null
    private var isInitialized = false
    private var clientCount = 0
    private var listenersRegistered = false
    private var applicationContext: Context? = null
    private val _state = MutableStateFlow(ShizukuState.Unavailable)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()
    private val _permissionResults = MutableSharedFlow<ShizukuPermissionResult>(
        extraBufferCapacity = 1
    )
    val permissionResults: SharedFlow<ShizukuPermissionResult> = _permissionResults.asSharedFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userService = IUserService.Stub.asInterface(service)
            _state.value = ShizukuState.Ready
            Log.d(TAG, "UserService Connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            _state.value = ShizukuState.Binding
            Log.d(TAG, "UserService Disconnected")
        }
    }

    private lateinit var userServiceArgs: Shizuku.UserServiceArgs
    private var isDebuggable = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshState() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        userService = null
        _state.value = ShizukuState.Unavailable
    }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == PERMISSION_REQUEST_CODE) {
                _permissionResults.tryEmit(
                    if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        ShizukuPermissionResult.Granted
                    } else {
                        ShizukuPermissionResult.Denied
                    }
                )
                refreshState()
                applicationContext?.let { init(it) }
            }
        }

    fun acquire(context: Context) {
        clientCount++
        applicationContext = context.applicationContext
        registerListeners()
        init(context)
    }

    fun release() {
        clientCount = (clientCount - 1).coerceAtLeast(0)
        if (clientCount == 0) {
            unbind()
            unregisterListeners()
        }
    }

    fun init(context: Context) {
        registerListeners()
        if (isInitialized) return
        if (!hasShizukuPermission()) {
            refreshState()
            return
        }
        isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShizukuUserService::class.java.name)
        ).apply {
            processNameSuffix("privileged_service")
            debuggable(isDebuggable)
            version(1)
        }
        _state.value = ShizukuState.Binding
        bind()
        isInitialized = true
    }

    private fun bind() {
        if (userService != null) return
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Throwable) {
            Log.e(TAG, "Bind failed", e)
            _state.value = ShizukuState.Unavailable
        }
    }

    private fun unbind() {
        if (!isInitialized) return
        try {
            userService?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to destroy user service", e)
        }
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        userService = null
        isInitialized = false
        _state.value = ShizukuState.Unavailable
    }

    fun requestPermission() {
        if (hasShizukuPermission()) {
            refreshState()
            return
        }
        if (!Shizuku.pingBinder()) {
            _state.value = ShizukuState.Unavailable
            return
        }
        _state.value = ShizukuState.PermissionRequesting
        try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        } catch (e: Throwable) {
            Log.e(TAG, "Permission request failed", e)
            _state.value = ShizukuState.PermissionRequired
        }
    }

    fun refreshState() {
        _state.value = when {
            !Shizuku.pingBinder() -> ShizukuState.Unavailable
            !hasShizukuPermission() -> ShizukuState.PermissionRequired
            userService != null -> ShizukuState.Ready
            else -> ShizukuState.PermissionGranted
        }
        if (_state.value == ShizukuState.PermissionGranted && clientCount > 0) {
            applicationContext?.let { init(it) }
        }
    }

    private fun registerListeners() {
        if (listenersRegistered) return
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            listenersRegistered = true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register Shizuku listeners", e)
        }
    }

    private fun unregisterListeners() {
        if (!listenersRegistered) return
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            listenersRegistered = false
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to remove Shizuku listeners", e)
        }
    }

    fun getSsid(): String? = try {
        userService?.currentNetworkSsid
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get SSID via Shizuku", e)
        null
    }

    fun connectToOpenWifi(ssid: String): Boolean = try {
        userService?.connectToOpenWifi(ssid) ?: false
    } catch (e: Exception) {
        Log.e(TAG, "Failed to connect to open WiFi via Shizuku", e)
        false
    }

    fun disconnectWifi(): Boolean = try {
        userService?.disconnectWifi() ?: false
    } catch (e: Exception) {
        Log.e(TAG, "Failed to disconnect WiFi via Shizuku", e)
        false
    }

    fun grantPermission(packageName: String, permission: String) {
        try {
            userService?.grantPermission(packageName, permission)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to grant permission via Shizuku", e)
        }
    }

    fun wifiRevalidate(): Boolean = try {
        userService?.wifiRevalidate() ?: false
    } catch (e: Exception) {
        Log.e(TAG, "Failed to revalidate WiFi via Shizuku", e)
        false
    }
}

enum class ShizukuState {
    Unavailable,
    PermissionRequired,
    PermissionRequesting,
    PermissionGranted,
    Binding,
    Ready
}

enum class ShizukuPermissionResult {
    Granted,
    Denied
}
