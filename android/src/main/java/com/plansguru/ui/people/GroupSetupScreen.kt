package com.plansguru.ui.people

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.plansguru.data.repository.GraphRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSetupScreen(
    initialMembers: List<String>,
    onBack: () -> Unit,
    onAskGuru: (String, List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var selectedTargetTime by remember { mutableStateOf("Tonight @ 7:00 PM") }
    val context = LocalContext.current
    val repository = remember { GraphRepository(context) }
    val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
    val localUserId = prefs.getString("user_id", "local_user") ?: "local_user"
    val userDisplayName = remember { repository.getPerson(localUserId)?.displayName?.takeIf { it.isNotBlank() && it != localUserId } ?: "Me" }

    val allFriendsList = remember {
        mutableStateListOf("Maya", "Jo", "Priya", "Lena", "Sam").apply {
            try {
                val dbPersons = repository.getAllPersons().map { it.displayName }
                for (pName in dbPersons) {
                    if (!contains(pName) && pName != "Me" && pName != localUserId && pName != userDisplayName) add(pName)
                }
            } catch (_: Exception) {}
        }
    }

    val selectedMembers = remember {
        mutableStateListOf<String>().apply {
            if (initialMembers.isNotEmpty()) {
                addAll(initialMembers.filter { it != "Me" && it != localUserId && it != userDisplayName })
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Group Members", style = MaterialTheme.typography.titleLarge)
                        Text("${selectedMembers.size + 1} people ($userDisplayName + ${selectedMembers.size} friends)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Create, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Name this group (e.g. Taco Crew)") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
            }

            Text("Target Dining Date & Time Slot:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val timeOptions = listOf(
                    "Tonight @ 7:00 PM",
                    "Tomorrow @ 6:30 PM",
                    "Friday @ 7:30 PM",
                    "Saturday @ 8:00 PM"
                )
                timeOptions.forEach { timeOpt ->
                    FilterChip(
                        selected = selectedTargetTime == timeOpt,
                        onClick = { selectedTargetTime = timeOpt },
                        label = { Text(timeOpt, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {
                        selectedMembers.clear()
                        selectedMembers.addAll(allFriendsList)
                    },
                    label = { Text("☑ Select All") }
                )
                AssistChip(
                    onClick = {
                        selectedMembers.clear()
                    },
                    label = { Text("☐ Clear All") }
                )
            }

            Text("Select people below for your dinner plan:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allFriendsList) { name ->
                    val isChecked = selectedMembers.contains(name)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedMembers.remove(name) else selectedMembers.add(name)
                            },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedMembers.contains(name)) selectedMembers.add(name)
                                    } else {
                                        selectedMembers.remove(name)
                                    }
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = {
                                selectedMembers.remove(name)
                                allFriendsList.remove(name)
                                try {
                                    repository.deletePerson(name)
                                } catch (_: Exception) {}
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Remove Person", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }



            Button(
                enabled = selectedMembers.isNotEmpty(),
                onClick = {
                    val syncClient = com.plansguru.data.sync.EncryptedSyncClient(context)
                    val gName = groupName.ifBlank { "Dinner Group" }
                    val fullParticipants = (listOf(userDisplayName) + selectedMembers.toList()).distinct()
                    repository.saveAssertion(
                        com.plansguru.data.model.Assertion(
                            assertionId = java.util.UUID.randomUUID().toString(),
                            observerId = localUserId,
                            subjectId = localUserId,
                            predicate = "preferred_time",
                            value = selectedTargetTime,
                            source = "GROUP_SETUP",
                            confidence = 1.0f,
                            validityTimestampStart = System.currentTimeMillis(),
                            validityTimestampEnd = null,
                            visibilityPolicy = "PRIVATE"
                        )
                    )
                    syncClient.broadcastPlanRequest(gName, fullParticipants)
                    onAskGuru(gName, fullParticipants)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("✨ Ask the Guru (${selectedMembers.size + 1} people)", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
