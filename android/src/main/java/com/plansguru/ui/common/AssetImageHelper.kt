package com.plansguru.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun rememberAssetImage(fileName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(fileName) {
        try {
            context.assets.open(fileName).use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun GuruTopBarAction(onOpenGuruChat: () -> Unit) {
    val mascotBitmap = rememberAssetImage("expression_happy.png")
    IconButton(onClick = onOpenGuruChat) {
        if (mascotBitmap != null) {
            Image(
                bitmap = mascotBitmap,
                contentDescription = "Ask Guru",
                modifier = Modifier.size(32.dp)
            )
        } else {
            Icon(Icons.Default.Favorite, contentDescription = "Ask Guru")
        }
    }
}
