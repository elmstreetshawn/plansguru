package com.plansguru.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class CirclePolicy(val id: String, val name: String, val policy: String, val sharedAttributes: String)
data class TaxonomyVariable(val id: String, val predicate: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateChat: () -> Unit = {}
) {
    val circles = remember {
        mutableStateListOf(
            CirclePolicy("1", "Circle 1 (Taco Crew)", "Authorized Peers", "Cuisines, Dietary Restrictions, General Vibe"),
            CirclePolicy("2", "Circle 2 (Salad Friends)", "Selected Peers Only", "Dietary Restrictions Only"),
            CirclePolicy("3", "Work Circle", "Private / Strict", "None (Isolated)")
        )
    }

    val variables = remember {
        mutableStateListOf(
            TaxonomyVariable("1", "likes_cuisine", "Preferred cuisines (e.g. Italian, Mexican)"),
            TaxonomyVariable("2", "dislikes_cuisine", "Disliked cuisines (Hard dealbreakers)"),
            TaxonomyVariable("3", "dietary_restriction", "Dietary needs (Vegan, Gluten-Free)"),
            TaxonomyVariable("4", "max_distance", "Travel distance & location preference"),
            TaxonomyVariable("5", "noise_preference", "Ambiance & crowd vibe (Quiet vs Loud)")
        )
    }

    var showAddCircleDialog by remember { mutableStateOf(false) }
    var showAddVarDialog by remember { mutableStateOf(false) }

    var editingCircle by remember { mutableStateOf<CirclePolicy?>(null) }
    var editingVar by remember { mutableStateOf<TaxonomyVariable?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & CRUD Policies", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(onClick = onNavigateChat) {
                        Text("Chatbot", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("100% On-Device & Encrypted", style = MaterialTheme.typography.titleSmall)
                            Text("SQLCipher & Android Keystore secured.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = onNavigateChat) {
                            Text("Open Chat")
                        }
                    }
                }
            }

            // Circles Section with CRUD
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Dining Circles & Sharing", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Manage circle permissions and shared attributes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showAddCircleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Circle")
                    }
                }
            }

            items(circles, key = { it.id }) { circle ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(circle.name, style = MaterialTheme.typography.titleMedium)
                            Row {
                                IconButton(onClick = { editingCircle = circle }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { circles.remove(circle) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(circle.policy, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Shared: ${circle.sharedAttributes}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Taxonomy Variables Section with CRUD
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Variable & Tag Taxonomy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Manage preference predicates and tags.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showAddVarDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Variable")
                    }
                }
            }

            items(variables, key = { it.id }) { v ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text(v.predicate, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Row {
                                IconButton(onClick = { editingVar = v }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { variables.remove(v) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(v.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Add Circle Dialog
    if (showAddCircleDialog) {
        var name by remember { mutableStateOf("") }
        var policy by remember { mutableStateOf("Authorized Peers") }
        var attrs by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCircleDialog = false },
            title = { Text("Add Dining Circle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Circle Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = policy, onValueChange = { policy = it }, label = { Text("Privacy Policy") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = attrs, onValueChange = { attrs = it }, label = { Text("Shared Attributes") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        circles.add(CirclePolicy(System.currentTimeMillis().toString(), name.trim(), policy.trim(), attrs.trim()))
                        showAddCircleDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAddCircleDialog = false }) { Text("Cancel") } }
        )
    }

    // Edit Circle Dialog
    if (editingCircle != null) {
        var name by remember { mutableStateOf(editingCircle!!.name) }
        var policy by remember { mutableStateOf(editingCircle!!.policy) }
        var attrs by remember { mutableStateOf(editingCircle!!.sharedAttributes) }

        AlertDialog(
            onDismissRequest = { editingCircle = null },
            title = { Text("Edit Dining Circle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Circle Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = policy, onValueChange = { policy = it }, label = { Text("Privacy Policy") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = attrs, onValueChange = { attrs = it }, label = { Text("Shared Attributes") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val idx = circles.indexOfFirst { it.id == editingCircle!!.id }
                    if (idx != -1) {
                        circles[idx] = CirclePolicy(editingCircle!!.id, name.trim(), policy.trim(), attrs.trim())
                    }
                    editingCircle = null
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { editingCircle = null }) { Text("Cancel") } }
        )
    }

    // Add Variable Dialog
    if (showAddVarDialog) {
        var predicate by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddVarDialog = false },
            title = { Text("Add Preference Variable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = predicate, onValueChange = { predicate = it }, label = { Text("Predicate Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (predicate.isNotBlank()) {
                        variables.add(TaxonomyVariable(System.currentTimeMillis().toString(), predicate.trim(), desc.trim()))
                        showAddVarDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAddVarDialog = false }) { Text("Cancel") } }
        )
    }

    // Edit Variable Dialog
    if (editingVar != null) {
        var predicate by remember { mutableStateOf(editingVar!!.predicate) }
        var desc by remember { mutableStateOf(editingVar!!.description) }

        AlertDialog(
            onDismissRequest = { editingVar = null },
            title = { Text("Edit Preference Variable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = predicate, onValueChange = { predicate = it }, label = { Text("Predicate Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val idx = variables.indexOfFirst { it.id == editingVar!!.id }
                    if (idx != -1) {
                        variables[idx] = TaxonomyVariable(editingVar!!.id, predicate.trim(), desc.trim())
                    }
                    editingVar = null
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { editingVar = null }) { Text("Cancel") } }
        )
    }
}
