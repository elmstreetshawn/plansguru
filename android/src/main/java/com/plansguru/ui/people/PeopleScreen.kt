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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.plansguru.data.repository.GraphRepository

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
    onProceedToGroup: (List<String>) -> Unit,
    onOpenGuruChat: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showMyQrDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val repository = remember { GraphRepository(context) }

    val defaultFriends = listOf(
        FriendItem("1", "Maya", "MA", listOf("Circle 1", "taco crew"), false),
        FriendItem("2", "Jo", "JO", listOf("Circle 1", "taco crew", "work"), false),
        FriendItem("3", "Priya", "PR", listOf("Circle 2", "taco crew"), false),
        FriendItem("4", "Lena", "LE", listOf("Friend", "salad crew"), false),
        FriendItem("5", "Sam", "SA", listOf("Circle 2", "salad crew"), false)
    )

    val friends = remember {
        mutableStateListOf<FriendItem>().apply {
            addAll(defaultFriends)
        }
    }

    var myPersonState by remember { mutableStateOf(repository.getPerson("local_user")) }
    val myName = myPersonState?.displayName?.takeIf { it.isNotBlank() && it != "local_user" } ?: "Shawn (You)"
    val myZip = myPersonState?.locationZip ?: "78626"
    var showEditMyNameInPeople by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            myPersonState = repository.getPerson("local_user")
            val dbPersons = repository.getAllPersons()
            val existingNames = friends.map { it.name }
            for (person in dbPersons) {
                if (person.localPersonId == "local_user" || person.displayName == "local_user" || person.displayName == "Me") continue
                if (!existingNames.contains(person.displayName) && !existingNames.contains(person.localPersonId)) {
                    val name = person.displayName
                    val initials = name.take(2).uppercase()
                    friends.add(
                        FriendItem(
                            id = "db_${person.localPersonId}",
                            name = name,
                            initials = initials,
                            tags = listOf("Added via Agent", "Encrypted Graph"),
                            isSelected = false
                        )
                    )
                }
            }
        } catch (_: Exception) {}
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
                    com.plansguru.ui.common.GuruTopBarAction(onOpenGuruChat)
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

            // Pinned "YOU" Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(myName.take(2).uppercase(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(myName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                    Text("YOU", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Text("📍 Zip: $myZip • Encrypted Profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = { showEditMyNameInPeople = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit My Name", tint = MaterialTheme.colorScheme.primary)
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

            // Add a friend button (Explicit clickable row inside Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddDialog = true }
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
                    var showPreferencesDialog by remember { mutableStateOf(false) }
                    var friendAssertions by remember { mutableStateOf<List<com.plansguru.data.model.Assertion>>(emptyList()) }
                    var newPrefPredicate by remember { mutableStateOf("likes_cuisine") }
                    var newPrefValue by remember { mutableStateOf("") }

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
                            TextButton(onClick = {
                                friendAssertions = repository.getAssertionsForSubject(friend.name)
                                showPreferencesDialog = true
                            }) {
                                Text("Prefs")
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

                    if (showPreferencesDialog) {
                        var selectedPredicate by remember { mutableStateOf("likes_cuisine") }
                        var customValueInput by remember { mutableStateOf("") }

                        val taxonomyVariables = listOf(
                            "likes_cuisine" to "🍕 Likes Cuisine",
                            "dislikes_cuisine" to "🚫 Dislikes Cuisine",
                            "dietary_restriction" to "🥗 Dietary / Allergy",
                            "max_travel_time" to "🚗 Travel Distance / Drive",
                            "noise_preference" to "🤫 Noise / Crowd Level",
                            "locationZip" to "📍 Zip Code"
                        )

                        val quickSuggestions = when (selectedPredicate) {
                            "likes_cuisine" -> listOf("Italian", "Mexican", "Sushi", "Thai", "Pizza", "BBQ", "Japanese", "Chinese")
                            "dislikes_cuisine" -> listOf("Spicy", "Fast Food", "Seafood", "Indian", "BBQ")
                            "dietary_restriction" -> listOf("Vegan", "Gluten-Free", "Vegetarian", "Dairy-Free", "Halal", "Kosher", "Peanut Allergy")
                            "max_travel_time" -> listOf("10 min", "15 min", "20 min", "30 min")
                            "noise_preference" -> listOf("Quiet / Low Crowds", "Moderate", "Vibrant / Loud")
                            "locationZip" -> listOf("78626", "78681", "78701", "78704", "78652", "78748")
                            else -> listOf("Italian", "Vegan", "15 min")
                        }

                        AlertDialog(
                            onDismissRequest = { showPreferencesDialog = false },
                            title = { Text("Preferences for ${friend.name}") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Current Encrypted Preferences:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    if (friendAssertions.isEmpty()) {
                                        Text("No preferences recorded yet. Set one below!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            friendAssertions.forEach { assertion ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        AssistChip(onClick = {}, label = { Text(assertion.predicate.replace('_', ' '), style = MaterialTheme.typography.labelSmall) })
                                                        Text(assertion.value, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            repository.deleteAssertion(assertion.assertionId)
                                                            friendAssertions = repository.getAssertionsForSubject(friend.name)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Text("×", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Text("Add New Preference:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text("1. Select Preference Category:", style = MaterialTheme.typography.labelSmall)

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        taxonomyVariables.forEach { (varKey, varName) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedPredicate = varKey }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(selected = selectedPredicate == varKey, onClick = { selectedPredicate = varKey })
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(varName, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }

                                    Text("2. Quick Tap Suggestions:", style = MaterialTheme.typography.labelSmall)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(quickSuggestions) { sug ->
                                            FilterChip(
                                                selected = customValueInput == sug,
                                                onClick = { customValueInput = sug },
                                                label = { Text(sug, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = customValueInput,
                                        onValueChange = { customValueInput = it },
                                        label = { Text("Value / Custom Preference") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    enabled = customValueInput.isNotBlank(),
                                    onClick = {
                                        val valText = customValueInput.trim()
                                        if (selectedPredicate == "locationZip") {
                                            val currentPerson = repository.getPerson(friend.name) ?: com.plansguru.data.model.Person(
                                                localPersonId = friend.name,
                                                displayName = friend.name,
                                                publicIdentityKey = "pub_key_${friend.name}",
                                                creationTimestamp = System.currentTimeMillis(),
                                                locationZip = valText
                                            )
                                            repository.savePerson(currentPerson.copy(locationZip = valText))
                                        }

                                        repository.saveAssertion(
                                            com.plansguru.data.model.Assertion(
                                                assertionId = java.util.UUID.randomUUID().toString(),
                                                observerId = "local_user",
                                                subjectId = friend.name,
                                                predicate = selectedPredicate,
                                                value = valText,
                                                source = "PEOPLE_SCREEN_UI",
                                                confidence = 1.0f,
                                                validityTimestampStart = System.currentTimeMillis(),
                                                validityTimestampEnd = null,
                                                visibilityPolicy = "PRIVATE"
                                            )
                                        )
                                        friendAssertions = repository.getAssertionsForSubject(friend.name)
                                        customValueInput = ""
                                    }
                                ) {
                                    Text("Save Preference")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPreferencesDialog = false }) {
                                    Text("Done")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Friend Dialog / Bottom Sheet
    if (showAddDialog) {
        var manualName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add a Friend") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            showAddDialog = false
                            showQrScannerDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("📷 Scan Friend's QR Code")
                    }

                    OutlinedButton(
                        onClick = {
                            showAddDialog = false
                            showMyQrDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📱 Show My Personal QR Code")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        placeholder = { Text("Enter friend's name (e.g. Alex)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    Button(
                        onClick = {
                            if (manualName.isNotBlank()) {
                                val name = manualName.trim()
                                val initials = name.split(" ").mapNotNull { part -> part.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                                if (friends.none { f -> f.name.equals(name, ignoreCase = true) }) {
                                    friends.add(
                                        FriendItem(
                                            id = "manual_${System.currentTimeMillis()}",
                                            name = name,
                                            initials = initials,
                                            tags = listOf("Manual", "Encrypted Graph"),
                                            isSelected = false
                                        )
                                    )
                                    val repository = GraphRepository(context)
                                    repository.savePerson(
                                        com.plansguru.data.model.Person(
                                            localPersonId = name,
                                            displayName = name,
                                            publicIdentityKey = "pub_key_$name",
                                            creationTimestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                                manualName = ""
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Friend Manually")
                    }

                    OutlinedButton(
                        onClick = {
                            val demos = listOf("Alex Johnson", "David Miller", "Jessica Taylor", "Chris Evans")
                            val repository = GraphRepository(context)
                            for (demo in demos) {
                                if (friends.none { f -> f.name.equals(demo, ignoreCase = true) }) {
                                    val initials = demo.split(" ").mapNotNull { part -> part.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                                    friends.add(
                                        FriendItem(
                                            id = "demo_${System.currentTimeMillis()}_${friends.size}",
                                            name = demo,
                                            initials = initials,
                                            tags = listOf("Demo Contact"),
                                            isSelected = false
                                        )
                                    )
                                    repository.savePerson(
                                        com.plansguru.data.model.Person(
                                            localPersonId = demo,
                                            displayName = demo,
                                            publicIdentityKey = "pub_key_$demo",
                                            creationTimestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            showAddDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load Demo Contacts (For Emulator)")
                    }

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

    if (showQrScannerDialog) {
        com.plansguru.ui.common.QrCodeScannerDialog(
            onDismissRequest = { showQrScannerDialog = false },
            onQrCodeScanned = { scannedUri ->
                val uri = android.net.Uri.parse(scannedUri)
                val inviter = uri.getQueryParameter("inviter") ?: uri.getQueryParameter("coordinator") ?: "Scanned Friend"
                val initials = inviter.take(2).uppercase()
                if (friends.none { f -> f.name.equals(inviter, ignoreCase = true) }) {
                    friends.add(
                        FriendItem(
                            id = "qr_${System.currentTimeMillis()}",
                            name = inviter,
                            initials = initials,
                            tags = listOf("QR Scan", "Encrypted Graph"),
                            isSelected = false
                        )
                    )
                    repository.savePerson(
                        com.plansguru.data.model.Person(
                            localPersonId = inviter,
                            displayName = inviter,
                            publicIdentityKey = "pub_key_$inviter",
                            creationTimestamp = System.currentTimeMillis()
                        )
                    )
                    android.widget.Toast.makeText(context, "✨ Connected with $inviter via QR Scan!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showMyQrDialog) {
        val inviteLink = "https://plansguru.app/join?inviter=Me&pubkey=pub_key_local_user"
        val qrBitmap = remember(inviteLink) { com.plansguru.ui.common.QrCodeHelper.generateQrCodeBitmap(inviteLink) }

        AlertDialog(
            onDismissRequest = { showMyQrDialog = false },
            title = { Text("My Personal QR Code") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Have your friend scan this QR code to connect profiles!", style = MaterialTheme.typography.bodySmall)
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap,
                            contentDescription = "Personal QR Code",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    Text(inviteLink, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { showMyQrDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showEditMyNameInPeople) {
        var nameInput by remember { mutableStateOf(myPersonState?.displayName?.takeIf { it != "local_user" } ?: "") }
        AlertDialog(
            onDismissRequest = { showEditMyNameInPeople = false },
            title = { Text("Set Your Profile Display Name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your display name will be shown to friends when coordinating group dinner plans.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Your Name (e.g. Shawn)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = nameInput.isNotBlank(),
                    onClick = {
                        val updated = (myPersonState ?: com.plansguru.data.model.Person(
                            localPersonId = "local_user",
                            displayName = nameInput.trim(),
                            publicIdentityKey = "pub_key_local_user",
                            creationTimestamp = System.currentTimeMillis()
                        )).copy(displayName = nameInput.trim())

                        repository.savePerson(updated)
                        myPersonState = updated
                        showEditMyNameInPeople = false
                    }
                ) {
                    Text("Save Name")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMyNameInPeople = false }) {
                    Text("Cancel")
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

    val repository = GraphRepository(context)
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
                        isSelected = false
                    )
                )
                repository.savePerson(
                    com.plansguru.data.model.Person(
                        localPersonId = name,
                        displayName = name,
                        publicIdentityKey = "pub_key_$name",
                        creationTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
