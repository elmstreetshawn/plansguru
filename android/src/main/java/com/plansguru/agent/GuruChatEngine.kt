package com.plansguru.agent

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.plansguru.data.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class StructuredAssertion(
    val subject: String,
    val predicate: String,
    val value: String,
    val confidence: Float = 1.0f
)

data class FaqItem(
    val keywords: List<String>,
    val answer: String
)

const val SERVER_BASE_URL = "http://192.168.2.37:8000"

class GuruChatEngine(private val context: Context) {
    private val repository = GraphRepository(context)
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    var isModelLoaded = false
        private set
    var initializationError: String? = null
        private set

    private val faqKnowledgeBase = listOf(
        FaqItem(
            listOf("tag", "group", "squad", "change tag", "edit group", "manage group"),
            "💡 To edit your groups and tags, head over to the **Preferences Hub** tab, select **Squads & Groups**, and tap on any group card to edit its title, add members, or remove members."
        ),
        FaqItem(
            listOf("add a friend", "add friend", "new friend", "people", "contact"),
            "💡 To add a friend, go to the **People** tab and tap **'Add a friend'**. You can add them manually, load demo contacts for testing, or import from phone contacts!"
        ),
        FaqItem(
            listOf("add a place", "add place", "restaurant", "new restaurant", "places", "food spot"),
            "💡 To add a custom restaurant or place, go to the **Places** tab and tap the floating **`+`** button in the bottom right corner."
        ),
        FaqItem(
            listOf("data", "privacy", "secure", "encrypted", "cloud", "store", "where is my data"),
            "🔒 Your data is 100% private and stored locally on your device in a secure SQLite database secured by Android Keystore. Zero cloud storage!"
        )
    )

    init {
        // Non-blocking quick check
    }

    suspend fun initializeModel(statusCallback: (String) -> Unit = { _ -> }): Boolean = withContext(Dispatchers.IO) {
        if (isModelLoaded && engine != null) return@withContext true

        try {
            statusCallback("Checking local model file...")
            val modelFileName = "gemma3-1b-it-int4.litertlm"
            val externalDir = context.getExternalFilesDir(null)
            val externalFile = if (externalDir != null) File(externalDir, modelFileName) else null

            val modelFile = if (externalFile != null && externalFile.exists() && externalFile.length() > 500L * 1024L * 1024L) {
                Log.d("GuruChatEngine", "Using external files model: ${externalFile.absolutePath}")
                externalFile
            } else {
                File(context.filesDir, modelFileName)
            }

            val isValidTaskFile = modelFile.exists() && modelFile.length() > 500L * 1024L * 1024L

            if (!isValidTaskFile) {
                if (modelFile.exists()) {
                    modelFile.delete()
                }

                val assetOpened = try {
                    context.assets.open(modelFileName).use { input ->
                        modelFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } catch (_: Exception) {
                    false
                }

                if (!assetOpened && (!modelFile.exists() || modelFile.length() < 500L * 1024L * 1024L)) {
                    statusCallback("Connecting to laptop ($SERVER_BASE_URL)...")
                    Log.d("GuruChatEngine", "Downloading Gemma model from laptop ($SERVER_BASE_URL/api/model/gemma)...")

                    val client = OkHttpClient.Builder()
                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(1800, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(1800, java.util.concurrent.TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .build()

                    var success = false
                    var attempts = 0
                    var lastException: Throwable? = null

                    while (!success && attempts < 3) {
                        attempts++
                        try {
                            if (modelFile.exists()) modelFile.delete()
                            val request = Request.Builder()
                                .url("$SERVER_BASE_URL/api/model/gemma")
                                .build()

                            client.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) {
                                    val errorBody = response.body?.string() ?: "Unknown error"
                                    throw java.io.IOException("HTTP error ${response.code}: $errorBody")
                                }
                                val body = response.body ?: throw java.io.IOException("Empty response body")
                                val contentLength = body.contentLength()

                                body.byteStream().use { input ->
                                    modelFile.outputStream().use { output ->
                                        val buffer = ByteArray(65536)
                                        var bytesCopied = 0L
                                        var bytes: Int
                                        while (input.read(buffer).also { bytes = it } >= 0) {
                                            output.write(buffer, 0, bytes)
                                            bytesCopied += bytes
                                            if (contentLength > 0) {
                                                val percent = (bytesCopied * 100 / contentLength).toInt()
                                                val mbCopied = bytesCopied / (1024 * 1024)
                                                val mbTotal = contentLength / (1024 * 1024)
                                                statusCallback("Downloading Gemma model (Attempt $attempts/3):\n$mbCopied MB / $mbTotal MB ($percent%)")
                                            } else {
                                                val mbCopied = bytesCopied / (1024 * 1024)
                                                statusCallback("Downloading Gemma model (Attempt $attempts/3):\n$mbCopied MB downloaded...")
                                            }
                                        }
                                    }
                                }
                            }
                            success = true
                        } catch (e: Throwable) {
                            lastException = e
                            Log.w("GuruChatEngine", "Download attempt $attempts failed: ${e.message}. Retrying...", e)
                            statusCallback("⚠️ Connection dropped. Retrying (Attempt ${attempts + 1}/3)...")
                            kotlinx.coroutines.delay(3000L * attempts)
                        }
                    }

                    if (!success) {
                        throw lastException ?: java.io.IOException("Failed to download model after 3 attempts.")
                    }

                    statusCallback("Download complete! Initializing LiteRT-LM engine...")
                    Log.d("GuruChatEngine", "Successfully downloaded Gemma model to ${modelFile.absolutePath}")
                }
            }

            if (modelFile.exists() && modelFile.length() > 500L * 1024L * 1024L) {
                statusCallback("Initializing LiteRT-LM Gemma Engine...")
                val engineConfig = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU()
                )
                val newEngine = Engine(engineConfig)
                newEngine.initialize()
                conversation = newEngine.createConversation()
                engine = newEngine
                isModelLoaded = true
                initializationError = null
                statusCallback("✨ LiteRT-LM Gemma AI engine is ready!")
                Log.d("GuruChatEngine", "LiteRT-LM Engine successfully initialized!")
                true
            } else {
                throw java.io.IOException("Model file is missing or invalid.")
            }
        } catch (e: Throwable) {
            isModelLoaded = false
            engine = null
            conversation = null
            initializationError = e.message ?: e.toString()
            statusCallback("❌ LiteRT-LM initialization failed: ${e.message}")
            Log.e("GuruChatEngine", "Failed to initialize LiteRT-LM Engine", e)
            false
        }
    }

    suspend fun generateStructuredResponse(userMessage: String): Pair<String, List<StructuredAssertion>> {
        val lower = userMessage.lowercase().trim()

        // 1. Check FAQ queries first
        val matchingFaq = faqKnowledgeBase.find { faq ->
            faq.keywords.any { keyword -> lower.contains(keyword) }
        }
        if (matchingFaq != null) {
            return Pair(matchingFaq.answer, emptyList())
        }

        // 2. Run real Gemma LLM Inference on-device via LiteRT-LM with Local RAG Memory Context!
        val assertions = parseLlmSemanticOutput(userMessage)
        val graphContext = buildLocalGraphContext()

        val prompt = if (assertions.isNotEmpty()) {
            """
            You are PlansGuru, a warm, friendly, and intelligent on-device AI dinner agent.
            User logged: "$userMessage"
            
            Instruction: Acknowledge logging this preference warmly in 1-2 friendly, natural sentences. Do not write a long lecture.
            """.trimIndent()
        } else {
            """
            You are PlansGuru, a warm, friendly, and intelligent on-device AI dinner agent.
            
            Known Social Graph Context:
            $graphContext
            
            User message: "$userMessage"
            
            Instructions:
            - Answer the user warmly and conversationally in 1-2 helpful sentences.
            - Use the Known Social Graph Context above to answer questions from memory or suggest restaurant ideas.
            """.trimIndent()
        }

        val llmOutput = if (isModelLoaded && conversation != null) {
            try {
                withContext(Dispatchers.IO) {
                    val resultMsg = conversation!!.sendMessage(prompt)
                    resultMsg.toString()
                }
            } catch (e: Throwable) {
                Log.e("GuruChatEngine", "LiteRT-LM inference execution error", e)
                null
            }
        } else {
            null
        }

        val agentReply = llmOutput ?: buildString {
            if (assertions.isNotEmpty()) {
                append("On-Device Agent: Got it! I extracted ")
                append(assertions.joinToString(", ") { "${it.subject}'s ${it.predicate.replace('_', ' ')} (${it.value})" })
                append(" and updated your local social graph.")
            } else {
                append("On-Device Agent: Recorded your preferences locally into your secure social graph.")
            }
        }

        val responseText = buildString {
            append(agentReply)
            if (assertions.isNotEmpty()) {
                append("\n\n📌 Mapped Social Graph Assertions:\n")
                append(assertions.joinToString("\n") { "• [${it.subject}] ${it.predicate.replace('_', ' ')}: ${it.value}" })
            }
        }

        return Pair(responseText, assertions)
    }

    private fun buildLocalGraphContext(): String {
        val persons = repository.getAllPersons()
        val sb = StringBuilder()

        val userAssertions = repository.getAssertionsForSubject("local_user")
        if (userAssertions.isNotEmpty()) {
            sb.append("User (local_user) preferences:\n")
            userAssertions.forEach { sb.append("  - ${it.predicate.replace('_', ' ')}: ${it.value}\n") }
        }

        for (person in persons) {
            val pAssertions = repository.getAssertionsForSubject(person.localPersonId)
            if (pAssertions.isNotEmpty()) {
                sb.append("${person.displayName}'s preferences:\n")
                pAssertions.forEach { sb.append("  - ${it.predicate.replace('_', ' ')}: ${it.value}\n") }
            }
        }

        return if (sb.isEmpty()) "No preferences logged in social graph yet." else sb.toString().trim()
    }

    private fun parseLlmSemanticOutput(userMessage: String, defaultSubject: String = "local_user"): List<StructuredAssertion> {
        val lower = userMessage.lowercase()
        val isNegative = lower.contains("don't") || lower.contains("doesn't") || lower.contains("dont") || lower.contains("doesnt") || lower.contains("dislike") || lower.contains("hate") || lower.contains("no ")

        val subject = extractSubjectFromText(userMessage)
        val results = mutableListOf<StructuredAssertion>()

        // 1. Cuisines
        val cuisines = listOf("italian", "mexican", "japanese", "thai", "chinese", "indian", "american", "pizza", "burger", "sushi", "tai")
        for (cuisine in cuisines) {
            if (lower.contains(cuisine)) {
                val cleanCuisine = if (cuisine == "tai") "Thai" else cuisine.replaceFirstChar { it.uppercase() }
                val predicate = if (isNegative) "dislikes_cuisine" else "likes_cuisine"
                results.add(StructuredAssertion(subject, predicate, cleanCuisine))
            }
        }

        // 2. Dietary Restrictions
        val dietary = mapOf("vegan" to "Vegan", "gluten" to "Gluten-Free", "vegetarian" to "Vegetarian", "dairy" to "Dairy-Free", "halal" to "Halal", "kosher" to "Kosher")
        for ((key, value) in dietary) {
            if (lower.contains(key)) {
                val predicate = if (isNegative) "dislikes_dietary" else "dietary_restriction"
                results.add(StructuredAssertion(subject, predicate, value))
            }
        }

        // 3. Structured Distance / Travel Time / Zip Code Parsing
        if (lower.contains("distance") || lower.contains("mile") || lower.contains("travel") || lower.contains("far") || lower.contains("close") || lower.contains("travelled") || lower.contains("drive") || lower.contains("minute") || lower.contains("hour") || lower.contains("min") || lower.contains("hr") || lower.contains("commute") || lower.contains("time") || lower.contains("zip")) {
            val numberRegex = Regex("\\d+")
            val numbers = numberRegex.findAll(lower).map { it.value.toInt() }.toList()
            val num = numbers.firstOrNull()

            val isMinutes = lower.contains("minute") || lower.contains("min")
            val isHours = lower.contains("hour") || lower.contains("hr")
            val isMiles = lower.contains("mile") || lower.contains("mi")

            // Extract 5-digit zip code if present
            val zipRegex = Regex("\\b\\d{5}\\b")
            val zipMatch = zipRegex.find(userMessage)
            val zipCode = zipMatch?.value

            val normalizedValue = buildString {
                if (num != null) {
                    when {
                        isMinutes -> append("$num minutes")
                        isHours -> append("$num hours (${num * 60} minutes)")
                        isMiles -> append("$num miles")
                        else -> append("$num units")
                    }
                } else {
                    append(userMessage)
                }
                if (zipCode != null) {
                    append(" from zip $zipCode")
                }
            }

            val predicate = if (isMinutes || isHours) "max_travel_time" else "max_distance_miles"
            results.add(StructuredAssertion(subject, predicate, normalizedValue))
        }

        // 4. Noise / Crowds / Vibe
        if (lower.contains("loud") || lower.contains("crowd") || lower.contains("quiet") || lower.contains("noise") || lower.contains("vibe") || lower.contains("busy")) {
            val valText = if (isNegative && (lower.contains("loud") || lower.contains("crowd"))) "Quiet / No Crowds" else userMessage
            results.add(StructuredAssertion(subject, "noise_preference", valText))
        }

        return results
    }

    private fun extractSubjectFromText(userMessage: String): String {
        val words = userMessage.split(" ", ",", ".")
        for (i in words.indices) {
            val cleanWord = words[i].replace(Regex("[^a-zA-Z]"), "")
            if (cleanWord.length > 1 && cleanWord[0].isUpperCase()) {
                val lower = cleanWord.lowercase()
                val stopWords = listOf("i", "my", "we", "our", "the", "a", "an", "this", "that", "hello", "hi", "hey", "plansguru", "italian", "mexican", "japanese", "thai", "chinese", "indian", "american", "pizza", "burger", "sushi", "vegan", "gluten", "vegetarian")
                if (!stopWords.contains(lower)) {
                    return cleanWord.replaceFirstChar { it.uppercase() }
                }
            }
        }
        return "local_user"
    }

    suspend fun generateResponse(userMessage: String): String {
        return generateStructuredResponse(userMessage).first
    }

    companion object {
        fun isModelReady(context: Context): Boolean {
            val filenames = listOf("gemma3-1b-it-int4.litertlm", "gemma-4-E2B-it-web.task")
            val extDir = context.getExternalFilesDir(null)
            for (fn in filenames) {
                val extFile = if (extDir != null) File(extDir, fn) else null
                if (extFile != null && extFile.exists() && extFile.length() > 500L * 1024L * 1024L) return true
                val localFile = File(context.filesDir, fn)
                if (localFile.exists() && localFile.length() > 500L * 1024L * 1024L) return true
            }
            return false
        }
    }
}
