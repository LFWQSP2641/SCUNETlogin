package com.lfwqsp2641.scunet_login.data.utils

import android.content.Context
import androidx.datastore.dataStore

val Context.configDataStore by dataStore(
    fileName = "config.json",
    serializer = ConfigSerializer
)