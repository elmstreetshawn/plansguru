package com.plansguru

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.plansguru.data.model.Person
import com.plansguru.data.repository.GraphRepository
import com.plansguru.ui.navigation.NavGraph
import com.plansguru.ui.theme.PlansGuruTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent(intent)

        setContent {
            PlansGuruTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null) {
            val inviter = uri.getQueryParameter("inviter") ?: uri.getQueryParameter("coordinator")
            val pubkey = uri.getQueryParameter("pubkey") ?: "pub_key_$inviter"
            if (!inviter.isNullOrBlank()) {
                try {
                    val repository = GraphRepository(this)
                    repository.savePerson(
                        Person(
                            localPersonId = inviter,
                            displayName = inviter,
                            publicIdentityKey = pubkey,
                            creationTimestamp = System.currentTimeMillis()
                        )
                    )
                    Toast.makeText(this, "✨ Connected with $inviter via Deep Link!", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {}
            }
        }
    }
}
