package com.plansguru.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp

data class PlanRecord(
    val id: String,
    var title: String,
    val restaurantName: String,
    val cuisine: String,
    val price: String,
    val participants: List<String>,
    val consensusScore: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansHistoryScreen(
    onCreateNewPlan: () -> Unit,
    onOpenGuruChat: () -> Unit = {}
) {
    val previousPlans = remember {
        mutableStateListOf(
            PlanRecord(
                id = "1",
                title = "Friday Taco Crew Night",
                restaurantName = "El Fuego Taqueria",
                cuisine = "Mexican",
                price = "$",
                participants = listOf("Maya", "Jo", "Priya"),
                consensusScore = "98% Match",
                date = "Yesterday, 7:30 PM"
            ),
            PlanRecord(
                id = "2",
                title = "Sunday Salad Friends Brunch",
                restaurantName = "Green Leaf Bistro",
                cuisine = "Vegetarian / Vegan",
                price = "$$",
                participants = listOf("Lena", "Sam"),
                consensusScore = "95% Match",
                date = "Oct 24, 11:30 AM"
            ),
            PlanRecord(
                id = "3",
                title = "Friday Night Italian Dinner",
                restaurantName = "Trattoria Bella",
                cuisine = "Italian",
                price = "$$$",
                participants = listOf("Maya", "Jo", "Priya", "Sam"),
                consensusScore = "92% Match",
                date = "Oct 18, 8:00 PM"
            )
        )
    }

    var selectedPlan by remember { mutableStateOf<PlanRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dinner Plans & History", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    com.plansguru.ui.common.GuruTopBarAction(onOpenGuruChat)
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

            Text("PREVIOUS DINNER PLANS · ${previousPlans.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(previousPlans, key = { it.id }) { plan ->
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
                            Text("Participants: ${plan.participants.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Completed: ${plan.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
                    TextButton(
                        onClick = {
                            previousPlans.remove(selectedPlan)
                            selectedPlan = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Plan History")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = previousPlans.indexOfFirst { it.id == selectedPlan!!.id }
                        if (idx != -1) {
                            previousPlans[idx] = selectedPlan!!.copy(title = editableTitle.trim())
                        }
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
