package com.plansguru.ui.friends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onBack: () -> Unit
) {
    val friends by viewModel.friendsState.collectAsState()
    var inviteCodeInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends & Peer Network") },
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
                value = inviteCodeInput,
                onValueChange = { inviteCodeInput = it },
                label = { Text("Enter Invitation Code / QR Peer ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (inviteCodeInput.isNotBlank()) {
                        viewModel.addFriend(inviteCodeInput)
                        inviteCodeInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pair with Friend")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Connected Friends (Dinner Group for 4)", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friends) { friend ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Friend ID: ${friend.relatedPersonId}", style = MaterialTheme.typography.titleSmall)
                            Text("Trust State: ${friend.trustState}", style = MaterialTheme.typography.bodyMedium)
                            Text("Sharing: ${friend.sharingPermissions}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
