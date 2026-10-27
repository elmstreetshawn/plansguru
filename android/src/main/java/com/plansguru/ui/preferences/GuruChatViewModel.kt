package com.plansguru.ui.preferences

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.agent.GuruChatEngine
import com.plansguru.data.model.Assertion
import com.plansguru.data.model.Person
import com.plansguru.data.model.Relationship
import com.plansguru.data.repository.GraphRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean
)

class GuruChatViewModel(application: Application) : AndroidViewModel(application) {
    private val chatEngine = GuruChatEngine()
    private val repository = GraphRepository(application)

    private var pendingFriendName: String? = null
    private var pendingAssertionText: String? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("1", "Hi! I'm your on-device PlansGuru agent. Tell me what cuisines, dietary needs, travel distance, or loud crowd preferences you or your friends have, and I'll structure them into your encrypted social graph!", false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(UUID.randomUUID().toString(), text, true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            val lower = text.lowercase()

            // If we are waiting for confirmation/details to create a pending friend
            if (pendingFriendName != null) {
                if (lower.contains("yes") || lower.contains("sure") || lower.contains("create") || text.length > 2) {
                    val friendName = pendingFriendName!!
                    val fullName = if (text.lowercase().contains(friendName.lowercase())) text else "$friendName $text"

                    // Save person to graph
                    repository.savePerson(
                        Person(
                            localPersonId = friendName,
                            displayName = fullName,
                            publicIdentityKey = "pub_key_$friendName",
                            creationTimestamp = System.currentTimeMillis()
                        )
                    )
                    repository.saveRelationship(
                        Relationship(
                            ownerId = "local_user",
                            relatedPersonId = friendName,
                            relationshipType = "FRIEND",
                            trustState = "VERIFIED",
                            sharingPermissions = "ALL"
                        )
                    )

                    val confirmationMsg = ChatMessage(UUID.randomUUID().toString(), "Created profile for $fullName and added them to your social graph!", false)
                    _messages.value = _messages.value + confirmationMsg

                    // If there was a pending assertion, process it now via structured parser
                    pendingAssertionText?.let { pending ->
                        val (_, structuredAssertions) = chatEngine.generateStructuredResponse(pending)
                        for (sa in structuredAssertions) {
                            repository.saveAssertion(
                                Assertion(
                                    assertionId = UUID.randomUUID().toString(),
                                    observerId = "local_user",
                                    subjectId = friendName,
                                    predicate = sa.predicate,
                                    value = sa.value,
                                    source = "LLM_STRUCTURED_PARSER",
                                    confidence = sa.confidence,
                                    validityTimestampStart = System.currentTimeMillis(),
                                    validityTimestampEnd = null,
                                    visibilityPolicy = "PRIVATE"
                                )
                            )
                        }
                    }

                    pendingFriendName = null
                    pendingAssertionText = null
                    return@launch
                } else if (lower.contains("no") || lower.contains("cancel")) {
                    val cancelMsg = ChatMessage(UUID.randomUUID().toString(), "Okay, I won't add $pendingFriendName.", false)
                    _messages.value = _messages.value + cancelMsg
                    pendingFriendName = null
                    pendingAssertionText = null
                    return@launch
                }
            }

            // Dynamically extract friend name from message (e.g. "my friend Gina", "Gina doesn't like...")
            val mentionedName = extractMentionedFriend(text)

            if (mentionedName != null) {
                val existingPerson = repository.getPerson(mentionedName)
                if (existingPerson == null) {
                    // Ask user if they want to create this person! NEVER auto-add!
                    pendingFriendName = mentionedName
                    pendingAssertionText = text
                    val askMsg = ChatMessage(UUID.randomUUID().toString(), "I can't find $mentionedName in your social graph. Should I create them? What's their last name or details?", false)
                    _messages.value = _messages.value + askMsg
                    return@launch
                }
            }

            // Simulate neural network inference time (token generation latency)
            delay(1200)

            // Get LLM structured response and assertions
            val (replyText, structuredAssertions) = chatEngine.generateStructuredResponse(text)
            val botMsg = ChatMessage(UUID.randomUUID().toString(), replyText, false)
            _messages.value = _messages.value + botMsg

            val targetSubject = mentionedName ?: "local_user"
            for (sa in structuredAssertions) {
                repository.saveAssertion(
                    Assertion(
                        assertionId = UUID.randomUUID().toString(),
                        observerId = "local_user",
                        subjectId = if (mentionedName != null) targetSubject else sa.subject,
                        predicate = sa.predicate,
                        value = sa.value,
                        source = "LLM_STRUCTURED_PARSER",
                        confidence = sa.confidence,
                        validityTimestampStart = System.currentTimeMillis(),
                        validityTimestampEnd = null,
                        visibilityPolicy = "PRIVATE"
                    )
                )
            }
        }
    }

    private fun extractMentionedFriend(text: String): String? {
        val patterns = listOf(
            Regex("my friend ([A-Z][a-zA-Z]*)", RegexOption.IGNORE_CASE),
            Regex("friend ([A-Z][a-zA-Z]*)", RegexOption.IGNORE_CASE),
            Regex("for ([A-Z][a-zA-Z]*)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val name = match.groupValues[1]
                if (!listOf("my", "the", "a", "an", "our", "their").contains(name.lowercase())) {
                    return name.replaceFirstChar { it.uppercase() }
                }
            }
        }
        val knownNames = listOf("Sam", "Maya", "Jo", "Priya", "Lena", "Sarah", "Mike", "Jen", "Gina", "Alex", "David")
        for (name in knownNames) {
            if (text.contains(name, ignoreCase = true)) {
                return name
            }
        }
        return null
    }
}
