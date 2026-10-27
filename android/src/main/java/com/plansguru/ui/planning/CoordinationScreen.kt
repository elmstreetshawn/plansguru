package com.plansguru.ui.planning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinationScreen(
    viewModel: PlanningViewModel,
    onNavigateRecommendations: () -> Unit
) {
    val status by viewModel.coordinationStatus.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startDinnerPlanning()
    }

    LaunchedEffect(recommendations) {
        if (recommendations.isNotEmpty()) {
            onNavigateRecommendations()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Coordination in Progress") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = status,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Independent agents are negotiating preferences and hard constraints privately across encrypted local graphs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
