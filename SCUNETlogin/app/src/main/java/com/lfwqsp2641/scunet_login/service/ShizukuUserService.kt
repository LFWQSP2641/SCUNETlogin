package com.lfwqsp2641.scunet_login.service

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.os.IBinder
import com.lfwqsp2641.scunet_login.IUserService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

@SuppressLint("PrivateApi", "BlockedPrivateApi")
class ShizukuUserService : IUserService.Stub() {
    init {
        HiddenApiBypass.addHiddenApiExemptions(
            "Landroid/os/ServiceManager;",
            "Landroid/net/wifi/IWifiManager;",
            "Landroid/net/wifi/WifiConfiguration;",
            "Landroid/permission/IPermissionManager;"
        )
    }

    private val getServiceMethod by lazy {
        val serviceManagerClass = Class.forName("android.os.ServiceManager")
        serviceManagerClass.getDeclaredMethod("getService", String::class.java)
    }

    private fun getService(name: String): IBinder? {
        return try {
            getServiceMethod.invoke(null, name) as? IBinder
        } catch (_: Throwable) {
            null
        }
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun grantPermission(packageName: String, permission: String) {
        try {
            val pmBinder = getService("permissionmgr") ?: return
            val pmStubClass = Class.forName($$"android.permission.IPermissionManager$Stub")
            val asInterface = pmStubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            val pmService = asInterface.invoke(null, pmBinder) ?: return

            val grantMethod = pmService.javaClass.methods.firstOrNull {
                it.name == "grantRuntimePermission" && it.parameterTypes.size == 3
            } ?: return

            grantMethod.invoke(pmService, packageName, permission, 0)
        } catch (_: Throwable) {
            // Ignore when hidden API signatures vary across ROMs.
        }
    }

    private val wifiService: Any? by lazy {
        try {
            val wifiBinder = getService(Context.WIFI_SERVICE) ?: return@lazy null
            val wifiStubClass = Class.forName($$"android.net.wifi.IWifiManager$Stub")
            val asInterface = wifiStubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterface.invoke(null, wifiBinder)
        } catch (_: Throwable) {
            null
        }
    }

    private fun setAllowAutojoinIfPresent(config: WifiConfiguration) {
        try {
            val field = WifiConfiguration::class.java.getDeclaredField("allowAutojoin")
            field.isAccessible = true

            when (field.type) {
                Boolean::class.javaPrimitiveType -> {
                    field.setBoolean(config, false)
                }

                Boolean::class.java -> {
                    field.set(config, false)
                }

                else -> {
                    field.set(config, false)
                }
            }
        } catch (_: Throwable) {
            // allowAutojoin is hidden/absent on older APIs and ROMs; ignore and continue.
        }
    }

    override fun getCurrentNetworkSsid(): String {
        return try {
            val service = wifiService ?: return "Unknown (wifiService is null)"

            // In API 29+, getConnectionInfo() requires `String callingPackage` 
            // In API 30+, it optionally requires `String callingFeatureId` too
            val getConnectionInfoMethod =
                service.javaClass.methods.firstOrNull { it.name == "getConnectionInfo" }
                    ?: return "Unknown (getConnectionInfo not found)"

            val args = getConnectionInfoMethod.parameterTypes.map { type ->
                when (type) {
                    String::class.java -> "com.android.shell" // Emulate shell's calling package
                    Int::class.java -> 0
                    else -> null
                }
            }.toTypedArray<Any?>()

            val connectionInfo = getConnectionInfoMethod.invoke(service, *args)
            val ssid =
                connectionInfo?.javaClass?.getMethod("getSSID")?.invoke(connectionInfo) as? String

            ssid?.replace("\"", "") ?: "Unknown (SSID is null)"
        } catch (e: Throwable) {
            "Unknown (Error: ${e.message})"
        }
    }

    override fun connectToOpenWifi(ssid: String): Boolean {
        if (ssid.isEmpty()) return false

        return try {
            val service = wifiService ?: return false
            val connectMethod = service.javaClass.methods.firstOrNull {
                it.name == "connect" &&
                        it.parameterTypes.firstOrNull() == WifiConfiguration::class.java
            } ?: return false

            val config = WifiConfiguration()
            config.SSID = String.format("\"%s\"", ssid)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            setAllowAutojoinIfPresent(config)

            val args = connectMethod.parameterTypes.mapIndexed { index, type ->
                when {
                    index == 0 -> config
                    index == 1 -> -1 // networkId = WifiConfiguration.INVALID_NETWORK_ID
                    type == IBinder::class.java -> android.os.Binder()
                    type == String::class.java -> "com.android.shell" // callingPackage / callingFeatureId
                    type == Int::class.java -> 0 // callbackIdentifier
                    else -> null // IActionListener etc.
                }
            }.toTypedArray<Any?>()

            connectMethod.invoke(service, *args)
            true
        } catch (_: Throwable) {
            false
        }
    }

    override fun disconnectWifi(): Boolean {
        return try {
            val service = wifiService ?: return false
            val disconnectMethod =
                service.javaClass.methods.firstOrNull { it.name == "disconnect" } ?: return false

            val args = disconnectMethod.parameterTypes.map { type ->
                when (type) {
                    String::class.java -> "com.android.shell"
                    Int::class.java -> 0
                    else -> null
                }
            }.toTypedArray<Any?>()

            disconnectMethod.invoke(service, *args)
            true
        } catch (_: Throwable) {
            false
        }
    }
}