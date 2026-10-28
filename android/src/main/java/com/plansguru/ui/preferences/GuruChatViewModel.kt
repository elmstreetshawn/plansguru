package com.plansguru.ui.preferences

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.agent.GuruChatEngine
import com.plansguru.data.model.Assertion
import com.plansguru.data.model.Person
import com.plansguru.data.model.Relationship
import com.plansguru.data.repository.GraphRepository
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
    private val chatEngine = GuruChatEngine(application)
    private val repository = GraphRepository(application)

    private var pendingFriendName: String? = null
    private var pendingAssertionText: String? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("1", "Hi! I'm your on-device PlansGuru agent. Tell me what cuisines, dietary needs, travel distance, or loud crowd preferences you or your friends have, and I'll structure them into your encrypted social graph!", false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            if (!chatEngine.isModelLoaded) {
                val statusMsgId = UUID.randomUUID().toString()
                val statusMsg = ChatMessage(statusMsgId, "⚙️ Checking on-device Gemma AI engine...", false)
                _messages.value = _messages.value + statusMsg

                val success = chatEngine.initializeModel { statusText ->
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == statusMsgId) msg.copy(text = statusText) else msg
                    }
                }

                if (success) {
                    _messages.value = _messages.value.filterNot { it.id == statusMsgId }
                }
            }
        }
    }

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

            // Get LLM structured response and assertions
            val (replyText, structuredAssertions) = chatEngine.generateStructuredResponse(text)

            // Check if assertions reference a subject other than local_user that isn't in graph yet
            val newFriendSubject = structuredAssertions.map { it.subject }.firstOrNull { it != "local_user" && repository.getPerson(it) == null }

            if (newFriendSubject != null) {
                pendingFriendName = newFriendSubject
                pendingAssertionText = text
                val askMsg = ChatMessage(UUID.randomUUID().toString(), "I noted preferences for $newFriendSubject, but I can't find them in your social graph. Should I create a profile for $newFriendSubject?", false)
                _messages.value = _messages.value + askMsg
                return@launch
            }

            val botMsg = ChatMessage(UUID.randomUUID().toString(), replyText, false)
            _messages.value = _messages.value + botMsg

            for (sa in structuredAssertions) {
                if (sa.predicate == "locationZip") {
                    val currentMe = repository.getPerson("local_user") ?: Person(
                        localPersonId = "local_user",
                        displayName = "Me",
                        publicIdentityKey = "pub_key_local_user",
                        creationTimestamp = System.currentTimeMillis(),
                        locationZip = sa.value
                    )
                    repository.savePerson(currentMe.copy(locationZip = sa.value))
                }

                repository.saveAssertion(
                    Assertion(
                        assertionId = UUID.randomUUID().toString(),
                        observerId = "local_user",
                        subjectId = sa.subject,
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
}
