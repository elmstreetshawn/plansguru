package com.plansguru.ui.people

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class FriendItem(
    val id: String,
    val name: String,
    val initials: String,
    val tags: List<String>,
    var isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    onProceedToGroup: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val friends = remember {
        mutableStateListOf(
            FriendItem("1", "Maya", "MA", listOf("Circle 1", "taco crew"), true),
            FriendItem("2", "Jo", "JO", listOf("Circle 1", "taco crew", "work"), true),
            FriendItem("3", "Priya", "PR", listOf("Circle 2", "taco crew"), true),
            FriendItem("4", "Lena", "LE", listOf("Friend", "salad crew"), false),
            FriendItem("5", "Sam", "SA", listOf("Circle 2", "salad crew"), false)
        )
    }

    // Contact import permission launcher
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            importPhoneContacts(context, friends)
        }
    }

    val selectedFriends = friends.filter { it.isSelected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("People", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedFriends.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onProceedToGroup(selectedFriends.map { it.name }) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
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
            // Selected avatar pills row
            if (selectedFriends.isNotEmpty()) {
                Text("${selectedFriends.size} picked", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedFriends) { friend ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(friend.initials, style = MaterialTheme.typography.labelMedium)
                                Text(friend.name, style = MaterialTheme.typography.bodyMedium)
                                Text("×", modifier = Modifier.clickable {
                                    val index = friends.indexOfFirst { it.id == friend.id }
                                    if (index != -1) friends[index] = friends[index].copy(isSelected = false)
                                }, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search friends") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            // Add a friend button (clickable to open dialog)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddDialog = true },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("Add a friend", style = MaterialTheme.typography.titleSmall)
                        Text("Invite link · QR · Import Contacts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Squads Quick Start
            Text("SQUADS · QUICK START", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text("🌮 Taco crew · 3") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                )
                AssistChip(
                    onClick = { },
                    label = { Text("🥗 Salad friends · 2") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                )
            }

            // Friends list
            val filteredFriends = friends.filter { it.name.contains(searchQuery, ignoreCase = true) }
            Text("FRIENDS ON PLANS GURU · ${filteredFriends.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFriends) { friend ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val index = friends.indexOfFirst { it.id == friend.id }
                                if (index != -1) friends[index] = friends[index].copy(isSelected = !friend.isSelected)
                            },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(friend.initials, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(friend.tags.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(
                                checked = friend.isSelected,
                                onCheckedChange = { checked ->
                                    val index = friends.indexOfFirst { it.id == friend.id }
                                    if (index != -1) friends[index] = friends[index].copy(isSelected = checked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Friend Dialog / Bottom Sheet
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add a Friend") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Connect with friends to securely coordinate plans and share preferences.")
                    Button(
                        onClick = {
                            showAddDialog = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                importPhoneContacts(context, friends)
                            } else {
                                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import from Phone Contacts")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

fun importPhoneContacts(context: android.content.Context, friendsList: MutableList<FriendItem>) {
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
        null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    cursor?.use {
        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        while (it.moveToNext()) {
            val name = if (nameIdx != -1) it.getString(nameIdx) else "Unknown"
            val initials = name.split(" ").mapNotNull { part -> part.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
            if (friendsList.none { f -> f.name.equals(name, ignoreCase = true) }) {
                friendsList.add(
                    FriendItem(
                        id = "contact_${System.currentTimeMillis()}_${friendsList.size}",
                        name = name,
                        initials = initials.ifEmpty { "P" },
                        tags = listOf("Phone Contact"),
                        isSelected = true
                    )
                )
            }
        }
    }
}
