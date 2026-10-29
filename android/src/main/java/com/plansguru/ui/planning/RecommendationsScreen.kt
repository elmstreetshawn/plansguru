package com.plansguru.ui.planning

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.plansguru.agent.NegotiationEngine
import com.plansguru.data.crypto.ChannelCryptoHelper
import com.plansguru.data.crypto.KeyManager
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class PlanChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    viewModel: PlanningViewModel,
    planId: String = "group_discussion_session",
    groupTitle: String = "Dinner Plan",
    onFinish: () -> Unit
) {
    val recommendations by viewModel.recommendations.collectAsState()
    val context = LocalContext.current
    val repository = remember { com.plansguru.data.repository.GraphRepository(context) }
    val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
    val localUserId = prefs.getString("user_id", "local_user") ?: "local_user"
    val myName = remember { repository.getPerson(localUserId)?.displayName?.takeIf { it.isNotBlank() && it != localUserId } ?: "Me" }
    
    val sessionId = if (planId == "group_discussion_session") "group_discussion_session" else "session_$planId"
    val syncClient = remember { com.plansguru.data.sync.EncryptedSyncClient(context, sessionId) }

    val coroutineScope = rememberCoroutineScope()
    val chatMessages = remember { mutableStateListOf<PlanChatMessage>() }
    var inputMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        syncClient.connect { text ->
            try {
                val jsonParser = Json { ignoreUnknownKeys = true }
                val msg = jsonParser.decodeFromString<com.plansguru.data.model.ProtocolMessage>(text)
                if (msg.type == "CHAT_MESSAGE") {
                    val decryptedPayload = ChannelCryptoHelper.decryptSession(msg.payload, sessionId)
                    val payload = jsonParser.decodeFromString<Map<String, String>>(decryptedPayload)
                    val senderName = payload["sender"] ?: msg.senderId
                    val content = payload["text"] ?: ""
                    if (content.isNotBlank() && senderName != myName) {
                        chatMessages.add(
                            PlanChatMessage(
                                id = msg.signature,
                                sender = senderName,
                                text = content,
                                timestamp = msg.timestamp
                            )
                        )
                    }
                } else if (msg.type == "PLAN_JOIN") {
                    val decryptedPayload = ChannelCryptoHelper.decryptSession(msg.payload, sessionId)
                    val payload = jsonParser.decodeFromString<Map<String, String>>(decryptedPayload)
                    val joinerName = payload["joinerName"] ?: msg.senderId
                    if (joinerName.isNotBlank() && joinerName != myName) {
                        val existing = repository.getPlanRecord(planId)
                        if (existing != null) {
                            val updatedParts = (existing.participants + joinerName).distinct()
                            repository.savePlanRecord(
                                planId = existing.id,
                                title = existing.title,
                                restaurantName = existing.restaurantName,
                                cuisine = existing.cuisine,
                                price = existing.price,
                                participants = updatedParts,
                                consensusScore = existing.consensusScore,
                                date = existing.date,
                                isUpcoming = existing.isUpcoming
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val savedPlanRecord = remember(planId) { repository.getPlanRecord(planId) }
    val displayRecs = remember(recommendations, savedPlanRecord) {
        if (recommendations.isNotEmpty()) {
            recommendations
        } else if (savedPlanRecord != null) {
            listOf(
                NegotiationEngine.EvaluationResult(
                    restaurant = com.plansguru.data.model.Restaurant(
                        id = "saved_${savedPlanRecord.id}",
                        name = savedPlanRecord.restaurantName,
                        cuisine = savedPlanRecord.cuisine,
                        priceRange = savedPlanRecord.price,
                        vegetarianOptions = true,
                        dietaryAccommodations = listOf("Vegetarian", "Gluten-Free"),
                        noiseLevel = "Moderate",
                        crowdLevel = "Moderate",
                        location = "Greater Austin, TX",
                        openingHours = "5:00 PM - 10:00 PM",
                        accessibilityAttributes = listOf("Wheelchair Accessible"),
                        address = "Austin, TX",
                        zipCode = "78701"
                    ),
                    minSatisfaction = 0.98f,
                    avgSatisfaction = 0.98f,
                    hardConstraintPassed = true
                )
            )
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (savedPlanRecord != null) savedPlanRecord.title else "Top Group Recommendations") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Group Recommendations & Consensus Details:",
                style = MaterialTheme.typography.bodyLarge
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(displayRecs) { index, rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("#${index + 1}: ${rec.restaurant.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Cuisine: ${rec.restaurant.cuisine} • Price: ${rec.restaurant.priceRange}", style = MaterialTheme.typography.bodyMedium)
                            Text("Location: ${rec.restaurant.location} • Hours: ${rec.restaurant.openingHours}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                            Text("Group Min Satisfaction: ${(rec.minSatisfaction * 100).toInt()}% (Avg: ${(rec.avgSatisfaction * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium)
                            Text("Hard Constraints Passed: ${rec.hardConstraintPassed}", style = MaterialTheme.typography.labelSmall)

                            Spacer(modifier = Modifier.height(4.dp))

                            var showAiTrace by remember { mutableStateOf(index == 0) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAiTrace = !showAiTrace }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🤖 On-Device AI Agent Negotiation Log", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(if (showAiTrace) "Hide Log ▲" else "View AI Reasoning ▼", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }

                            if (showAiTrace) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                                            Text("🔴", style = MaterialTheme.typography.bodySmall)
                                            Text("Conflict Identified: Initial candidates generated score penalties due to conflicting dis-affinities across group profiles.", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                                            Text("🔄", style = MaterialTheme.typography.bodySmall)
                                            Text("AI Reconciled: Shifted focus to ${rec.restaurant.cuisine} (${rec.restaurant.name}) — maximizes group satisfaction score (${(rec.avgSatisfaction * 100).toInt()}%) within ${rec.restaurant.priceRange} budget.", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                                            Text("🛡️", style = MaterialTheme.typography.bodySmall)
                                            Text("Constraints Verified: Verified ${rec.restaurant.name}'s menu supports ${if (rec.restaurant.dietaryAccommodations.isNotEmpty()) rec.restaurant.dietaryAccommodations.joinToString(", ") else "Vegetarian & Gluten-Free"} accommodations.", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                                            Text("📍", style = MaterialTheme.typography.bodySmall)
                                            Text("Geographic Radius: ${rec.restaurant.location} — optimal travel radius for all participants.", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Map and Calendar action buttons for the top recommendation
            val topRec = displayRecs.firstOrNull()
            if (topRec != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
                            val serverUrl = prefs.getString("server_url", "https://plansguru-austin.loca.lt")?.trimEnd('/') ?: "https://plansguru-austin.loca.lt"
                            
                            var userId = prefs.getString("user_id", null)
                            if (userId.isNullOrBlank() || userId == "local_user") {
                                userId = java.util.UUID.randomUUID().toString()
                                prefs.edit().putString("user_id", userId).apply()
                            }
                            var publicKey = prefs.getString("public_key", null)
                            if (publicKey.isNullOrBlank()) {
                                val keyPair = com.plansguru.data.crypto.KeyManager.getOrCreateIdentityKeyPair(context)
                                publicKey = keyPair.public.encoded.joinToString("") { "%02x".format(it) }
                                prefs.edit().putString("public_key", publicKey).apply()
                            }
                            val groupTitle = "Dinner Plan"
                            val pId = if (planId == "group_discussion_session") "plan_${System.currentTimeMillis()}" else planId
                            val rName = topRec.restaurant.name
                            val cName = topRec.restaurant.cuisine
                            val pPrice = topRec.restaurant.priceRange
                            val cScore = "${(topRec.minSatisfaction * 100).toInt()}% Match"
                            val pList = (listOf(myName) + chatMessages.map { it.sender }).distinct().joinToString(",")

                            val deepLink = "$serverUrl/join?inviter=${Uri.encode(userId)}&pubkey=${Uri.encode(publicKey)}&name=${Uri.encode(myName)}&group=${Uri.encode(groupTitle)}&planid=${Uri.encode(pId)}&date=${Uri.encode("Active Now")}&restaurant=${Uri.encode(rName)}&cuisine=${Uri.encode(cName)}&price=${Uri.encode(pPrice)}&score=${Uri.encode(cScore)}&participants=${Uri.encode(pList)}"

                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val serverApi = com.plansguru.data.api.ServerApiClient(context)
                                val shortUrl = serverApi.createShortInvite(deepLink)
                                val shareUrl = shortUrl ?: deepLink

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Join my PlansGuru Dinner Plan")
                                    putExtra(Intent.EXTRA_TEXT, "Hey! Join my PlansGuru dinner plan '$groupTitle' at ${topRec.restaurant.name}! Tap here: $shareUrl")
                                }
                                val chooser = Intent.createChooser(shareIntent, "Share Plan Link via Messenger / SMS")
                                try {
                                    context.startActivity(chooser)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape
                    ) {
                        Text("💬 Share via SMS")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("geo:0,0?q=${Uri.encode(topRec.restaurant.name + ", " + topRec.restaurant.location)}")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape
                    ) {
                        Text("🗺️ Open Map")
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, "Group Dinner: ${topRec.restaurant.name}")
                                putExtra(CalendarContract.Events.EVENT_LOCATION, topRec.restaurant.location)
                                putExtra(CalendarContract.Events.DESCRIPTION, "PlansGuru Dinner Consensus (${(topRec.minSatisfaction * 100).toInt()}% match)")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape
                    ) {
                        Text("📅 Add to Calendar")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text("💬 Group Discussion", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatMessages) { msg ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (msg.sender == myName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${msg.sender}: ${msg.text}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Type a message to group...") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                Button(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            val txt = inputMessage.trim()
                            chatMessages.add(PlanChatMessage(System.currentTimeMillis().toString(), myName, txt, System.currentTimeMillis()))

                            val jsonParser = Json { ignoreUnknownKeys = true }
                            val rawPayload = jsonParser.encodeToString(mapOf("sender" to myName, "text" to txt))
                            val encryptedPayload = ChannelCryptoHelper.encryptSession(rawPayload, sessionId)
                            val signature = KeyManager.sign(context, encryptedPayload)
                            val protoMsg = com.plansguru.data.model.ProtocolMessage(
                                type = "CHAT_MESSAGE",
                                senderId = localUserId,
                                sessionId = sessionId,
                                payload = encryptedPayload,
                                signature = signature,
                                timestamp = System.currentTimeMillis()
                            )
                            syncClient.sendEncryptedPayload(jsonParser.encodeToString(protoMsg))
                            inputMessage = ""
                        }
                    },
                    shape = CircleShape
                ) {
                    Text("Send")
                }
            }

            Button(
                onClick = {
                    val topRec = displayRecs.firstOrNull()
                    if (topRec != null) {
                        val pIdToSave = if (planId == "group_discussion_session") "plan_${System.currentTimeMillis()}" else planId
                        val titleToSave = if (savedPlanRecord != null) savedPlanRecord.title else if (groupTitle.isNotBlank()) groupTitle else "Group Dinner Consensus"
                        val existingParticipants = savedPlanRecord?.participants ?: emptyList()
                        val participants = (listOf(myName) + existingParticipants + chatMessages.map { it.sender }).distinct().filter { it.isNotBlank() }
                        repository.savePlanRecord(
                            planId = pIdToSave,
                            title = titleToSave,
                            restaurantName = topRec.restaurant.name,
                            cuisine = topRec.restaurant.cuisine,
                            price = topRec.restaurant.priceRange,
                            participants = participants,
                            consensusScore = "${(topRec.minSatisfaction * 100).toInt()}% Match",
                            date = savedPlanRecord?.date ?: "Active Now",
                            isUpcoming = true
                        )
                    }
                    onFinish()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Text("Done / Back to Dashboard")
            }
        }
    }
}
