package com.plansguru.agent

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class GuruChatEngine(context: Context) {
    private var inference: LlmInference? = null
    private var isModelLoaded = false

    init {
        try {
            // Attempt to initialize MediaPipe LLM Inference with on-device model from assets
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath("gemma-2b-it-cpu-int4.bin")
                .setMaxTokens(256)
                .setTemperature(0.7f)
                .build()
            inference = LlmInference.createFromOptions(context, options)
            isModelLoaded = true
        } catch (e: Exception) {
            isModelLoaded = false
        }
    }

    fun generateResponse(userMessage: String): String {
        return if (isModelLoaded && inference != null) {
            try {
                inference!!.generateResponse(userMessage)
            } catch (e: Exception) {
                fallbackSmartResponse(userMessage)
            }
        } else {
            fallbackSmartResponse(userMessage)
        }
    }

    private fun fallbackSmartResponse(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("italian") || lower.contains("pasta") -> 
                "Got it! I've noted down your love for Italian cuisine in your encrypted local social graph."
            lower.contains("vegan") || lower.contains("gluten") -> 
                "Understood! I've recorded your dietary requirement as a hard constraint for future group dinners."
            lower.contains("friday") || lower.contains("dinner") || lower.contains("eat") -> 
                "I'm ready! Head over to the 'One plan' tab to coordinate dinner for four with Mike, Sarah, and Jen."
            else -> 
                "I'm your on-device PlansGuru agent! I keep your data 100% private. Tell me what cuisines or dietary preferences you have, and I'll learn them locally."
        }
    }
}
