package com.plansguru.ui.places

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.data.model.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class PlacesViewModel(application: Application) : AndroidViewModel(application) {
    private val _restaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val restaurants: StateFlow<List<Restaurant>> = _restaurants.asStateFlow()

    init {
        loadRestaurants()
    }

    private fun loadRestaurants() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val jsonString = context.assets.open("restaurant_dataset.json").bufferedReader().use { it.readText() }
                val root = Json { ignoreUnknownKeys = true }.decodeFromString<RestaurantDatasetRoot>(jsonString)
                _restaurants.value = root.restaurants
            } catch (e: Exception) {
                _restaurants.value = emptyList()
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class RestaurantDatasetRoot(
    val restaurants: List<Restaurant>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    viewModel: PlacesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val restaurants by viewModel.restaurants.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = restaurants.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.cuisine.contains(searchQuery, ignoreCase = true) ||
        it.location.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Places", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cuisine, restaurant, or location") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            Text("LOCAL RESTAURANT DATASET · ${filtered.size} PLACES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { restaurant ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(restaurant.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text(restaurant.priceRange, style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cuisine: ${restaurant.cuisine}", style = MaterialTheme.typography.bodyMedium)
                            Text("Location: ${restaurant.location} • Noise: ${restaurant.noiseLevel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                restaurant.dietaryAccommodations.take(3).forEach { accommodation ->
                                    AssistChip(onClick = {}, label = { Text(accommodation, style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
