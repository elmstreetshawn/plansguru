package com.plansguru.ui.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.plansguru.agent.NegotiationEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    viewModel: PlanningViewModel,
    onFinish: () -> Unit
) {
    val recommendations by viewModel.recommendations.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Here are the top 3 recommendations optimized for Mike, Sarah, Jen, and you:",
                style = MaterialTheme.typography.bodyLarge
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(recommendations) { index, rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("#${index + 1}: ${rec.restaurant.name}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cuisine: ${rec.restaurant.cuisine} | Price: ${rec.restaurant.priceRange}", style = MaterialTheme.typography.bodyMedium)
                            Text("Location: ${rec.restaurant.location} | Hours: ${rec.restaurant.openingHours}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Group Min Satisfaction: ${(rec.minSatisfaction * 100).toInt()}% (Avg: ${(rec.avgSatisfaction * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium)
                            Text("Hard Constraints Passed: ${rec.hardConstraintPassed}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done / Back to Profile")
            }
        }
    }
}
