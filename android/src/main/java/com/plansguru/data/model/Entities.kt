package com.plansguru.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Person(
    val localPersonId: String,
    val displayName: String,
    val publicIdentityKey: String,
    val creationTimestamp: Long,
    val locationZip: String = "90210"
)

@Serializable
data class Relationship(
    val ownerId: String,
    val relatedPersonId: String,
    val relationshipType: String, // e.g. "FRIEND", "FAMILY"
    val trustState: String, // e.g. "VERIFIED", "PENDING"
    val sharingPermissions: String // JSON or flags
)

@Serializable
data class Assertion(
    val assertionId: String,
    val observerId: String,
    val subjectId: String,
    val predicate: String, // e.g. "likes_cuisine", "dietary_restriction"
    val value: String, // e.g. "Italian", "Gluten-Free"
    val source: String, // "SELF" or "OBSERVATION"
    val confidence: Float,
    val validityTimestampStart: Long,
    val validityTimestampEnd: Long?,
    val visibilityPolicy: String // "PRIVATE", "AUTHORIZED_PEERS"
)

@Serializable
data class GraphEvent(
    val eventId: String,
    val authorId: String,
    val subjectId: String,
    val eventType: String,
    val payload: String,
    val version: Int,
    val timestamp: Long,
    val digitalSignature: String,
    val synchronizationStatus: String // "SYNCED", "PENDING"
)

@Serializable
data class PlanningSession(
    val sessionId: String,
    val participants: List<String>,
    val requestedActivity: String,
    val requestedTime: String,
    val status: String // "INIT", "NEGOTIATING", "CONSENSUS", "COMPLETED"
)

@Serializable
data class AgentProposal(
    val sessionId: String,
    val candidateId: String,
    val compatibilityScore: Float,
    val hardConstraintResult: Boolean,
    val revisionNumber: Int
)
