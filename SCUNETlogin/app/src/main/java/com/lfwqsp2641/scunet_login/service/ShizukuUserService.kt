package com.lfwqsp2641.scunet_login.service

import android.annotation.SuppressLint
import android.content.Context
import android.net.Network
import android.net.NetworkCapabilities
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
            "Landroid/permission/IPermissionManager;",
            "Landroid/net/IConnectivityManager;",
            "Landroid/net/NetworkCapabilities;",
            "Landroid/net/Network;",
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

    private val connectivityService: Any? by lazy {
        try {
            val connectivityBinder = getService(Context.CONNECTIVITY_SERVICE) ?: return@lazy null
            val connectivityStubClass = Class.forName($$"android.net.IConnectivityManager$Stub")
            val asInterface = connectivityStubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterface.invoke(null, connectivityBinder)
        } catch (_: Throwable) {
            null
        }
    }

    override fun wifiRevalidate(): Boolean {
        return try {
            val service = connectivityService ?: return false
            val clazz = service.javaClass

            val getAllNetworks = clazz.methods.firstOrNull {
                it.name == "getAllNetworks" && it.parameterTypes.isEmpty()
            } ?: return false

            @Suppress("UNCHECKED_CAST")
            val networks = getAllNetworks.invoke(service) as? Array<Network>
            if (networks.isNullOrEmpty()) return false

            val getCaps = clazz.methods
                .filter { m ->
                    m.name == "getNetworkCapabilities" &&
                            m.parameterTypes.isNotEmpty() &&
                            m.parameterTypes[0].name == "android.net.Network"
                }
                .minByOrNull { it.parameterTypes.size }
                ?: return false

            fun capsOf(network: Network): Any? = try {
                val types = getCaps.parameterTypes
                val args = Array(types.size) { i ->
                    when {
                        i == 0 -> network
                        types[i] == String::class.java ->
                            if (i == 1) "com.android.shell" else ""
                        else -> null
                    }
                }
                getCaps.invoke(service, *args)
            } catch (_: Throwable) {
                null
            }

            var hasTransport: java.lang.reflect.Method? = null
            var hasCapability: java.lang.reflect.Method? = null

            fun ensureNcMethods(caps: Any): Boolean {
                if (hasTransport == null) {
                    hasTransport = caps.javaClass.methods.firstOrNull { m ->
                        m.name == "hasTransport" &&
                                m.parameterTypes.size == 1 &&
                                m.parameterTypes[0] == Int::class.javaPrimitiveType
                    }
                }
                if (hasCapability == null) {
                    hasCapability = caps.javaClass.methods.firstOrNull { m ->
                        m.name == "hasCapability" &&
                                m.parameterTypes.size == 1 &&
                                m.parameterTypes[0] == Int::class.javaPrimitiveType
                    }
                }
                return hasTransport != null && hasCapability != null
            }

            fun isWifi(caps: Any): Boolean {
                if (!ensureNcMethods(caps)) return false
                return hasTransport!!.invoke(
                    caps,
                    NetworkCapabilities.TRANSPORT_WIFI
                ) == true
            }

            fun isValidated(caps: Any): Boolean {
                if (!ensureNcMethods(caps)) return false
                return hasCapability!!.invoke(
                    caps,
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                ) == true
            }

            data class WifiCandidate(val network: Network, val validated: Boolean)

            val candidates = networks.mapNotNull { n ->
                val caps = capsOf(n) ?: return@mapNotNull null
                if (!isWifi(caps)) return@mapNotNull null
                WifiCandidate(n, isValidated(caps))
            }
            if (candidates.isEmpty()) return false

            val wifiNetwork =
                (candidates.firstOrNull { !it.validated } ?: candidates.first()).network

            val report = clazz.methods
                .filter { m ->
                    m.name == "reportNetworkConnectivity" &&
                            m.parameterTypes.size >= 2 &&
                            m.parameterTypes[0].name == "android.net.Network" &&
                            m.parameterTypes[1] == Boolean::class.javaPrimitiveType
                }
                .minByOrNull { it.parameterTypes.size }
                ?: return false

            val reportArgs = Array(report.parameterTypes.size) { i ->
                when (i) {
                    0 -> wifiNetwork
                    1 -> true
                    else -> if (report.parameterTypes[i] == String::class.java) "" else null
                }
            }
            report.invoke(service, *reportArgs)
            true
        } catch (_: Throwable) {
            false
        }
    }
}