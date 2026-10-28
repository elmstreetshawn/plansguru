package com.plansguru.ui.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.plansguru.agent.GuruChatEngine
import com.plansguru.ui.main.MainScreen
import com.plansguru.ui.welcome.ModelDownloadScreen
import com.plansguru.ui.welcome.WelcomeScreen

@Composable
fun NavGraph() {
    val context = LocalContext.current
    var isModelReady by remember { mutableStateOf(GuruChatEngine.isModelReady(context)) }

    if (!isModelReady) {
        ModelDownloadScreen(
            onModelReady = {
                isModelReady = true
            }
        )
    } else {
        val prefs = remember { context.getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE) }
        var hasWelcomed by remember { mutableStateOf(prefs.getBoolean("welcomed", false)) }
        var autoOpenChat by remember { mutableStateOf(false) }

        if (!hasWelcomed) {
            WelcomeScreen(
                onGetStarted = {
                    hasWelcomed = true
                    autoOpenChat = true
                }
            )
        } else {
            MainScreen(initialShowChat = autoOpenChat)
        }
    }
}
