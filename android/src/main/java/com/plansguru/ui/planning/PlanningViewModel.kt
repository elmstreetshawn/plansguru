package com.plansguru.ui.planning

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.agent.NegotiationEngine
import com.plansguru.data.model.Assertion
import com.plansguru.data.model.Restaurant
import com.plansguru.data.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.`io`.InputStreamReader

class PlanningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GraphRepository(application)
    private val negotiationEngine = NegotiationEngine()

    private val _coordinationStatus = MutableStateFlow("Initializing Agent Coordination...")
    val coordinationStatus: StateFlow<String> = _coordinationStatus.asStateFlow()

    private val _recommendations = MutableStateFlow<List<NegotiationEngine.EvaluationResult>>(emptyList())
    val recommendations: StateFlow<List<NegotiationEngine.EvaluationResult>> = _recommendations.asStateFlow()

    fun startDinnerPlanning() {
        viewModelScope.launch(Dispatchers.IO) {
            _coordinationStatus.value = "Round 1: Independent Candidate Evaluation..."
            kotlinx.coroutines.delay(1000)

            // Load 20 restaurants from assets
            val context = getApplication<Application>()
            val jsonString = context.assets.open("restaurant_dataset.json").bufferedReader().use { it.readText() }
            val root = Json { ignoreUnknownKeys = true }.decodeFromString<RestaurantDatasetRoot>(jsonString)

            // Gather mock participant assertions for Mike, Sarah, Jen, and me
            val participantsAssertions = mapOf(
                "local_user" to repository.getAssertionsForSubject("local_user"),
                "friend_mike" to listOf(Assertion("a2", "friend_mike", "friend_mike", "likes_cuisine", "Italian", "SELF", 1.0f, 0L, null, "AUTHORIZED")),
                "friend_sarah" to listOf(Assertion("a3", "friend_sarah", "friend_sarah", "dietary_restriction", "Vegan", "SELF", 1.0f, 0L, null, "AUTHORIZED")),
                "friend_jen" to listOf(Assertion("a4", "friend_jen", "friend_jen", "likes_cuisine", "Japanese", "SELF", 1.0f, 0L, null, "AUTHORIZED"))
            )

            _coordinationStatus.value = "Round 2: Reconciling Constraints & Negotiating..."
            kotlinx.coroutines.delay(1200)

            val evaluated = negotiationEngine.evaluateCandidates(root.restaurants, participantsAssertions)
            // Take top 3 recommendations
            val top3 = evaluated.take(3)

            _recommendations.value = top3
            _coordinationStatus.value = "Consensus Reached! Top 3 Recommendations Computed."
        }
    }
}

@kotlinx.serialization.Serializable
data class RestaurantDatasetRoot(
    val restaurants: List<Restaurant>
)
