package com.plansguru.ui.planning

import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class PlanRecord(
    val id: String,
    var title: String,
    val restaurantName: String,
    val cuisine: String,
    val price: String,
    val participants: List<String>,
    val consensusScore: String,
    val date: String,
    val isUpcoming: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlansHistoryScreen(
    onCreateNewPlan: () -> Unit,
    onOpenPlanChat: (String, String) -> Unit = { _, _ -> },
    onOpenGuruChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { com.plansguru.data.repository.GraphRepository(context) }
    val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
    val localUserId = prefs.getString("user_id", "local_user") ?: "local_user"
    val allPlans = remember { mutableStateListOf<PlanRecord>() }

    fun refreshPlansFromDb() {
        val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
        val seeded = prefs.getBoolean("default_plans_seeded", false)
        val dbPlans = repository.getAllPlanRecords()

        if (dbPlans.isEmpty() && !seeded) {
            prefs.edit().putBoolean("default_plans_seeded", true).apply()
            val defaultSample1 = PlanRecord("1", "Friday Taco Crew Night", "Suerte", "Interior Mexican", "$$$", listOf("Maya", "Jo", "Priya"), "98% Match", "Active Now", isUpcoming = true)
            val defaultSample2 = PlanRecord("2", "Sunday Salad Friends Brunch", "Green Leaf Bistro", "Vegetarian / Vegan", "$$", listOf("Lena", "Sam"), "95% Match", "Oct 24, 11:30 AM", isUpcoming = false)
            val defaultSample3 = PlanRecord("3", "Friday Night Italian Dinner", "Trattoria Bella", "Italian", "$$$", listOf("Maya", "Jo", "Priya", "Sam"), "92% Match", "Oct 18, 8:00 PM", isUpcoming = false)

            repository.savePlanRecord(defaultSample1.id, defaultSample1.title, defaultSample1.restaurantName, defaultSample1.cuisine, defaultSample1.price, defaultSample1.participants, defaultSample1.consensusScore, defaultSample1.date, defaultSample1.isUpcoming)
            repository.savePlanRecord(defaultSample2.id, defaultSample2.title, defaultSample2.restaurantName, defaultSample2.cuisine, defaultSample2.price, defaultSample2.participants, defaultSample2.consensusScore, defaultSample2.date, defaultSample2.isUpcoming)
            repository.savePlanRecord(defaultSample3.id, defaultSample3.title, defaultSample3.restaurantName, defaultSample3.cuisine, defaultSample3.price, defaultSample3.participants, defaultSample3.consensusScore, defaultSample3.date, defaultSample3.isUpcoming)

            allPlans.clear()
            allPlans.addAll(listOf(defaultSample1, defaultSample2, defaultSample3))
        } else {
            allPlans.clear()
            allPlans.addAll(dbPlans)
        }
    }

    LaunchedEffect(Unit) {
        refreshPlansFromDb()

        val friendsList = repository.getAllPersons().map { it.displayName } + listOf("Maya", "Jo", "Priya", "Lena", "Sam", "Alex", "Brian")
        val syncClient = com.plansguru.data.sync.EncryptedSyncClient(context)
        syncClient.listenOnFriendChannels(friendsList.distinct()) { msg ->
            if (msg.type == "PLAN_REQUEST") {
                try {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val payload = json.decodeFromString<Map<String, String>>(msg.payload)
                    val title = payload["groupTitle"] ?: "New Group Dinner"
                    val parts = payload["participants"]?.split(",") ?: listOf(msg.senderId)

                    val planId = "plan_${System.currentTimeMillis()}"
                    repository.savePlanRecord(
                        planId = planId,
                        title = title,
                        restaurantName = "Suerte",
                        cuisine = "Interior Mexican",
                        price = "$$$",
                        participants = parts,
                        consensusScore = "98% Match",
                        date = "Active Now",
                        isUpcoming = true
                    )

                    refreshPlansFromDb()
                } catch (_: Exception) {}
            } else if (msg.type == "GRAPH_UPDATE") {
                try {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val payload = json.decodeFromString<Map<String, String>>(msg.payload)
                    val assertion = com.plansguru.data.model.Assertion(
                        assertionId = payload["assertionId"] ?: java.util.UUID.randomUUID().toString(),
                        observerId = msg.senderId,
                        subjectId = payload["subjectId"] ?: msg.senderId,
                        predicate = payload["predicate"] ?: "general_preference",
                        value = payload["value"] ?: "",
                        source = "E2E_PEER_SYNC",
                        confidence = payload["confidence"]?.toFloatOrNull() ?: 1.0f,
                        validityTimestampStart = System.currentTimeMillis(),
                        validityTimestampEnd = null,
                        visibilityPolicy = payload["visibilityPolicy"] ?: "AUTHORIZED_PEERS"
                    )
                    repository.saveAssertion(assertion)
                } catch (_: Exception) {}
            }
        }
    }

    SideEffect {
        refreshPlansFromDb()
    }

    var selectedPlan by remember { mutableStateOf<PlanRecord?>(null) }

    val upcomingPlans = allPlans.filter { it.isUpcoming }
    val pastPlans = allPlans.filter { !it.isUpcoming }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Dinner Plans & History", style = MaterialTheme.typography.titleLarge)
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("v1.2.4", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    com.plansguru.ui.common.GuruTopBarAction(onOpenGuruChat, onOpenSettings)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewPlan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Start New Plan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Coordinate Group Dinner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Plan consensus meals based on everyone's encrypted preferences.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Button(onClick = onCreateNewPlan, shape = CircleShape) {
                        Text("New Plan")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: ACTIVE & UPCOMING PLANS
                item {
                    Text("🚀 ACTIVE & UPCOMING PLANS · ${upcomingPlans.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                if (upcomingPlans.isEmpty()) {
                    item {
                        Text("No active upcoming plans yet. Tap 'New Plan' to start one!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(upcomingPlans, key = { "up_${it.id}" }) { plan ->
                        var showPlanMenu by remember { mutableStateOf(false) }

                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { selectedPlan = plan },
                                        onLongClick = { showPlanMenu = true }
                                    ),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(plan.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                plan.consensusScore,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${plan.restaurantName} (${plan.cuisine} • ${plan.price})", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    val formattedParticipants = plan.participants.map { p ->
                                        val trimmed = p.trim()
                                        if (trimmed.equals("Me", ignoreCase = true) || trimmed.equals(localUserId, ignoreCase = true)) {
                                            "Me (You)"
                                        } else {
                                            val person = repository.getPerson(trimmed) ?: repository.getAllPersons().find { it.displayName.equals(trimmed, ignoreCase = true) }
                                            if (person != null) {
                                                val rel = repository.getRelationship(localUserId, person.localPersonId)
                                                val circleTag = rel?.relationshipType ?: "Friend"
                                                "$trimmed ($circleTag)"
                                            } else {
                                                "$trimmed (Friend-of-Friend 🛡️)"
                                            }
                                        }
                                    }.joinToString(", ")

                                    Text("Participants: $formattedParticipants", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Status: ${plan.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            DropdownMenu(
                                expanded = showPlanMenu,
                                onDismissRequest = { showPlanMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✏️ Edit Plan Details") },
                                    onClick = {
                                        showPlanMenu = false
                                        selectedPlan = plan
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("💬 Share Plan Link via Messenger") },
                                    onClick = {
                                        showPlanMenu = false
                                        val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
                                        val localUserId = prefs.getString("user_id", "local_user") ?: "local_user"
                                        val userDisplayName = repository.getPerson(localUserId)?.displayName?.takeIf { it.isNotBlank() && it != localUserId } ?: "Me"
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

                                        val partsEnc = java.net.URLEncoder.encode(plan.participants.joinToString(","), "UTF-8")
                                        val deepLink = "$serverUrl/join?inviter=${java.net.URLEncoder.encode(userId, "UTF-8")}&pubkey=${java.net.URLEncoder.encode(publicKey, "UTF-8")}&name=${java.net.URLEncoder.encode(userDisplayName, "UTF-8")}&group=${java.net.URLEncoder.encode(plan.title, "UTF-8")}&planid=${java.net.URLEncoder.encode(plan.id, "UTF-8")}&date=${java.net.URLEncoder.encode(plan.date, "UTF-8")}&restaurant=${java.net.URLEncoder.encode(plan.restaurantName, "UTF-8")}&cuisine=${java.net.URLEncoder.encode(plan.cuisine, "UTF-8")}&price=${java.net.URLEncoder.encode(plan.price, "UTF-8")}&score=${java.net.URLEncoder.encode(plan.consensusScore, "UTF-8")}&participants=$partsEnc"

                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            val serverApi = com.plansguru.data.api.ServerApiClient(context)
                                            val shortUrl = serverApi.createShortInvite(deepLink)
                                            val shareUrl = shortUrl ?: deepLink

                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, "Join my PlansGuru dinner plan '${plan.title}': $shareUrl")
                                            }
                                            try { context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Plan Link")) } catch (_: Exception) {}
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🗑️ Delete Plan", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showPlanMenu = false
                                        repository.deletePlanRecord(plan.id)
                                        refreshPlansFromDb()
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 2: PAST DINNER HISTORY
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📜 PAST DINNER HISTORY · ${pastPlans.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (pastPlans.isEmpty()) {
                    item {
                        Text("No completed dinner history yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(pastPlans, key = { "past_${it.id}" }) { plan ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlan = plan },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(plan.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            plan.consensusScore,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${plan.restaurantName} (${plan.cuisine} • ${plan.price})", style = MaterialTheme.typography.bodyMedium)
                                }
                                val formattedPastParticipants = plan.participants.map { p ->
                                    val trimmed = p.trim()
                                    if (trimmed.equals("Me", ignoreCase = true) || trimmed.equals(localUserId, ignoreCase = true)) {
                                        "Me (You)"
                                    } else {
                                        val person = repository.getPerson(trimmed) ?: repository.getAllPersons().find { it.displayName.equals(trimmed, ignoreCase = true) }
                                        if (person != null) {
                                            val rel = repository.getRelationship(localUserId, person.localPersonId)
                                            val circleTag = rel?.relationshipType ?: "Friend"
                                            "$trimmed ($circleTag)"
                                        } else {
                                            "$trimmed (Friend-of-Friend 🛡️)"
                                        }
                                    }
                                }.joinToString(", ")
                                Text("Participants: $formattedPastParticipants", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Completed: ${plan.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit / Delete Plan History Dialog
    if (selectedPlan != null) {
        var editableTitle by remember { mutableStateOf(selectedPlan!!.title) }

        AlertDialog(
            onDismissRequest = { selectedPlan = null },
            title = { Text("Edit Dinner Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editableTitle,
                        onValueChange = { editableTitle = it },
                        label = { Text("Plan Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Text("Restaurant: ${selectedPlan!!.restaurantName} (${selectedPlan!!.cuisine} • ${selectedPlan!!.price})", style = MaterialTheme.typography.bodyMedium)
                    Text("Participants: ${selectedPlan!!.participants.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Consensus: ${selectedPlan!!.consensusScore}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val p = selectedPlan!!
                            selectedPlan = null
                            onOpenPlanChat(p.id, p.title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💬 Open Live E2E Group Chat")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            repository.deletePlanRecord(selectedPlan!!.id)
                            refreshPlansFromDb()
                            selectedPlan = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Plan")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = selectedPlan!!
                        repository.savePlanRecord(p.id, editableTitle.trim(), p.restaurantName, p.cuisine, p.price, p.participants, p.consensusScore, p.date, p.isUpcoming)
                        refreshPlansFromDb()
                        selectedPlan = null
                    },
                    shape = CircleShape
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlan = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
