package com.plansguru.ui.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.plansguru.ui.main.MainScreen
import com.plansguru.ui.welcome.WelcomeScreen

@Composable
fun NavGraph() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE) }
    var hasWelcomed by remember { mutableStateOf(prefs.getBoolean("welcomed", false)) }

    if (!hasWelcomed) {
        WelcomeScreen(
            onGetStarted = {
                hasWelcomed = true
            }
        )
    } else {
        MainScreen()
    }
}
