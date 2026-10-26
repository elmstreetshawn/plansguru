package com.plansguru

import com.plansguru.agent.NegotiationEngine
import com.plansguru.data.model.Assertion
import com.plansguru.data.model.Restaurant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NegotiationEngineTest {

    @Test
    fun testNegotiationEngineEvaluation() {
        val engine = NegotiationEngine()
        val restaurants = listOf(
            Restaurant(
                id = "r1",
                name = "Green Leaf Bistro",
                cuisine = "Vegan",
                priceRange = "$$",
                vegetarianOptions = true,
                dietaryAccommodations = listOf("Vegan", "Gluten-Free"),
                noiseLevel = "Quiet",
                crowdLevel = "Moderate",
                location = "Downtown",
                openingHours = "11:00 - 22:00",
                accessibilityAttributes = listOf("Wheelchair Accessible")
            ),
            Restaurant(
                id = "r2",
                name = "Sizzling Steakhouse",
                cuisine = "Steakhouse",
                priceRange = "$$$$",
                vegetarianOptions = false,
                dietaryAccommodations = emptyList(),
                noiseLevel = "Loud",
                crowdLevel = "Busy",
                location = "Downtown",
                openingHours = "16:00 - 23:30",
                accessibilityAttributes = listOf("Wheelchair Accessible")
            )
        )

        val participantAssertions = mapOf(
            "p1" to listOf(Assertion("a1", "p1", "p1", "dietary_restriction", "Vegan", "SELF", 1.0f, 0L, null, "AUTH")),
            "p2" to listOf(Assertion("a2", "p2", "p2", "likes_cuisine", "Vegan", "SELF", 1.0f, 0L, null, "AUTH"))
        )

        val results = engine.evaluateCandidates(restaurants, participantAssertions)
        assertEquals(2, results.size)
        // Green Leaf Bistro should pass hard constraints (Vegan) and be ranked first
        assertEquals("Green Leaf Bistro", results[0].restaurant.name)
        assertTrue(results[0].hardConstraintPassed)
        // Steakhouse should fail hard constraints for Vegan participant
        assertEquals("Sizzling Steakhouse", results[1].restaurant.name)
        assertTrue(!results[1].hardConstraintPassed)
    }
}
