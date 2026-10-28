package com.plansguru.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProtocolMessage(
    val type: String, // HELLO, GRAPH_UPDATE, PLAN_REQUEST, PROPOSAL, CHALLENGE, REVISE, CONSENSUS, ACK
    val senderId: String,
    val sessionId: String,
    val payload: String, // JSON string of specific payload
    val signature: String,
    val timestamp: Long
)

@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val cuisine: String,
    val priceRange: String,
    val vegetarianOptions: Boolean,
    val dietaryAccommodations: List<String>,
    val noiseLevel: String,
    val crowdLevel: String,
    val location: String,
    val openingHours: String,
    val accessibilityAttributes: List<String>,
    val address: String = "450 N Canon Dr, Beverly Hills, CA",
    val zipCode: String = "90210"
)
