package com.lfwqsp2641.scunet_login.utils

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

fun hasShizukuPermission(): Boolean {
    if (!Shizuku.pingBinder()) {
        return false
    }
    return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
}
