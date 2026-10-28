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
    val context = LocalContext.current
    val repository = remember { GraphRepository(context) }

    val allFriendsList = remember {
        mutableStateListOf("Maya", "Jo", "Priya", "Lena", "Sam").apply {
            try {
                val dbPersons = repository.getAllPersons().map { it.displayName }
                for (pName in dbPersons) {
                    if (!contains(pName) && pName != "Me" && pName != "local_user") add(pName)
                }
            } catch (_: Exception) {}
        }
    }

    val selectedMembers = remember {
        mutableStateListOf<String>().apply {
            if (initialMembers.isNotEmpty()) {
                addAll(initialMembers)
            } else {
                addAll(listOf("Maya", "Jo", "Priya"))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Group Members", style = MaterialTheme.typography.titleLarge)
                        Text("${selectedMembers.size} selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val gName = groupName.ifBlank { "Dinner Group" }
                    val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
                    val serverUrl = prefs.getString("server_url", "https://few-jars-wear.loca.lt")?.trimEnd('/') ?: "https://few-jars-wear.loca.lt"
                    val deepLink = "$serverUrl/join?inviter=local_user&group=${java.net.URLEncoder.encode(gName, "UTF-8")}"
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Join my PlansGuru Dinner Plan")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Hey! Join my PlansGuru dinner plan '$gName'! Tap here: $deepLink")
                    }
                    val chooser = android.content.Intent.createChooser(shareIntent, "Share Plan Link via Messenger / SMS")
                    try {
                        context.startActivity(chooser)
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape
            ) {
                Text("💬 Share Plan Link via Messenger / SMS")
            }

            Button(
                enabled = selectedMembers.isNotEmpty(),
                onClick = {
                    val syncClient = com.plansguru.data.sync.EncryptedSyncClient(context)
                    val gName = groupName.ifBlank { "Dinner Group" }
                    syncClient.broadcastPlanRequest(gName, selectedMembers.toList())
                    onAskGuru(gName, selectedMembers.toList())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("✨ Ask the Guru (${selectedMembers.size} members)", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
