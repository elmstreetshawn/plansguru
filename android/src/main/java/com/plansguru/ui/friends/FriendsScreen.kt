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
                title = { Text("Your people • PlansGuru") },
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
                    Text("Less planning. More doing.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Coordinate privately with verified peers using local, encrypted social graphs.", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(
                value = inviteCodeInput,
                onValueChange = { inviteCodeInput = it },
                label = { Text("Enter Invitation Code / QR Peer ID") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Button(
                onClick = {
                    if (inviteCodeInput.isNotBlank()) {
                        viewModel.addFriend(inviteCodeInput)
                        inviteCodeInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Pair with Friend")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("Connected Dinner Group (4 People)", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friends) { friend ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Peer: ${friend.relatedPersonId.replace("friend_", "").replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Trust State: ${friend.trustState}", style = MaterialTheme.typography.bodyMedium)
                            Text("Sharing Policy: ${friend.sharingPermissions}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
