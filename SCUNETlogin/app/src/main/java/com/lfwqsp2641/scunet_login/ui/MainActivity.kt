package com.lfwqsp2641.scunet_login.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lfwqsp2641.scunet_login.manager.ShizukuManager
import com.lfwqsp2641.scunet_login.ui.screens.RootScreen
import com.lfwqsp2641.scunet_login.ui.theme.SCUNETloginTheme
import com.lfwqsp2641.scunet_login.utils.hasShizukuPermission
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {
    companion object {
        private const val SHIZUKU_PERM = 10001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SCUNETloginTheme {
                RootScreen()
            }
        }
        if (hasShizukuPermission()) {
            ShizukuManager.init(applicationContext)
        } else if (Shizuku.pingBinder()) {
            requestShizukuPermission()
        }
    }

    private fun requestShizukuPermission() {
        Shizuku.addRequestPermissionResultListener(this)
        Shizuku.requestPermission(SHIZUKU_PERM)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == SHIZUKU_PERM) {
            Shizuku.removeRequestPermissionResultListener(this)
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                ShizukuManager.init(applicationContext)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
        ShizukuManager.unbind()
    }
}