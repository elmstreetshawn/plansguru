package com.plansguru.ui.welcome

import android.content.Context
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
import androidx.compose.ui.unit.dp
import com.plansguru.ui.planning.rememberAssetImage

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    val context = LocalContext.current
    val mascotBitmap = rememberAssetImage("mascot_hero.png")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (mascotBitmap != null) {
                        Image(
                            bitmap = mascotBitmap,
                            contentDescription = "PlansGuru Mascot",
                            modifier = Modifier.size(160.dp)
                        )
                    } else {
                        Text("🌮✨", style = MaterialTheme.typography.displayLarge)
                    }
                }

                Text("PlansGuru", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Dinner planning without compromise.\nAnd without the cloud.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "100% On-Device • Encrypted Social Graph • Zero Tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("welcomed", true).apply()
                        onGetStarted()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape
                ) {
                    Text("Get Started", style = MaterialTheme.typography.titleMedium)
                }
                Text("🔒 Your data never leaves your phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
