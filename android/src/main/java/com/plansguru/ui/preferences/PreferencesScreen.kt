package com.plansguru.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.plansguru.data.model.Assertion
import com.plansguru.data.model.Person
import com.plansguru.data.repository.GraphRepository

data class SquadItem(
    val id: String,
    var title: String,
    val members: MutableList<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    viewModel: GuruChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { GraphRepository(context) }

    var selectedTab by remember { mutableStateOf(0) } // 0: People Graph, 1: Squads, 2: Tags, 3: AI Chat

    val messages by viewModel.messages.collectAsState()
    var inputMessage by remember { mutableStateOf("") }

    var allPersons by remember { mutableStateOf<List<Person>>(emptyList()) }
    var allAssertions by remember { mutableStateOf<List<Assertion>>(emptyList()) }

    val squads = remember {
        mutableStateListOf(
            SquadItem("1", "🌮 Taco crew", mutableStateListOf("Maya", "Jo", "Priya")),
            SquadItem("2", "🥗 Salad friends", mutableStateListOf("Lena", "Sam")),
            SquadItem("3", "🍕 Pizza night circle", mutableStateListOf("Maya", "Sam", "Gina", "Alex"))
        )
    }

    LaunchedEffect(selectedTab) {
        try {
            repository.deleteGeneralPreferences()
            allPersons = repository.getAllPersons()
            val subjects = listOf("local_user") + allPersons.map { it.localPersonId } + allPersons.map { it.displayName }
            val combined = mutableListOf<Assertion>()
            for (subj in subjects.distinct()) {
                combined.addAll(repository.getAssertionsForSubject(subj).filter { it.predicate != "general_preference" })
            }
            allAssertions = combined
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
                TopAppBar(
                    title = { Text("Preferences Hub", style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    edgePadding = 16.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        text = { Text("People Graph") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        text = { Text("Squads & Groups") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        text = { Text("Tags & Taxonomy") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        text = { Text("AI Chat Agent") }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // People Graph Tab (Full Interactive CRUD)
                    var targetSubjectForNewAssertion by remember { mutableStateOf<String?>(null) }
                    var selectedPredicate by remember { mutableStateOf("likes_cuisine") }
                    var customValueInput by remember { mutableStateOf("") }

                    val refreshGraphData = {
                        try {
                            repository.deleteGeneralPreferences()
                            allPersons = repository.getAllPersons()
                            val subjects = listOf("local_user") + allPersons.map { it.localPersonId }
                            val combined = mutableListOf<Assertion>()
                            for (subj in subjects.distinct()) {
                                combined.addAll(repository.getAssertionsForSubject(subj).filter { it.predicate != "general_preference" })
                            }
                            allAssertions = combined
                        } catch (_: Exception) {}
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Encrypted Social Graph • People & Preference Assertions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Manage connected profiles and their individual preference variables directly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val subjects = listOf("local_user") + allPersons.map { it.localPersonId }
                            items(subjects.distinct()) { subj ->
                                val assertions = allAssertions.filter { it.subjectId.equals(subj, true) }
                                val personName = if (subj == "local_user") "You (Local User)" else allPersons.find { it.localPersonId == subj }?.displayName ?: subj

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(personName.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                                Text(personName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(onClick = {
                                                    targetSubjectForNewAssertion = subj
                                                    selectedPredicate = "likes_cuisine"
                                                    customValueInput = ""
                                                }) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Add Preference", style = MaterialTheme.typography.labelMedium)
                                                }

                                                if (subj != "local_user") {
                                                    IconButton(onClick = {
                                                        repository.deletePerson(subj)
                                                        refreshGraphData()
                                                    }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Person", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        if (assertions.isEmpty()) {
                                            Text("No preferences recorded yet. Tap '+ Add Preference' or chat with Guru.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            assertions.forEach { assertion ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        AssistChip(onClick = {}, label = { Text(assertion.predicate.replace('_', ' '), style = MaterialTheme.typography.labelSmall) })
                                                        Text(assertion.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                                                    }
                                                    IconButton(onClick = {
                                                        repository.deleteAssertion(assertion.assertionId)
                                                        refreshGraphData()
                                                    }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Delete Assertion", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dialog to Add/CRUD Assertion Directly
                    if (targetSubjectForNewAssertion != null) {
                        val targetSubj = targetSubjectForNewAssertion!!
                        val targetName = if (targetSubj == "local_user") "You (Local User)" else allPersons.find { it.localPersonId == targetSubj }?.displayName ?: targetSubj

                        AlertDialog(
                            onDismissRequest = { targetSubjectForNewAssertion = null },
                            title = { Text("Add Preference for $targetName") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Select Variable Type:", style = MaterialTheme.typography.labelMedium)
                                    val taxonomyVariables = listOf(
                                        "likes_cuisine" to "Likes Cuisine",
                                        "dislikes_cuisine" to "Dislikes Cuisine",
                                        "dietary_restriction" to "Dietary Restriction",
                                        "max_travel_time" to "Max Travel Time",
                                        "noise_preference" to "Noise Preference",
                                        "locationZip" to "Zip Code"
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        taxonomyVariables.forEach { (varKey, varName) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedPredicate = varKey }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(selected = selectedPredicate == varKey, onClick = { selectedPredicate = varKey })
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(varName, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = customValueInput,
                                        onValueChange = { customValueInput = it },
                                        label = { Text("Value (e.g. Italian, Vegan, 15 min, 90210)") },
                                        modifier = Modifier.fillMaxWidth(),
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
                                            val currentPerson = repository.getPerson(targetSubj) ?: Person(
                                                localPersonId = targetSubj,
                                                displayName = if (targetSubj == "local_user") "Me" else targetSubj,
                                                publicIdentityKey = "pub_key_$targetSubj",
                                                creationTimestamp = System.currentTimeMillis(),
                                                locationZip = valText
                                            )
                                            repository.savePerson(currentPerson.copy(locationZip = valText))
                                        }

                                        repository.saveAssertion(
                                            Assertion(
                                                assertionId = java.util.UUID.randomUUID().toString(),
                                                observerId = "local_user",
                                                subjectId = targetSubj,
                                                predicate = selectedPredicate,
                                                value = valText,
                                                source = "MANUAL_UI_CRUD",
                                                confidence = 1.0f,
                                                validityTimestampStart = System.currentTimeMillis(),
                                                validityTimestampEnd = null,
                                                visibilityPolicy = "PRIVATE"
                                            )
                                        )

                                        refreshGraphData()
                                        targetSubjectForNewAssertion = null
                                    }
                                ) {
                                    Text("Save Preference")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { targetSubjectForNewAssertion = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                1 -> {
                    // Squads & Groups Tab (Fully interactive CRUD)
                    var selectedSquad by remember { mutableStateOf<SquadItem?>(null) }
                    var newMemberInput by remember { mutableStateOf("") }
                    var showAddSquadDialog by remember { mutableStateOf(false) }
                    var newSquadTitle by remember { mutableStateOf("") }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Squads & Dinner Groups", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("Tap any squad to edit members, title, and tags.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FloatingActionButton(
                                onClick = { showAddSquadDialog = true },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Squad", modifier = Modifier.size(20.dp))
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                            items(squads, key = { it.id }) { squad ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSquad = squad },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(squad.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                                Text("${squad.members.size} members", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            squad.members.forEach { m ->
                                                AssistChip(onClick = {}, label = { Text(m, style = MaterialTheme.typography.labelSmall) })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Edit Squad Dialog
                    if (selectedSquad != null) {
                        var editableTitle by remember { mutableStateOf(selectedSquad!!.title.replace(Regex("^[\\p{So}\\p{Cs}\\s]+"), "")) }
                        var selectedEmoji by remember { mutableStateOf(selectedSquad!!.title.takeWhile { !it.isLetterOrDigit() }.ifBlank { "🌮" }.trim()) }

                        AlertDialog(
                            onDismissRequest = { selectedSquad = null },
                            title = { Text("Edit Squad") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    EmojiPickerRow(
                                        selectedEmoji = selectedEmoji,
                                        onEmojiSelected = { selectedEmoji = it }
                                    )
                                    OutlinedTextField(
                                        value = editableTitle,
                                        onValueChange = { editableTitle = it },
                                        label = { Text("Squad Title") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    Text("Members:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    selectedSquad!!.members.forEach { member ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• $member", style = MaterialTheme.typography.bodyMedium)
                                            TextButton(onClick = {
                                                selectedSquad!!.members.remove(member)
                                            }) {
                                                Text("Remove", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = newMemberInput,
                                        onValueChange = { newMemberInput = it },
                                        label = { Text("Add member name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    Button(
                                        onClick = {
                                            if (newMemberInput.isNotBlank()) {
                                                selectedSquad!!.members.add(newMemberInput.trim())
                                                newMemberInput = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CircleShape
                                    ) {
                                        Text("Add Member")
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val emojiPrefix = if (selectedEmoji.isNotBlank()) "$selectedEmoji " else ""
                                    selectedSquad!!.title = "$emojiPrefix${editableTitle.trim()}".trim()
                                    selectedSquad = null
                                }) {
                                    Text("Save Changes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { selectedSquad = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Add Squad Dialog
                    if (showAddSquadDialog) {
                        var selectedEmoji by remember { mutableStateOf("🌮") }
                        AlertDialog(
                            onDismissRequest = { showAddSquadDialog = false },
                            title = { Text("Create New Squad") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    EmojiPickerRow(
                                        selectedEmoji = selectedEmoji,
                                        onEmojiSelected = { selectedEmoji = it }
                                    )
                                    OutlinedTextField(
                                        value = newSquadTitle,
                                        onValueChange = { newSquadTitle = it },
                                        label = { Text("Squad Title (e.g. Taco Crew)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (newSquadTitle.isNotBlank()) {
                                        val fullTitle = "$selectedEmoji ${newSquadTitle.trim()}"
                                        squads.add(SquadItem(System.currentTimeMillis().toString(), fullTitle, mutableStateListOf("Maya", "Sam")))
                                        newSquadTitle = ""
                                        showAddSquadDialog = false
                                    }
                                }) {
                                    Text("Create Squad")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddSquadDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                2 -> {
                    // Tags & Taxonomy Tab (Fully interactive CRUD)
                    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
                    var newTagInput by remember { mutableStateOf("") }
                    var showAddTagDialog by remember { mutableStateOf(false) }

                    val categories = listOf(
                        Triple("Cuisine Affinities", "likes_cuisine", allAssertions.filter { it.predicate.contains("cuisine") }),
                        Triple("Dietary Restrictions", "dietary_restriction", allAssertions.filter { it.predicate.contains("dietary") }),
                        Triple("Travel Distance", "max_distance", allAssertions.filter { it.predicate == "max_distance" }),
                        Triple("Noise & Crowds", "noise_preference", allAssertions.filter { it.predicate == "noise_preference" })
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Preference Taxonomy & Tags", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("Tap any category card to add, edit, or delete tags.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FloatingActionButton(
                                onClick = { showAddTagDialog = true },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Tag", modifier = Modifier.size(20.dp))
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                            items(categories) { cat ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCategoryName = cat.first },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(cat.first, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("Predicate: ${cat.second}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        val distinctValues = cat.third.map { it.value }.distinct()
                                        if (distinctValues.isEmpty()) {
                                            Text("No entries recorded in this category yet. Tap to add.", style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                distinctValues.forEach { value ->
                                                    AssistChip(onClick = {}, label = { Text(value) })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Edit Category / Tags Dialog
                    if (selectedCategoryName != null) {
                        val activeCat = categories.find { it.first == selectedCategoryName }
                        val activeAssertions = activeCat?.third ?: emptyList()
                        val predicateName = activeCat?.second ?: "likes_cuisine"

                        AlertDialog(
                            onDismissRequest = { selectedCategoryName = null },
                            title = { Text("Manage $selectedCategoryName") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Existing Tags & Values:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    if (activeAssertions.isEmpty()) {
                                        Text("No tags in this category yet.", style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        activeAssertions.forEach { assertion ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("• ${assertion.value}", style = MaterialTheme.typography.bodyMedium)
                                                TextButton(onClick = {
                                                    val dbHelper = com.plansguru.data.db.EncryptedGraphDatabase(context)
                                                    val writable = dbHelper.writableDatabase
                                                    writable.delete("assertions", "assertionId = ?", arrayOf(assertion.assertionId))

                                                    // Refresh assertions
                                                    try {
                                                        allPersons = repository.getAllPersons()
                                                        val subjects = listOf("local_user") + allPersons.map { it.localPersonId }
                                                        val combined = mutableListOf<Assertion>()
                                                        for (subj in subjects.distinct()) {
                                                            combined.addAll(repository.getAssertionsForSubject(subj))
                                                        }
                                                        allAssertions = combined
                                                    } catch (_: Exception) {}
                                                }) {
                                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = newTagInput,
                                        onValueChange = { newTagInput = it },
                                        label = { Text("Add new tag value") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    Button(
                                        onClick = {
                                            if (newTagInput.isNotBlank()) {
                                                repository.saveAssertion(
                                                    Assertion(
                                                        assertionId = java.util.UUID.randomUUID().toString(),
                                                        observerId = "local_user",
                                                        subjectId = "local_user",
                                                        predicate = predicateName,
                                                        value = newTagInput.trim(),
                                                        source = "MANUAL_TAG_EDIT",
                                                        confidence = 1.0f,
                                                        validityTimestampStart = System.currentTimeMillis(),
                                                        validityTimestampEnd = null,
                                                        visibilityPolicy = "PRIVATE"
                                                    )
                                                )
                                                try {
                                                    allPersons = repository.getAllPersons()
                                                    val subjects = listOf("local_user") + allPersons.map { it.localPersonId }
                                                    val combined = mutableListOf<Assertion>()
                                                    for (subj in subjects.distinct()) {
                                                        combined.addAll(repository.getAssertionsForSubject(subj))
                                                    }
                                                    allAssertions = combined
                                                } catch (_: Exception) {}
                                                newTagInput = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CircleShape
                                    ) {
                                        Text("Add Tag")
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { selectedCategoryName = null }, shape = CircleShape) {
                                    Text("Done")
                                }
                            }
                        )
                    }

                    // Add Custom Category / Tag Dialog
                    if (showAddTagDialog) {
                        var customPredicate by remember { mutableStateOf("") }
                        var customValue by remember { mutableStateOf("") }

                        AlertDialog(
                            onDismissRequest = { showAddTagDialog = false },
                            title = { Text("Create Custom Preference Tag") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = customPredicate,
                                        onValueChange = { customPredicate = it },
                                        label = { Text("Predicate (e.g. favorite_drink)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    OutlinedTextField(
                                        value = customValue,
                                        onValueChange = { customValue = it },
                                        label = { Text("Value (e.g. Espresso)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (customPredicate.isNotBlank() && customValue.isNotBlank()) {
                                            repository.saveAssertion(
                                                Assertion(
                                                    assertionId = java.util.UUID.randomUUID().toString(),
                                                    observerId = "local_user",
                                                    subjectId = "local_user",
                                                    predicate = customPredicate.trim(),
                                                    value = customValue.trim(),
                                                    source = "CUSTOM_TAGS_HUB",
                                                    confidence = 1.0f,
                                                    validityTimestampStart = System.currentTimeMillis(),
                                                    validityTimestampEnd = null,
                                                    visibilityPolicy = "PRIVATE"
                                                )
                                            )
                                            try {
                                                allPersons = repository.getAllPersons()
                                                val subjects = listOf("local_user") + allPersons.map { it.localPersonId }
                                                val combined = mutableListOf<Assertion>()
                                                for (subj in subjects.distinct()) {
                                                    combined.addAll(repository.getAssertionsForSubject(subj))
                                                }
                                                allAssertions = combined
                                            } catch (_: Exception) {}
                                            customPredicate = ""
                                            customValue = ""
                                            showAddTagDialog = false
                                        }
                                    },
                                    shape = CircleShape
                                ) {
                                    Text("Create Tag")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddTagDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                3 -> {
                    // AI Chat Agent Tab
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🤖 On-Device AI Agent", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("Chat naturally with your local agent. It learns your tastes and saves them to your encrypted social graph.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Chat Messages List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            reverseLayout = true
                        ) {
                            items(messages.reversed()) { msg ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            modifier = Modifier.padding(12.dp),
                                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        // Chat Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputMessage,
                                onValueChange = { inputMessage = it },
                                placeholder = { Text("Tell Guru your preferences...") },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.extraLarge
                            )
                            FloatingActionButton(
                                onClick = {
                                    if (inputMessage.isNotBlank()) {
                                        viewModel.sendMessage(inputMessage)
                                        inputMessage = ""
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(50.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiPickerRow(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit
) {
    val emojis = listOf("🌮", "🥗", "🍕", "🍔", "🍱", "🍝", "☕", "🍦", "🍹", "🍻", "🍿", "✨", "🎉", "🔥")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Select Emoji:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(emojis) { emoji ->
                Surface(
                    onClick = { onEmojiSelected(emoji) },
                    shape = CircleShape,
                    color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (selectedEmoji == emoji) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(emoji, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
