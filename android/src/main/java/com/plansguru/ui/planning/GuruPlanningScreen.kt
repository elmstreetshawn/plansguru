package com.plansguru.ui.planning

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun rememberAssetImage(fileName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(fileName) {
        try {
            context.assets.open(fileName).use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruPlanningScreen(
    groupTitle: String,
    members: List<String>,
    viewModel: PlanningViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onPlanningComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val clarificationPerson by viewModel.clarificationNeededPerson.collectAsState()
    val coordinationStatus by viewModel.coordinationStatus.collectAsState()

    var clarificationInput by remember { mutableStateOf("") }
    var check1 by remember { mutableStateOf(false) }
    var check2 by remember { mutableStateOf(false) }
    var check3 by remember { mutableStateOf(false) }

    LaunchedEffect(members) {
        viewModel.checkAndPlan(members)
    }

    LaunchedEffect(coordinationStatus) {
        if (coordinationStatus.contains("Consensus Reached")) {
            check1 = true
            check2 = true
            check3 = true
            delay(500)
            onPlanningComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mascot illustration from assets
                val bitmap = rememberAssetImage("expression_thinking.png")
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Guru Mascot",
                            modifier = Modifier.size(120.dp)
                        )
                    } else {
                        Text("🧙‍♂️✨", style = MaterialTheme.typography.displayLarge)
                    }
                }

                Text("Your Guru is planning...", style = MaterialTheme.typography.headlineMedium)
                Text("For $groupTitle ($coordinationStatus)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                // Animated checklist
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChecklistItem("Everyone's free times", check1)
                    ChecklistItem("Tastes and diets", check2)
                    ChecklistItem("Places within everyone's travel time", check3)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🔒 Your notes stay on your phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = onPlanningComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("Skip / Force Recommendations →", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Clarification Dialog if knowledge threshold isn't met for any participant
    if (clarificationPerson != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Clarification Needed for $clarificationPerson") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("We don't have enough preference data recorded for $clarificationPerson yet. What do they like or dislike to eat?")
                    OutlinedTextField(
                        value = clarificationInput,
                        onValueChange = { clarificationInput = it },
                        placeholder = { Text("e.g. Loves Japanese, hates spicy") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clarificationInput.isNotBlank()) {
                            viewModel.provideClarification(clarificationPerson!!, clarificationInput.trim()) {
                                viewModel.checkAndPlan(members)
                            }
                            clarificationInput = ""
                        }
                    },
                    shape = CircleShape
                ) {
                    Text("Save & Resume Planning")
                }
            }
        )
    }
}

@Composable
fun ChecklistItem(text: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
