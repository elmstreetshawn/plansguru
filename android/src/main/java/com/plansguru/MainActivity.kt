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
            // New UUID-based format
            val inviterId = uri.getQueryParameter("inviter")  // UUID
            val inviterPubKey = uri.getQueryParameter("pubkey")  // Public key
            val group = uri.getQueryParameter("group") ?: "Dinner Plan"
            val rawPlanId = uri.getQueryParameter("planid")
            val planDate = uri.getQueryParameter("date") ?: "Active Now"
            val restaurant = uri.getQueryParameter("restaurant")?.takeIf { it.isNotBlank() } ?: "Suerte"
            val cuisine = uri.getQueryParameter("cuisine")?.takeIf { it.isNotBlank() } ?: "Interior Mexican"
            val price = uri.getQueryParameter("price")?.takeIf { it.isNotBlank() } ?: "$$$"
            val score = uri.getQueryParameter("score")?.takeIf { it.isNotBlank() } ?: "98% Match"
            val rawParticipants = uri.getQueryParameter("participants")

            // Fallback to old format for backwards compatibility
            val displayName = uri.getQueryParameter("name") ?: inviterId ?: "Friend"

            val participantsList = if (!rawParticipants.isNullOrBlank()) {
                (listOf("Me") + rawParticipants.split(",")).distinct()
            } else {
                listOf("Me", displayName)
            }

            try {
                val repository = GraphRepository(this)

                // Check if this person already exists by public key
                val existingPerson = if (!inviterPubKey.isNullOrBlank()) {
                    repository.getPersonByPublicKey(inviterPubKey)
                } else {
                    null
                }

                // Use existing person or create new one
                val personId = existingPerson?.localPersonId ?: (inviterId ?: "friend_${System.currentTimeMillis()}")

                val person = Person(
                    localPersonId = personId,
                    displayName = displayName,
                    publicIdentityKey = inviterPubKey ?: "pub_key_$displayName",
                    creationTimestamp = System.currentTimeMillis()
                )
                repository.savePerson(person)

                val planId = if (!rawPlanId.isNullOrBlank()) rawPlanId else "plan_dl_${System.currentTimeMillis()}"
                repository.savePlanRecord(
                    planId = planId,
                    title = group,
                    restaurantName = restaurant,
                    cuisine = cuisine,
                    price = price,
                    participants = participantsList,
                    consensusScore = score,
                    date = planDate,
                    isUpcoming = true
                )

                val prefs = getSharedPreferences("plansguru_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("pending_navigate_to_plans", true).apply()

                Toast.makeText(this, "✨ Joined '$group' with $displayName!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error joining plan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
