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
import rikka.shizuku.Shizuku

object ShizukuManager {
    private const val TAG = "ShizukuManager"
    private var userService: IUserService? = null
    private var isInitialized = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userService = IUserService.Stub.asInterface(service)
            Log.d(TAG, "UserService Connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            Log.d(TAG, "UserService Disconnected")
        }
    }
    private lateinit var userServiceArgs: Shizuku.UserServiceArgs
    private var isDebuggable: Boolean = false

    fun init(context: Context) {
        if (isInitialized) {
            return
        }
        if (!hasShizukuPermission()) {
            Log.e(TAG, "Shizuku permission is not granted or Shizuku is not running")
            return
        }
        isDebuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShizukuUserService::class.java.name)
        ).apply {
            processNameSuffix("privileged_service")
            debuggable(isDebuggable)
            version(1)
        }
        bind()
        isInitialized = true
    }

    fun bind() {
        if (userService != null) return
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Throwable) {
            Log.e(TAG, "Bind failed", e)
        }
    }

    fun unbind() {
        if (!isInitialized) {
            return
        }
        try {
            userService?.destroy()
        } catch (e: Exception) {
        }
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        userService = null
        isInitialized = false
    }

    fun getSsid(): String? {
        return try {
            userService?.currentNetworkSsid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SSID via Shizuku", e)
            "IPC Error: ${e.message}"
        }
    }

    fun connectToOpenWifi(ssid: String): Boolean {
        return try {
            userService?.connectToOpenWifi(ssid) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to open WiFi via Shizuku", e)
            false
        }
    }

    fun disconnectWifi(): Boolean {
        return try {
            userService?.disconnectWifi() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect WiFi via Shizuku", e)
            false
        }
    }

    fun grantPermission(packageName: String, permission: String) {
        try {
            userService?.grantPermission(packageName, permission)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to grant permission via Shizuku", e)
        }
    }
}