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
                // Check hard constraints (e.g. dietary restrictions)
                val dietaryRestrictions = assertions.filter { it.predicate == "dietary_restriction" }
                for (restriction in dietaryRestrictions) {
                    if (restriction.value.equals("Vegan", ignoreCase = true) && !restaurant.dietaryAccommodations.contains("Vegan") && !restaurant.vegetarianOptions) {
                        hardPassed = false
                    }
                    if (restriction.value.equals("Gluten-Free", ignoreCase = true) && !restaurant.dietaryAccommodations.any { it.contains("Gluten-Free", ignoreCase = true) }) {
                        hardPassed = false
                    }
                }

                // Calculate participant satisfaction score (0.0 to 1.0)
                var score = 0.5f
                val preferredCuisines = assertions.filter { it.predicate == "likes_cuisine" }
                if (preferredCuisines.any { restaurant.cuisine.contains(it.value, ignoreCase = true) }) {
                    score += 0.4f
                }
                if (restaurant.priceRange == "$$") {
                    score += 0.1f
                }
                satisfactionScores.add(min(1.0f, score))
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
