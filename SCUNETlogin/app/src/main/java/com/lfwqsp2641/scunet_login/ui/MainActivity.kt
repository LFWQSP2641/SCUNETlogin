package com.lfwqsp2641.scunet_login.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lfwqsp2641.scunet_login.ui.screens.SCUNETloginApp
import com.lfwqsp2641.scunet_login.ui.theme.SCUNETloginTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SCUNETloginTheme {
                SCUNETloginApp()
            }
        }
    }
}