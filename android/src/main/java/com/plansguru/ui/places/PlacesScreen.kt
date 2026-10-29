package com.plansguru.ui.places

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

    val zipCoords = mapOf(
        "78626" to Pair(30.6332, -97.6772), // Georgetown
        "78681" to Pair(30.5083, -97.6789), // Round Rock
        "78664" to Pair(30.5086, -97.6534), // Round Rock
        "78758" to Pair(30.3892, -97.7121), // Domain / North Austin
        "78753" to Pair(30.3802, -97.6785), // North Austin
        "78751" to Pair(30.3092, -97.7210), // Hyde Park
        "78705" to Pair(30.2915, -97.7400), // UT / Central
        "78701" to Pair(30.2672, -97.7431), // Downtown Austin
        "78702" to Pair(30.2638, -97.7171), // East Austin
        "78704" to Pair(30.2458, -97.7600), // South Congress / SoLam
        "78748" to Pair(30.1652, -97.8285), // Manchaca / South Austin
        "78652" to Pair(30.1328, -97.8386), // Manchaca
        "90210" to Pair(30.2672, -97.7431)  // Default to Downtown Austin
    )

    val p1 = zipCoords[cleanFrom] ?: zipCoords["90210"]!!
    val p2 = zipCoords[cleanTo] ?: Pair(30.2672, -97.7431)

    val lat1 = Math.toRadians(p1.first)
    val lon1 = Math.toRadians(p1.second)
    val lat2 = Math.toRadians(p2.first)
    val lon2 = Math.toRadians(p2.second)

    val dlat = lat2 - lat1
    val dlon = lon2 - lon1

    val a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
            Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    val miles = 3958.8 * c
    return miles.coerceAtLeast(0.8)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
            val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
            val userId = prefs.getString("user_id", "local_user") ?: "local_user"
            val userPerson = repository.getPerson(userId)
            if (userPerson != null && userPerson.locationZip.isNotBlank()) {
                userZip = userPerson.locationZip
            }
        } catch (_: Exception) {}
    }

    val restaurants by viewModel.restaurants.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var activeFilterChip by remember { mutableStateOf("All") }
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    val filtered = restaurants.filter { rest ->
        val matchesSearch = rest.name.contains(searchQuery, ignoreCase = true) ||
                rest.cuisine.contains(searchQuery, ignoreCase = true) ||
                rest.location.contains(searchQuery, ignoreCase = true) ||
                rest.address.contains(searchQuery, ignoreCase = true) ||
                rest.zipCode.contains(searchQuery, ignoreCase = true)

        val distMiles = calculateZipDistanceMiles(userZip, rest.zipCode)
        val driveMins = (distMiles * 2.5).toInt().coerceAtLeast(3)

        val matchesFilter = when (activeFilterChip) {
            "Vegan" -> rest.dietaryAccommodations.any { it.contains("Vegan", true) } || rest.vegetarianOptions
            "Gluten-Free" -> rest.dietaryAccommodations.any { it.contains("Gluten", true) }
            "< 15 Min" -> driveMins <= 15
            "< 30 Min" -> driveMins <= 30
            "Quiet" -> rest.noiseLevel.equals("Quiet", true) || rest.crowdLevel.equals("Quiet", true)
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Places", style = MaterialTheme.typography.headlineMedium)
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("v1.2.0", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
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

            // 1-Tap Quick Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val chips = listOf(
                    "All" to "All Places",
                    "Vegan" to "🌱 Vegan",
                    "Gluten-Free" to "🌾 Gluten-Free",
                    "< 15 Min" to "🚗 < 15 Min",
                    "< 30 Min" to "🚗 < 30 Min",
                    "Quiet" to "🤫 Quiet Spots"
                )
                items(chips) { (filterKey, filterLabel) ->
                    FilterChip(
                        selected = activeFilterChip == filterKey,
                        onClick = { activeFilterChip = filterKey },
                        label = { Text(filterLabel, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

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
                    var showPlaceMenu by remember { mutableStateOf(false) }

                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { selectedRestaurant = restaurant },
                                    onLongClick = { showPlaceMenu = true }
                                ),
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

                        DropdownMenu(
                            expanded = showPlaceMenu,
                            onDismissRequest = { showPlaceMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🗺️ Open in Google Maps") },
                                onClick = {
                                    showPlaceMenu = false
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(restaurant.name + ", " + restaurant.address)}")
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("✏️ Edit Place Details") },
                                onClick = {
                                    showPlaceMenu = false
                                    selectedRestaurant = restaurant
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🗑️ Delete Place", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showPlaceMenu = false
                                    viewModel.deleteRestaurant(restaurant.id)
                                }
                            )
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
