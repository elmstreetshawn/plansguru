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
                title = { Text("New Coordination Plan") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
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
            Text("What would you like to coordinate?", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                label = { Text("Request / Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Participants", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Me (PlansGuru Owner)\n• Mike (Verified Peer)\n• Sarah (Verified Peer)\n• Jen (Verified Peer)")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onStartCoordination(promptInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Multi-Agent Coordination")
            }
        }
    }
}
