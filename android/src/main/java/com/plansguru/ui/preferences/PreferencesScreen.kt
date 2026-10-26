package com.plansguru.ui.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
                title = { Text("My Preferences & Assertions") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = predicateInput,
                onValueChange = { predicateInput = it },
                label = { Text("Predicate (e.g. likes_cuisine, dietary_restriction)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = valueInput,
                onValueChange = { valueInput = it },
                label = { Text("Value (e.g. Italian, Gluten-Free)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (valueInput.isNotBlank()) {
                        viewModel.addPreference(predicateInput, valueInput)
                        valueInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Preference Assertion")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Stored Local Assertions", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(assertions) { assertion ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Predicate: ${assertion.predicate}", style = MaterialTheme.typography.bodyMedium)
                            Text("Value: ${assertion.value}", style = MaterialTheme.typography.bodyLarge)
                            Text("Source: ${assertion.source} (Confidence: ${assertion.confidence})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
