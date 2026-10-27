package com.plansguru.ui.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    viewModel: PreferencesViewModel,
    onBack: () -> Unit
) {
    val assertions by viewModel.assertionsState.collectAsState()
    var predicateInput by remember { mutableStateOf("likes_cuisine") }
    var valueInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your preferences • PlansGuru") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Self-declared & Inferred Taste", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Your preferences stay encrypted on your device and are only evaluated during private agent negotiation.", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(
                value = predicateInput,
                onValueChange = { predicateInput = it },
                label = { Text("Predicate (e.g. likes_cuisine, dietary_restriction)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = valueInput,
                onValueChange = { valueInput = it },
                label = { Text("Value (e.g. Italian, Gluten-Free)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Button(
                onClick = {
                    if (valueInput.isNotBlank()) {
                        viewModel.addPreference(predicateInput, valueInput)
                        valueInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Add Preference Assertion")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("Stored Local Assertions", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(assertions) { assertion ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("${assertion.predicate}: ${assertion.value}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Source: ${assertion.source} • Confidence: ${(assertion.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
