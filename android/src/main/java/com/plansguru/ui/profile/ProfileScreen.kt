package com.plansguru.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigatePreferences: () -> Unit,
    onNavigateFriends: () -> Unit,
    onNavigateNewPlan: () -> Unit
) {
    val profile by viewModel.profileState.collectAsState()
    var nameInput by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        if (profile != null && nameInput.isEmpty()) {
            nameInput = profile!!.displayName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PlansGuru — Profile & Identity") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (profile == null) {
                CircularProgressIndicator()
            } else {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.updateDisplayName(nameInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Profile")
                }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cryptographic Identity Key", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(profile!!.publicIdentityKey, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onNavigatePreferences,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Manage Preferences")
                }

                Button(
                    onClick = onNavigateFriends,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Friends & Peer Network")
                }

                Button(
                    onClick = onNavigateNewPlan,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Plan Dinner for 4 (New Plan)")
                }
            }
        }
    }
}
