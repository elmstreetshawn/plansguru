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

const val SERVER_BASE_URL = "https://smart-lizards-nail.loca.lt"

class GuruChatEngine(private val context: Context) {
    private val repository = GraphRepository(context)
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    var isModelLoaded = false
        private set
    var initializationError: String? = null
        private set
        
    private val localUserId: String
        get() = context.getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE).getString("user_id", "local_user") ?: "local_user"

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
            
            Instructions:
            - Acknowledge recording these specific preferences warmly in 1-2 direct sentences.
            - Do NOT offer to search or find restaurants right now.
            - Do NOT write long lectures.
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

        val myName = repository.getPerson(localUserId)?.displayName?.takeIf { it.isNotBlank() && it != localUserId } ?: "You"

        val responseText = buildString {
            append(agentReply)
            if (assertions.isNotEmpty()) {
                append("\n\n📌 Mapped Social Graph Assertions:\n")
                append(assertions.joinToString("\n") {
                    val displaySubj = if (it.subject == localUserId) myName else it.subject
                    "• [$displaySubj] ${it.predicate.replace('_', ' ')}: ${it.value}"
                })
            }
        }

        return Pair(responseText, assertions)
    }

    private fun buildLocalGraphContext(): String {
        val persons = repository.getAllPersons()
        val sb = StringBuilder()

        val userAssertions = repository.getAssertionsForSubject(localUserId)
        if (userAssertions.isNotEmpty()) {
            sb.append("User ($localUserId) preferences:\n")
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

    private fun parseLlmSemanticOutput(userMessage: String, defaultSubject: String = localUserId): List<StructuredAssertion> {
        val results = mutableListOf<StructuredAssertion>()
        val globalSubject = extractSubjectFromText(userMessage)

        val clauses = userMessage.split(Regex("[,.!?;\\n]|\\b(but|however|except|and|or)\\b", RegexOption.IGNORE_CASE))

        val cuisineMap = mapOf(
            "italian" to "Italian", "mexican" to "Mexican", "japanese" to "Japanese",
            "sushi" to "Sushi", "ramen" to "Ramen", "thai" to "Thai", "tai" to "Thai",
            "chinese" to "Chinese", "indian" to "Indian", "american" to "American",
            "pizza" to "Pizza", "burger" to "Burger", "burgers" to "Burger",
            "bbq" to "BBQ", "barbecue" to "BBQ", "hotdog" to "Hotdogs",
            "hotdogs" to "Hotdogs", "hot dog" to "Hotdogs", "hot dogs" to "Hotdogs",
            "tex-mex" to "Tex-Mex", "seafood" to "Seafood", "steak" to "Steakhouse",
            "mediterranean" to "Mediterranean", "greek" to "Greek", "french" to "French",
            "cuban" to "Cuban", "vietnamese" to "Vietnamese", "pho" to "Vietnamese",
            "korean" to "Korean", "dim sum" to "Chinese", "taco" to "Mexican",
            "tacos" to "Mexican", "spicy" to "Spicy Cuisine"
        )

        val ingredientMap = mapOf(
            "american cheese" to "American Cheese", "cheese" to "Cheese",
            "broccoli" to "Broccoli", "cauliflower" to "Cauliflower",
            "olive oil" to "Olive Oil", "extra virgin olive oil" to "Olive Oil",
            "egg" to "Eggs", "eggs" to "Eggs", "chicken" to "Chicken",
            "salad" to "Salad & Greens", "salads" to "Salad & Greens",
            "potato" to "Potatoes", "potatoes" to "Potatoes",
            "mushroom" to "Mushrooms", "mushrooms" to "Mushrooms",
            "avocado" to "Avocado", "bacon" to "Bacon"
        )

        val beverageMap = mapOf(
            "protein shake" to "Protein Shakes", "protein shakes" to "Protein Shakes",
            "shake" to "Protein Shakes", "smoothie" to "Protein Shakes",
            "coffee" to "Coffee", "espresso" to "Espresso", "tea" to "Tea"
        )

        val dietaryMap = mapOf(
            "vegan" to "Vegan", "gluten" to "Gluten-Free", "vegetarian" to "Vegetarian",
            "dairy" to "Dairy-Free", "halal" to "Halal", "kosher" to "Kosher",
            "pescatarian" to "Pescatarian"
        )

        var currentSentimentIsNegative = false

        for (clause in clauses) {
            val clauseTrimmed = clause.trim()
            if (clauseTrimmed.isBlank()) continue
            val clauseLower = clauseTrimmed.lowercase()

            val clauseSubject = extractSubjectFromText(clauseTrimmed).ifEmpty { globalSubject }

            val hasNegativeIndicator = clauseLower.contains("don't") || clauseLower.contains("doesn't") ||
                    clauseLower.contains("dont") || clauseLower.contains("doesnt") ||
                    clauseLower.contains("dislike") || clauseLower.contains("hate") ||
                    clauseLower.contains("avoid") || clauseLower.contains("no ") ||
                    clauseLower.contains("not ") || clauseLower.contains("can't") ||
                    clauseLower.contains("cant")

            val hasPositiveIndicator = clauseLower.contains("like") || clauseLower.contains("love") ||
                    clauseLower.contains("prefer") || clauseLower.contains("enjoy") ||
                    clauseLower.contains("craving") || clauseLower.contains("want")

            if (hasNegativeIndicator) {
                currentSentimentIsNegative = true
            } else if (hasPositiveIndicator) {
                currentSentimentIsNegative = false
            }

            val isClauseNegative = currentSentimentIsNegative

            // 1. Cuisines
            for ((key, value) in cuisineMap) {
                if (key == "american" && (clauseLower.contains("american cheese") || clauseLower.contains("american slice"))) {
                    continue // Skip mapping American cuisine when the user specifically mentioned American Cheese
                }
                val keyRegex = Regex("\\b${key}s?\\b", RegexOption.IGNORE_CASE)
                if (keyRegex.containsMatchIn(clauseLower)) {
                    val predicate = if (isClauseNegative) "dislikes_cuisine" else "likes_cuisine"
                    val exists = results.any { it.subject == clauseSubject && it.predicate == predicate && it.value == value }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, predicate, value))
                    }
                }
            }

            // 2. Ingredients
            for ((key, value) in ingredientMap) {
                val keyRegex = Regex("\\b${key}s?\\b", RegexOption.IGNORE_CASE)
                if (keyRegex.containsMatchIn(clauseLower)) {
                    val predicate = if (isClauseNegative) "disliked_ingredient" else "favorite_ingredient"
                    val exists = results.any { it.subject == clauseSubject && it.predicate == predicate && it.value == value }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, predicate, value))
                    }
                }
            }

            // 3. Beverages
            for ((key, value) in beverageMap) {
                val keyRegex = Regex("\\b${key}s?\\b", RegexOption.IGNORE_CASE)
                if (keyRegex.containsMatchIn(clauseLower)) {
                    val predicate = if (isClauseNegative) "dislikes_beverage" else "beverage_preference"
                    val exists = results.any { it.subject == clauseSubject && it.predicate == predicate && it.value == value }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, predicate, value))
                    }
                }
            }

            // 4. Dietary Restrictions
            for ((key, value) in dietaryMap) {
                val keyRegex = Regex("\\b${key}\\b", RegexOption.IGNORE_CASE)
                if (keyRegex.containsMatchIn(clauseLower)) {
                    val predicate = if (isClauseNegative) "dislikes_dietary" else "dietary_restriction"
                    val exists = results.any { it.subject == clauseSubject && it.predicate == predicate && it.value == value }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, predicate, value))
                    }
                }
            }

            // Special Nut & Peanut Discrimination (STRICT WORD BOUNDARIES)
            val mentionsNut = Regex("\\b(nuts?|peanuts?)\\b", RegexOption.IGNORE_CASE).containsMatchIn(clauseLower)
            val explicitAllergyTerms = clauseLower.contains("allerg") || clauseLower.contains("nut-free") ||
                    clauseLower.contains("peanut-free") || clauseLower.contains("can't eat nut") ||
                    clauseLower.contains("cannot eat nut") || clauseLower.contains("no nuts") ||
                    clauseLower.contains("avoid nuts")

            if (mentionsNut) {
                if (explicitAllergyTerms) {
                    val nutVal = if (clauseLower.contains("peanut")) "Peanut Allergy" else "Nut Allergy"
                    results.removeAll { it.subject == clauseSubject && (it.value == "Nuts" || it.value == "Peanuts") }
                    val exists = results.any { it.subject == clauseSubject && it.predicate == "dietary_restriction" && it.value == nutVal }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, "dietary_restriction", nutVal))
                    }
                } else {
                    val nutVal = if (clauseLower.contains("peanut")) "Peanuts" else "Nuts"
                    val exists = results.any { it.subject == clauseSubject && it.value == nutVal }
                    if (!exists) {
                        results.add(StructuredAssertion(clauseSubject, "likes_cuisine", nutVal))
                    }
                }
            }

            // 3A. Zip Code Parsing (Exact 5-Digit Match Only)
            val zipMatch = Regex("\\b\\d{5}\\b").find(clauseTrimmed)?.value
            if (zipMatch != null) {
                val exists = results.any { it.subject == clauseSubject && it.predicate == "locationZip" }
                if (!exists) {
                    results.add(StructuredAssertion(clauseSubject, "locationZip", zipMatch))
                }
            }

            // 3B. Travel Time & Drive Distance Parsing (STRICT WORD BOUNDARIES)
            val mentionsTravelKeyword = clauseLower.contains("minute") || clauseLower.contains("min") ||
                    clauseLower.contains("hour") || clauseLower.contains("hr") ||
                    clauseLower.contains("mile") || clauseLower.contains("drive") ||
                    clauseLower.contains("commute") || clauseLower.contains("travel")

            if (mentionsTravelKeyword) {
                val clauseWithoutZip = clauseTrimmed.replace(Regex("\\b\\d{5}\\b"), "")
                val numbers = Regex("\\b\\d{1,3}\\b").findAll(clauseWithoutZip).mapNotNull { it.value.toIntOrNull() }.toList()
                val num = numbers.firstOrNull()

                if (num != null && num in 1..180) {
                    val hasMinutesKeyword = Regex("\\b(minutes?|mins?)\\b", RegexOption.IGNORE_CASE).containsMatchIn(clauseLower)
                    val hasMilesKeyword = Regex("\\b(miles?|mi)\\b", RegexOption.IGNORE_CASE).containsMatchIn(clauseLower)
                    val hasHoursKeyword = Regex("\\b(hours?|hrs?)\\b", RegexOption.IGNORE_CASE).containsMatchIn(clauseLower)

                    if (hasMilesKeyword) {
                        val exists = results.any { it.subject == clauseSubject && it.predicate == "max_distance_miles" }
                        if (!exists) {
                            results.add(StructuredAssertion(clauseSubject, "max_distance_miles", "$num miles"))
                        }
                    } else if (hasMinutesKeyword || hasHoursKeyword || clauseLower.contains("drive") || clauseLower.contains("travel")) {
                        val normVal = if (hasHoursKeyword) "${num * 60} minutes" else "$num minutes"
                        val exists = results.any { it.subject == clauseSubject && it.predicate == "max_travel_time" }
                        if (!exists) {
                            results.add(StructuredAssertion(clauseSubject, "max_travel_time", normVal))
                        }
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

        val mentionsFriendThirdPerson = lowerMessage.contains("my friend ") || lowerMessage.contains("my buddy ") ||
                lowerMessage.contains("my pal ") || lowerMessage.contains("my roommate ") ||
                lowerMessage.contains("my coworker ") || lowerMessage.contains("for ")

        if (!mentionsFriendThirdPerson) {
            val isFirstPerson = lowerMessage.startsWith("i ") || lowerMessage.startsWith("i'm ") ||
                    lowerMessage.startsWith("im ") || lowerMessage.startsWith("my ") ||
                    lowerMessage.startsWith("we ") || lowerMessage.startsWith("our ") ||
                    lowerMessage.contains("i like") || lowerMessage.contains("i love") ||
                    lowerMessage.contains("i hate") || lowerMessage.contains("i want") ||
                    lowerMessage.contains("i prefer") || lowerMessage.contains("my zip")

            if (isFirstPerson) {
                return localUserId
            }
        }

        val nonPersonWords = setOf(
            "i", "me", "my", "mine", "we", "our", "us", "you", "your", "yours", "he", "him", "his",
            "she", "her", "hers", "they", "them", "their", "this", "that", "these", "those",
            "hello", "hi", "hey", "plansguru", "guru", "thanks", "thank", "ok", "okay",
            "friend", "buddy", "pal", "roommate", "coworker", "guy", "girl", "person",
            "taco", "tacos", "pizza", "pizzas", "burger", "burgers", "sushi", "ramen",
            "italian", "mexican", "japanese", "thai", "chinese", "indian", "american", "french",
            "cuban", "vietnamese", "korean", "pho", "bbq", "barbecue", "steak", "steakhouse",
            "pasta", "food", "dinner", "lunch", "breakfast", "brunch", "snack", "meal",
            "restaurant", "restaurants", "place", "places", "zip", "code", "mile", "miles",
            "minute", "minutes", "min", "mins", "hour", "hours", "drive", "travel", "distance",
            "quiet", "loud", "crowd", "crowds", "vibe", "spicy", "vegan", "vegetarian", "gluten",
            "dairy", "halal", "kosher", "peanut", "nut", "allergy", "allergies", "pescatarian",
            "potato", "potatoes"
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

        return localUserId
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
