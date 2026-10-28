package com.plansguru.ui.places

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.data.model.Restaurant
import com.plansguru.data.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.abs

fun calculateZipDistanceMiles(fromZip: String, toZip: String): Double {
    val cleanFrom = fromZip.trim()
    val cleanTo = toZip.trim()
    if (cleanFrom == cleanTo) return 0.8
    val fromNum = cleanFrom.toIntOrNull() ?: 90210
    val toNum = cleanTo.toIntOrNull() ?: 90210
    val diff = abs(fromNum - toNum)
    return when {
        diff == 0 -> 0.8
        diff <= 5 -> 1.8 + (diff * 0.4)
        diff <= 20 -> 4.5 + (diff * 0.2)
        else -> 10.0 + (diff * 0.1)
    }
}

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

    fun addRestaurant(restaurant: Restaurant) {
        _restaurants.value = listOf(restaurant) + _restaurants.value
    }

    fun updateRestaurant(updated: Restaurant) {
        _restaurants.value = _restaurants.value.map { if (it.id == updated.id) updated else it }
    }

    fun deleteRestaurant(restaurantId: String) {
        _restaurants.value = _restaurants.value.filter { it.id != restaurantId }
    }
}

@kotlinx.serialization.Serializable
data class RestaurantDatasetRoot(
    val restaurants: List<Restaurant>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    viewModel: PlacesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onOpenGuruChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { GraphRepository(context) }
    var userZip by remember { mutableStateOf("90210") }

    LaunchedEffect(Unit) {
        try {
            val userPerson = repository.getPerson("local_user")
            if (userPerson != null && userPerson.locationZip.isNotBlank()) {
                userZip = userPerson.locationZip
            }
        } catch (_: Exception) {}
    }

    val restaurants by viewModel.restaurants.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    val filtered = restaurants.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.cuisine.contains(searchQuery, ignoreCase = true) ||
        it.location.contains(searchQuery, ignoreCase = true) ||
        it.address.contains(searchQuery, ignoreCase = true) ||
        it.zipCode.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Places", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    com.plansguru.ui.common.GuruTopBarAction(onOpenGuruChat)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPlaceDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Place")
            }
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
                placeholder = { Text("Search cuisine, address, zip code, or name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            Text("PLACES NEAR YOUR ZIP ($userZip) · ${filtered.size} AVAILABLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { restaurant ->
                    val distanceMiles = calculateZipDistanceMiles(userZip, restaurant.zipCode)
                    val driveMins = (distanceMiles * 2.5).toInt().coerceAtLeast(3)
                    val distFormatted = String.format("%.1f", distanceMiles)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRestaurant = restaurant },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(restaurant.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(restaurant.priceRange, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Text("Cuisine: ${restaurant.cuisine}", style = MaterialTheme.typography.bodyMedium)
                            Text("📍 ${restaurant.address} (Zip: ${restaurant.zipCode})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                                Text(
                                    "🚗 $distFormatted mi · $driveMins min drive from $userZip",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
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

    // Add Place Dialog
    if (showAddPlaceDialog) {
        var name by remember { mutableStateOf("") }
        var cuisine by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var zipCode by remember { mutableStateOf(userZip) }
        var price by remember { mutableStateOf("$$") }
        var noise by remember { mutableStateOf("Moderate") }

        AlertDialog(
            onDismissRequest = { showAddPlaceDialog = false },
            title = { Text("Add New Place") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Restaurant Name") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = cuisine, onValueChange = { cuisine = it }, label = { Text("Cuisine (e.g. Italian)") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address (e.g. 450 N Canon Dr)") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = zipCode, onValueChange = { zipCode = it }, label = { Text("Zip Code (e.g. 90210)") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price Range (e.g. $$, $$$)") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = noise, onValueChange = { noise = it }, label = { Text("Noise Level (Quiet, Moderate, Loud)") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && cuisine.isNotBlank()) {
                            val newRest = Restaurant(
                                id = "rest_${System.currentTimeMillis()}",
                                name = name.trim(),
                                cuisine = cuisine.trim(),
                                priceRange = price.trim(),
                                vegetarianOptions = true,
                                dietaryAccommodations = listOf("Vegan", "Gluten-Free"),
                                noiseLevel = noise.trim(),
                                crowdLevel = "Moderate",
                                location = address.trim().ifEmpty { "Downtown" },
                                openingHours = "11:00 - 22:00",
                                accessibilityAttributes = listOf("Wheelchair Accessible"),
                                address = address.trim().ifEmpty { "450 N Canon Dr" },
                                zipCode = zipCode.trim().ifEmpty { userZip }
                            )
                            viewModel.addRestaurant(newRest)
                            showAddPlaceDialog = false
                        }
                    },
                    shape = CircleShape
                ) {
                    Text("Add Place")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit / Delete Place & Tags Dialog
    if (selectedRestaurant != null) {
        var name by remember { mutableStateOf(selectedRestaurant!!.name) }
        var cuisine by remember { mutableStateOf(selectedRestaurant!!.cuisine) }
        var address by remember { mutableStateOf(selectedRestaurant!!.address) }
        var zipCode by remember { mutableStateOf(selectedRestaurant!!.zipCode) }
        var price by remember { mutableStateOf(selectedRestaurant!!.priceRange) }
        var noise by remember { mutableStateOf(selectedRestaurant!!.noiseLevel) }
        val accommodations = remember { mutableStateListOf<String>().apply { addAll(selectedRestaurant!!.dietaryAccommodations) } }
        var newTagInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedRestaurant = null },
            title = { Text("Edit Place & Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Restaurant Name") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = cuisine, onValueChange = { cuisine = it }, label = { Text("Cuisine") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = zipCode, onValueChange = { zipCode = it }, label = { Text("Zip Code") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price Range") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = noise, onValueChange = { noise = it }, label = { Text("Noise Level") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dietary Tags & Accommodations:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (accommodations.isEmpty()) {
                        Text("No tags recorded yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        accommodations.forEach { tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• $tag", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { accommodations.remove(tag) }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        label = { Text("Add tag (e.g. Nut-Free)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Button(
                        onClick = {
                            if (newTagInput.isNotBlank() && !accommodations.contains(newTagInput.trim())) {
                                accommodations.add(newTagInput.trim())
                                newTagInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape
                    ) {
                        Text("Add Tag")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.deleteRestaurant(selectedRestaurant!!.id)
                            selectedRestaurant = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Place")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = selectedRestaurant!!.copy(
                            name = name.trim(),
                            cuisine = cuisine.trim(),
                            address = address.trim(),
                            zipCode = zipCode.trim(),
                            location = address.trim(),
                            priceRange = price.trim(),
                            noiseLevel = noise.trim(),
                            dietaryAccommodations = accommodations.toList()
                        )
                        viewModel.updateRestaurant(updated)
                        selectedRestaurant = null
                    },
                    shape = CircleShape
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRestaurant = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
