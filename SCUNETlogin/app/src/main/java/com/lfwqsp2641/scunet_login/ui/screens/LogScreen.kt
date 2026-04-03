package com.lfwqsp2641.scunet_login.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lfwqsp2641.scunet_login.R

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.log),
        modifier = modifier
    )
}
