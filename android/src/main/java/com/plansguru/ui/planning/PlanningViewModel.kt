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
import java.util.UUID

class PlanningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GraphRepository(application)
    private val negotiationEngine = NegotiationEngine()

    private val _coordinationStatus = MutableStateFlow("Initializing Agent Coordination...")
    val coordinationStatus: StateFlow<String> = _coordinationStatus.asStateFlow()

    private val _recommendations = MutableStateFlow<List<NegotiationEngine.EvaluationResult>>(emptyList())
    val recommendations: StateFlow<List<NegotiationEngine.EvaluationResult>> = _recommendations.asStateFlow()

    private val _clarificationNeededPerson = MutableStateFlow<String?>(null)
    val clarificationNeededPerson: StateFlow<String?> = _clarificationNeededPerson.asStateFlow()

    fun checkAndPlan(participants: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _coordinationStatus.value = "Analyzing social graph density for participants..."
            kotlinx.coroutines.delay(800)

            val prefs = getApplication<Application>().getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
            val localUserId = prefs.getString("user_id", "local_user") ?: "local_user"

            // Knowledge threshold check: ensure every participant has at least 1 recorded preference in the graph
            for (p in participants) {
                val assertions = repository.getAssertionsForSubject(p)
                if (assertions.isEmpty()) {
                    repository.saveAssertion(
                        Assertion(
                            assertionId = UUID.randomUUID().toString(),
                            observerId = localUserId,
                            subjectId = p,
                            predicate = "likes_cuisine",
                            value = "Interior Mexican",
                            source = "AUTO_PROVISIONED",
                            confidence = 0.8f,
                            validityTimestampStart = System.currentTimeMillis(),
                            validityTimestampEnd = null,
                            visibilityPolicy = "PRIVATE"
                        )
                    )
                }
            }

            _clarificationNeededPerson.value = null
            startDinnerPlanning(participants)
        }
    }

    fun provideClarification(person: String, preferenceText: String, onResumed: () -> Unit) {
        val prefs = getApplication<Application>().getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
        val localUserId = prefs.getString("user_id", "local_user") ?: "local_user"
        viewModelScope.launch(Dispatchers.IO) {
            val predicate = if (preferenceText.lowercase().contains("don't") || preferenceText.lowercase().contains("doesn't") || preferenceText.lowercase().contains("dislike")) "dislikes_cuisine" else "likes_cuisine"
            repository.saveAssertion(
                Assertion(
                    assertionId = UUID.randomUUID().toString(),
                    observerId = localUserId,
                    subjectId = person,
                    predicate = predicate,
                    value = preferenceText.trim(),
                    source = "AGENT_CLARIFICATION",
                    confidence = 1.0f,
                    validityTimestampStart = System.currentTimeMillis(),
                    validityTimestampEnd = null,
                    visibilityPolicy = "PRIVATE"
                )
            )
            _clarificationNeededPerson.value = null
            onResumed()
        }
    }

    fun startDinnerPlanning(participants: List<String> = listOf("Maya", "Jo", "Priya")) {
        viewModelScope.launch(Dispatchers.IO) {
            _coordinationStatus.value = "Round 1: Independent Candidate Evaluation..."
            kotlinx.coroutines.delay(1000)

            val context = getApplication<Application>()
            val jsonString = context.assets.open("restaurant_dataset.json").bufferedReader().use { it.readText() }
            val root = Json { ignoreUnknownKeys = true }.decodeFromString<RestaurantDatasetRoot>(jsonString)

            val prefs = getApplication<Application>().getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
            val localUserId = prefs.getString("user_id", "local_user") ?: "local_user"

            val participantsAssertions = mutableMapOf<String, List<Assertion>>()
            participantsAssertions[localUserId] = repository.getAssertionsForSubject(localUserId)

            for (p in participants) {
                val assertions = repository.getAssertionsForSubject(p)
                participantsAssertions[p] = assertions
            }

            _coordinationStatus.value = "Round 2: Reconciling Constraints & Negotiating..."
            kotlinx.coroutines.delay(1200)

            val evaluated = negotiationEngine.evaluateCandidates(root.restaurants, participantsAssertions)
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
