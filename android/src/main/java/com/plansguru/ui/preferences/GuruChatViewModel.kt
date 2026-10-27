package com.plansguru.ui.preferences

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.agent.GuruChatEngine
import com.plansguru.data.model.Assertion
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

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("1", "Hi! I'm your on-device PlansGuru agent. Tell me what cuisines, dietary needs, or vibe you like for dinners, and I'll keep them encrypted on your device!", false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(UUID.randomUUID().toString(), text, true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            // Get on-device LLM response
            val replyText = chatEngine.generateResponse(text)
            val botMsg = ChatMessage(UUID.randomUUID().toString(), replyText, false)
            _messages.value = _messages.value + botMsg

            // Automatically extract assertion if user mentioned Italian or Vegan/Gluten-free
            val lower = text.lowercase()
            if (lower.contains("italian")) {
                repository.saveAssertion(
                    Assertion(UUID.randomUUID().toString(), "local_user", "local_user", "likes_cuisine", "Italian", "CHAT_INFERENCE", 1.0f, System.currentTimeMillis(), null, "PRIVATE")
                )
            }
            if (lower.contains("vegan")) {
                repository.saveAssertion(
                    Assertion(UUID.randomUUID().toString(), "local_user", "local_user", "dietary_restriction", "Vegan", "CHAT_INFERENCE", 1.0f, System.currentTimeMillis(), null, "PRIVATE")
                )
            }
            if (lower.contains("gluten")) {
                repository.saveAssertion(
                    Assertion(UUID.randomUUID().toString(), "local_user", "local_user", "dietary_restriction", "Gluten-Free", "CHAT_INFERENCE", 1.0f, System.currentTimeMillis(), null, "PRIVATE")
                )
            }
        }
    }
}
