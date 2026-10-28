package com.plansguru.agent

import com.plansguru.data.model.AgentProposal
import com.plansguru.data.model.Assertion
import com.plansguru.data.model.Restaurant
import kotlin.math.max
import kotlin.math.min

class NegotiationEngine {

    data class EvaluationResult(
        val restaurant: Restaurant,
        val minSatisfaction: Float,
        val avgSatisfaction: Float,
        val hardConstraintPassed: Boolean
    )

    fun evaluateCandidates(
        restaurants: List<Restaurant>,
        participantAssertions: Map<String, List<Assertion>>
    ): List<EvaluationResult> {
        val results = mutableListOf<EvaluationResult>()

        for (restaurant in restaurants) {
            var hardPassed = true
            val satisfactionScores = mutableListOf<Float>()

            for ((participantId, assertions) in participantAssertions) {
                // 1. Check Hard Constraints (Dietary restrictions / Allergies / Disliked Cuisines)
                val dietaryRestrictions = assertions.filter { it.predicate == "dietary_restriction" }
                for (restriction in dietaryRestrictions) {
                    val rVal = restriction.value.lowercase()
                    if (rVal.contains("vegan") && !restaurant.dietaryAccommodations.any { it.contains("Vegan", true) } && !restaurant.vegetarianOptions) {
                        hardPassed = false
                    }
                    if (rVal.contains("vegetarian") && !restaurant.vegetarianOptions && !restaurant.dietaryAccommodations.any { it.contains("Vegetarian", true) || it.contains("Vegan", true) }) {
                        hardPassed = false
                    }
                    if (rVal.contains("gluten") && !restaurant.dietaryAccommodations.any { it.contains("Gluten-Free", true) }) {
                        hardPassed = false
                    }
                    if (rVal.contains("nut") && !restaurant.dietaryAccommodations.any { it.contains("Nut-Free", true) }) {
                        hardPassed = false
                    }
                    if (rVal.contains("halal") && !restaurant.dietaryAccommodations.any { it.contains("Halal", true) }) {
                        hardPassed = false
                    }
                }

                // Check Disliked Cuisines (Hard dealbreaker)
                val dislikedCuisines = assertions.filter { it.predicate == "dislikes_cuisine" }
                if (dislikedCuisines.any { restaurant.cuisine.contains(it.value, ignoreCase = true) }) {
                    hardPassed = false
                }

                // 2. Calculate Soft Preferences Satisfaction Score (0.0 to 1.0)
                var score = 0.3f // Base score

                // Cuisine Affinity (+0.30)
                val preferredCuisines = assertions.filter { it.predicate == "likes_cuisine" }
                if (preferredCuisines.any { restaurant.cuisine.contains(it.value, ignoreCase = true) }) {
                    score += 0.30f
                }

                // Distance / Location Matching (+0.20)
                val distancePrefs = assertions.filter { it.predicate == "max_distance" }
                if (distancePrefs.isNotEmpty()) {
                    if (distancePrefs.any { it.value.lowercase().contains(restaurant.location.lowercase()) || it.value.lowercase().contains("close") || it.value.lowercase().contains("travel") || it.value.lowercase().contains("near") }) {
                        score += 0.20f
                    }
                } else {
                    score += 0.10f
                }

                // Noise & Crowd Vibe Matching (+0.20)
                val noisePrefs = assertions.filter { it.predicate == "noise_preference" }
                if (noisePrefs.isNotEmpty()) {
                    val userPrefersLoud = noisePrefs.any { it.value.lowercase().contains("loud") || it.value.lowercase().contains("crowd") || it.value.lowercase().contains("lively") || it.value.lowercase().contains("busy") }
                    val userPrefersQuiet = noisePrefs.any { it.value.lowercase().contains("quiet") || it.value.lowercase().contains("peaceful") || it.value.lowercase().contains("calm") }

                    if ((userPrefersLoud && (restaurant.noiseLevel.equals("Loud", true) || restaurant.crowdLevel.equals("Busy", true) || restaurant.crowdLevel.equals("Packed", true))) ||
                        (userPrefersQuiet && (restaurant.noiseLevel.equals("Quiet", true) || restaurant.crowdLevel.equals("Quiet", true))) ||
                        (!userPrefersLoud && !userPrefersQuiet && restaurant.noiseLevel.equals("Moderate", true))) {
                        score += 0.20f
                    }
                } else {
                    score += 0.10f
                }

                // Budget / Price Tier Matching (+0.10)
                val budgetPrefs = assertions.filter { it.predicate == "budget" || it.predicate == "price" }
                if (budgetPrefs.any { restaurant.priceRange.contains(it.value) } || restaurant.priceRange == "$$") {
                    score += 0.10f
                }

                satisfactionScores.add(min(1.0f, max(0.0f, score)))
            }

            val minSat = if (satisfactionScores.isNotEmpty()) satisfactionScores.minOrNull() ?: 0f else 0f
            val avgSat = if (satisfactionScores.isNotEmpty()) satisfactionScores.average().toFloat() else 0f

            results.add(
                EvaluationResult(
                    restaurant = restaurant,
                    minSatisfaction = minSat,
                    avgSatisfaction = avgSat,
                    hardConstraintPassed = hardPassed
                )
            )
        }

        // Sort: hard constraint passed first, then maximize minSatisfaction, then avgSatisfaction
        return results.sortedWith(
            compareByDescending<EvaluationResult> { it.hardConstraintPassed }
                .thenByDescending { it.minSatisfaction }
                .thenByDescending { it.avgSatisfaction }
        )
    }
}
