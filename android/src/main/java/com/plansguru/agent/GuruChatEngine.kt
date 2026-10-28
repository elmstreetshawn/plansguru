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

const val SERVER_BASE_URL = "https://few-jars-wear.loca.lt"

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
                    val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
                    val serverUrl = prefs.getString("server_url", SERVER_BASE_URL)?.trimEnd('/') ?: SERVER_BASE_URL

                    statusCallback("Connecting to server ($serverUrl)...")
                    Log.d("GuruChatEngine", "Downloading Gemma model from ($serverUrl/api/model/gemma)...")

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
                                .url("$serverUrl/api/model/gemma")
                                .addHeader("Bypass-Tunnel-Remainder", "true")
                                .addHeader("ngrok-skip-browser-warning", "true")
                                .addHeader("User-Agent", "PlansGuruApp/1.0")
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
        val results = mutableListOf<StructuredAssertion>()
        val globalSubject = extractSubjectFromText(userMessage)

        val clauses = userMessage.split(Regex("[.!?;\\n]|\\b(but|however|except)\\b", RegexOption.IGNORE_CASE))

        val cuisineMap = mapOf(
            "italian" to "Italian", "mexican" to "Mexican", "japanese" to "Japanese",
            "sushi" to "Sushi", "ramen" to "Ramen", "thai" to "Thai", "tai" to "Thai",
            "chinese" to "Chinese", "indian" to "Indian", "american" to "American",
            "pizza" to "Pizza", "burger" to "Burger", "bbq" to "BBQ", "barbecue" to "BBQ",
            "tex-mex" to "Tex-Mex", "seafood" to "Seafood", "steak" to "Steakhouse",
            "mediterranean" to "Mediterranean", "greek" to "Greek", "french" to "French",
            "cuban" to "Cuban", "vietnamese" to "Vietnamese", "pho" to "Vietnamese",
            "korean" to "Korean", "dim sum" to "Chinese", "taco" to "Mexican", "spicy" to "Spicy"
        )

        val dietaryMap = mapOf(
            "vegan" to "Vegan", "gluten" to "Gluten-Free", "vegetarian" to "Vegetarian",
            "dairy" to "Dairy-Free", "halal" to "Halal", "kosher" to "Kosher",
            "peanut" to "Peanut Allergy", "nut" to "Nut Allergy", "allergy" to "Allergies",
            "pescatarian" to "Pescatarian"
        )

        for (clause in clauses) {
            val clauseTrimmed = clause.trim()
            if (clauseTrimmed.isBlank()) continue
            val clauseLower = clauseTrimmed.lowercase()

            val clauseSubject = extractSubjectFromText(clauseTrimmed).ifEmpty { globalSubject }

            val isClauseNegative = clauseLower.contains("don't") || clauseLower.contains("doesn't") ||
                    clauseLower.contains("dont") || clauseLower.contains("doesnt") ||
                    clauseLower.contains("dislike") || clauseLower.contains("hate") ||
                    clauseLower.contains("avoid") || clauseLower.contains("no ") ||
                    clauseLower.contains("not ") || clauseLower.contains("can't") ||
                    clauseLower.contains("cant")

            // 1. Cuisines
            for ((key, value) in cuisineMap) {
                if (clauseLower.contains(key)) {
                    val predicate = if (isClauseNegative) "dislikes_cuisine" else "likes_cuisine"
                    val exists = results.any { it.subject == clauseSubject && it.predicate == predicate && it.value == value }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, predicate, value))
                    }
                }
            }

            // 2. Dietary Restrictions
            for ((key, value) in dietaryMap) {
                if (clauseLower.contains(key)) {
                    val predicate = if (isClauseNegative) "dislikes_dietary" else "dietary_restriction"
                    val exists = results.any { it.subject == clauseSubject && it.predicate == predicate && it.value == value }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, predicate, value))
                    }
                }
            }

            // 3. Travel Time & Distance & Zip Code
            if (clauseLower.contains("distance") || clauseLower.contains("mile") || clauseLower.contains("travel") ||
                clauseLower.contains("far") || clauseLower.contains("close") || clauseLower.contains("drive") ||
                clauseLower.contains("minute") || clauseLower.contains("hour") || clauseLower.contains("min") ||
                clauseLower.contains("hr") || clauseLower.contains("commute") || clauseLower.contains("zip")) {

                val numbers = Regex("\\d+").findAll(clauseLower).mapNotNull { it.value.toIntOrNull() }.toList()
                val num = numbers.firstOrNull()

                val zipMatch = Regex("\\b\\d{5}\\b").find(clauseTrimmed)?.value

                if (zipMatch != null) {
                    val exists = results.any { it.subject == clauseSubject && it.predicate == "locationZip" }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, "locationZip", zipMatch))
                    }
                }

                if (num != null) {
                    val isMinutes = clauseLower.contains("minute") || clauseLower.contains("min") || clauseLower.contains("drive")
                    val isHours = clauseLower.contains("hour") || clauseLower.contains("hr")
                    val isMiles = clauseLower.contains("mile") || clauseLower.contains("mi")

                    val normVal = when {
                        isMinutes -> "$num minutes"
                        isHours -> "${num * 60} minutes"
                        isMiles -> "$num miles"
                        else -> "$num minutes"
                    }

                    val predicate = if (isMiles) "max_distance_miles" else "max_travel_time"
                    val exists = results.any { it.subject == clauseSubject && it.predicate == predicate }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, predicate, normVal))
                    }
                }
            }

            // 4. Noise & Crowd Preference
            if (clauseLower.contains("loud") || clauseLower.contains("crowd") || clauseLower.contains("quiet") ||
                clauseLower.contains("noise") || clauseLower.contains("vibe") || clauseLower.contains("busy") ||
                clauseLower.contains("peaceful")) {

                val normVal = when {
                    clauseLower.contains("quiet") || clauseLower.contains("peaceful") -> "Quiet / Low Crowds"
                    clauseLower.contains("loud") || clauseLower.contains("busy") || clauseLower.contains("crowd") -> {
                        if (isClauseNegative) "Quiet / Low Crowds" else "Vibrant / Moderate"
                    }
                    else -> "Moderate"
                }

                val exists = results.any { it.subject == clauseSubject && it.predicate == "noise_preference" }
                if (!exists) {
                    results.add(StructuredAssertion(clauseSubject, predicate = "noise_preference", value = normVal))
                }
            }
        }

        return results
    }

    private fun extractSubjectFromText(userMessage: String): String {
        val lowerMessage = userMessage.lowercase().trim()

        val isFirstPerson = lowerMessage.startsWith("i ") || lowerMessage.startsWith("i'm ") ||
                lowerMessage.startsWith("im ") || lowerMessage.startsWith("my ") ||
                lowerMessage.startsWith("we ") || lowerMessage.startsWith("our ") ||
                lowerMessage.contains("i like") || lowerMessage.contains("i love") ||
                lowerMessage.contains("i hate") || lowerMessage.contains("i want") ||
                lowerMessage.contains("i prefer") || lowerMessage.contains("my zip")

        if (isFirstPerson) {
            return "local_user"
        }

        val nonPersonWords = setOf(
            "i", "me", "my", "mine", "we", "our", "us", "you", "your", "yours", "he", "him", "his",
            "she", "her", "hers", "they", "them", "their", "this", "that", "these", "those",
            "hello", "hi", "hey", "plansguru", "guru", "thanks", "thank", "ok", "okay",
            "taco", "tacos", "pizza", "pizzas", "burger", "burgers", "sushi", "ramen",
            "italian", "mexican", "japanese", "thai", "chinese", "indian", "american", "french",
            "cuban", "vietnamese", "korean", "pho", "bbq", "barbecue", "steak", "steakhouse",
            "pasta", "food", "dinner", "lunch", "breakfast", "brunch", "snack", "meal",
            "restaurant", "restaurants", "place", "places", "zip", "code", "mile", "miles",
            "minute", "minutes", "min", "mins", "hour", "hours", "drive", "travel", "distance",
            "quiet", "loud", "crowd", "crowds", "vibe", "spicy", "vegan", "vegetarian", "gluten",
            "dairy", "halal", "kosher", "peanut", "nut", "allergy", "allergies", "pescatarian"
        )

        val words = userMessage.split(" ", ",", ".", "!", "?", "\n")
        for (word in words) {
            val cleanWord = word.replace(Regex("[^a-zA-Z]"), "")
            if (cleanWord.length > 1 && cleanWord[0].isUpperCase()) {
                val lower = cleanWord.lowercase()
                if (!nonPersonWords.contains(lower) && !FoodKnowledgeGraph.isFoodOrGeneralTerm(cleanWord)) {
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
