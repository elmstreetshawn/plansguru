package com.plansguru.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.plansguru.agent.GuruChatEngine
import com.plansguru.ui.planning.rememberAssetImage
import kotlinx.coroutines.launch

@Composable
fun ModelDownloadScreen(
    onModelReady: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val mascotBitmap = rememberAssetImage("mascot_hero.png")

    var statusText by remember { mutableStateOf("Checking on-device Gemma AI model...") }
    var isDownloading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Keep screen awake while downloading model
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(Unit) {
        val engine = GuruChatEngine(context)
        if (GuruChatEngine.isModelReady(context)) {
            onModelReady()
        } else {
            isDownloading = true
            val success = engine.initializeModel { text ->
                statusText = text
            }
            isDownloading = false
            if (success) {
                onModelReady()
            } else {
                errorMessage = engine.initializationError ?: "Failed to initialize model."
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (mascotBitmap != null) {
                    Image(
                        bitmap = mascotBitmap,
                        contentDescription = "PlansGuru Mascot",
                        modifier = Modifier.size(120.dp)
                    )
                } else {
                    Text("🤖", style = MaterialTheme.typography.displayLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Setting Up On-Device AI", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "PlansGuru runs 100% locally on your device using Gemma. We are preparing your secure local AI engine.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        errorMessage = null
                        isDownloading = true
                        scope.launch {
                            val engine = GuruChatEngine(context)
                            val success = engine.initializeModel { text ->
                                statusText = text
                            }
                            isDownloading = false
                            if (success) {
                                onModelReady()
                            } else {
                                errorMessage = engine.initializationError ?: "Failed to initialize model."
                            }
                        }
                    },
                    shape = CircleShape
                ) {
                    Text("Retry Download Progress")
                }
            }
        }
    }
}
