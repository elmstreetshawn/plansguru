package com.plansguru.ui.planning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlanScreen(
    onStartCoordination: (String) -> Unit,
    onBack: () -> Unit
) {
    var promptInput by remember { mutableStateOf("Find somewhere for Mike, Sarah, Jen, and me to eat Friday.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("One plan • PlansGuru") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Less planning. More doing.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Independent AI agents negotiate across your encrypted social graph to find the perfect spot.", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                label = { Text("What are we planning?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Participants (Dinner Group)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Me (PlansGuru Owner)\n• Mike (Verified Peer)\n• Sarah (Verified Peer)\n• Jen (Verified Peer)")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onStartCoordination(promptInput) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Start Multi-Agent Coordination")
            }
        }
    }
}
