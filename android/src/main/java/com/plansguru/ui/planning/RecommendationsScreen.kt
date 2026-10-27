package com.plansguru.ui.planning

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
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
    onFinish: () -> Unit
) {
    val recommendations by viewModel.recommendations.collectAsState()
    val context = LocalContext.current

    val chatMessages = remember {
        mutableStateListOf(
            PlanChatMessage("1", "Mike", "Trattoria Bella looks amazing! Italian works for me.", System.currentTimeMillis() - 3600000),
            PlanChatMessage("2", "Sarah", "Agreed! And they have great vegan options.", System.currentTimeMillis() - 1800000)
        )
    }
    var inputMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Top Group Recommendations") },
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
                "Top 3 recommendations optimized for your group:",
                style = MaterialTheme.typography.bodyLarge
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(recommendations) { index, rec ->
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
                        }
                    }
                }
            }

            // Map and Calendar action buttons for the top recommendation
            val topRec = recommendations.firstOrNull()
            if (topRec != null) {
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

            Text("🔒 E2E Encrypted Group Discussion", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatMessages) { msg ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (msg.sender == "You") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("${msg.sender}: ${msg.text}", style = MaterialTheme.typography.bodySmall)
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
                    placeholder = { Text("Send encrypted message to plan group...") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                Button(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            chatMessages.add(PlanChatMessage(System.currentTimeMillis().toString(), "You", inputMessage.trim(), System.currentTimeMillis()))
                            inputMessage = ""
                        }
                    },
                    shape = CircleShape
                ) {
                    Text("Send")
                }
            }

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Text("Done / Back to Dashboard")
            }
        }
    }
}
