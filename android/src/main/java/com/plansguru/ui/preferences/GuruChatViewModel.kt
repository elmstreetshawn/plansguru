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

    private var wizardStep = 0
    private val localUserId: String
        get() = getApplication<Application>().getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE).getString("user_id", "localUserId") ?: "localUserId"
    private var wizardSubject = localUserId

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("1", "Hi! I'm your on-device PlansGuru agent. Tap '🧙‍♂️ Guided Setup' to set up your profile step-by-step, or tell me your food tastes below!", false)
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

    fun startGuidedProfileWizard(subject: String? = null) {
        wizardSubject = subject ?: localUserId
        wizardStep = 1
        val subjName = if (wizardSubject == localUserId) "your" else "${wizardSubject}'s"
        val firstPrompt = "🧙‍♂️ Let me guide you through setting up $subjName profile step-by-step!\n\nStep 1 of 7: What foods or cuisines do you love? (e.g. Tacos, Italian, Pizza, Sushi, Steak, Burgers)"
        val botMsg = ChatMessage(UUID.randomUUID().toString(), firstPrompt, false)
        _messages.value = _messages.value + botMsg
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        if (wizardStep > 0) {
            handleWizardInput(text)
            return
        }

        val userMsg = ChatMessage(UUID.randomUUID().toString(), text, true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            val lower = text.lowercase()

            if (pendingFriendName != null) {
                if (lower.contains("yes") || lower.contains("sure") || lower.contains("create") || text.length > 2) {
                    val friendName = pendingFriendName!!
                    val fullName = if (text.lowercase().contains(friendName.lowercase())) text else "$friendName $text"

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
                            ownerId = localUserId,
                            relatedPersonId = friendName,
                            relationshipType = "FRIEND",
                            trustState = "VERIFIED",
                            sharingPermissions = "ALL"
                        )
                    )

                    val confirmationMsg = ChatMessage(UUID.randomUUID().toString(), "Created profile for $fullName and added them to your social graph!", false)
                    _messages.value = _messages.value + confirmationMsg

                    pendingAssertionText?.let { pending ->
                        val (_, structuredAssertions) = chatEngine.generateStructuredResponse(pending)
                        for (sa in structuredAssertions) {
                            repository.saveAssertion(
                                Assertion(
                                    assertionId = UUID.randomUUID().toString(),
                                    observerId = localUserId,
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

            val isPlanCreationIntent = (lower.contains("set up") || lower.contains("plan") || lower.contains("organize") || lower.contains("schedule") || lower.contains("create")) && (lower.contains("dinner") || lower.contains("lunch") || lower.contains("food") || lower.contains("meal") || lower.contains("taco") || lower.contains("restaurant"))

            if (isPlanCreationIntent) {
                val allPersons = repository.getAllPersons()
                val myPerson = repository.getPerson(localUserId)
                val myDisplayName = myPerson?.displayName?.takeIf { it.isNotBlank() && it != localUserId } ?: "Me"

                // Extract mentioned participants from social graph
                val mentionedParticipants = allPersons.filter { p ->
                    lower.contains(p.displayName.lowercase()) || lower.contains(p.localPersonId.lowercase())
                }.map { it.displayName }.distinct()

                val participants = (listOf(myDisplayName) + mentionedParticipants).distinct()

                // Gather assertions for participants
                val participantAssertions = mutableMapOf<String, List<Assertion>>()
                for (pName in participants) {
                    val pObj = allPersons.find { it.displayName.equals(pName, ignoreCase = true) }
                    val pId = pObj?.localPersonId ?: pName
                    participantAssertions[pName] = repository.getAssertionsForSubject(pId)
                }

                val sampleRestaurants = listOf(
                    com.plansguru.data.model.Restaurant("1", "Suerte", "Interior Mexican", "$$$", true, listOf("Vegan", "Gluten-Free"), "Moderate", "Busy", "East Austin", "5:00 PM - 10:00 PM", listOf("Wheelchair Accessible"), zipCode = "78702"),
                    com.plansguru.data.model.Restaurant("2", "Loro", "Asian Smokehouse", "$$", true, listOf("Gluten-Free"), "Lively", "Busy", "South Lamar", "11:00 AM - 10:00 PM", listOf("Wheelchair Accessible"), zipCode = "78704"),
                    com.plansguru.data.model.Restaurant("3", "Uchi", "Japanese", "$$$$", true, listOf("Gluten-Free"), "Quiet", "Moderate", "South Lamar", "4:00 PM - 11:00 PM", listOf("Wheelchair Accessible"), zipCode = "78704"),
                    com.plansguru.data.model.Restaurant("4", "Arlo's Vegan Curbside", "Vegan Burgers", "$", true, listOf("100% Vegan"), "Moderate", "Moderate", "East Austin", "12:00 PM - 12:00 AM", listOf("Wheelchair Accessible"), zipCode = "78702")
                )
                val engine = com.plansguru.agent.NegotiationEngine()
                val results = engine.evaluateCandidates(sampleRestaurants, participantAssertions)
                val topRec = results.firstOrNull()

                if (topRec != null) {
                    val pId = "plan_${System.currentTimeMillis()}"
                    val planTitle = if (mentionedParticipants.isNotEmpty()) "Dinner with ${mentionedParticipants.joinToString(", ")}" else "Group Dinner Plan"

                    repository.savePlanRecord(
                        planId = pId,
                        title = planTitle,
                        restaurantName = topRec.restaurant.name,
                        cuisine = topRec.restaurant.cuisine,
                        price = topRec.restaurant.priceRange,
                        participants = participants,
                        consensusScore = "${(topRec.minSatisfaction * 100).toInt()}% Match",
                        date = "Active Now",
                        isUpcoming = true
                    )

                    val confirmationMsg = ChatMessage(
                        UUID.randomUUID().toString(),
                        "✨ I set up your dinner plan '$planTitle' at ${topRec.restaurant.name} (${topRec.restaurant.cuisine} · ${topRec.restaurant.priceRange})!\n\n• Participants: ${participants.joinToString(", ")}\n• Consensus Score: ${(topRec.minSatisfaction * 100).toInt()}% Match\n\nI've saved this plan to your Plans tab!",
                        false
                    )
                    _messages.value = _messages.value + confirmationMsg
                    return@launch
                }
            }

            val (replyText, structuredAssertions) = chatEngine.generateStructuredResponse(text)

            val newFriendSubject = structuredAssertions.map { it.subject }.firstOrNull { it != localUserId && repository.getPerson(it) == null }

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
                    val currentMe = repository.getPerson(localUserId) ?: Person(
                        localPersonId = localUserId,
                        displayName = "Me",
                        publicIdentityKey = getApplication<Application>().getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE).getString("public_key", "pub_key_local_user") ?: "pub_key_local_user",
                        creationTimestamp = System.currentTimeMillis(),
                        locationZip = sa.value
                    )
                    repository.savePerson(currentMe.copy(locationZip = sa.value))
                }

                val newAssertion = Assertion(
                    assertionId = UUID.randomUUID().toString(),
                    observerId = localUserId,
                    subjectId = sa.subject,
                    predicate = sa.predicate,
                    value = sa.value,
                    source = "LLM_STRUCTURED_PARSER",
                    confidence = sa.confidence,
                    validityTimestampStart = System.currentTimeMillis(),
                    validityTimestampEnd = null,
                    visibilityPolicy = "PRIVATE"
                )
                repository.saveAssertion(newAssertion)

                val syncClient = com.plansguru.data.sync.EncryptedSyncClient(getApplication())
                val friendsList = (repository.getAllPersons().map { it.localPersonId } + repository.getAllPersons().map { it.displayName }).distinct()
                syncClient.broadcastGraphUpdate(friendsList, newAssertion)
            }
        }
    }

    private fun handleWizardInput(text: String) {
        val userMsg = ChatMessage(UUID.randomUUID().toString(), text, true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            val (_, assertions) = chatEngine.generateStructuredResponse(text)

            for (sa in assertions) {
                val finalSubj = wizardSubject
                if (sa.predicate == "locationZip") {
                    val currentMe = repository.getPerson(finalSubj) ?: Person(
                        localPersonId = finalSubj,
                        displayName = if (finalSubj == localUserId) "Me" else finalSubj,
                        publicIdentityKey = "pub_key_$finalSubj",
                        creationTimestamp = System.currentTimeMillis(),
                        locationZip = sa.value
                    )
                    repository.savePerson(currentMe.copy(locationZip = sa.value))
                }

                repository.saveAssertion(
                    Assertion(
                        assertionId = UUID.randomUUID().toString(),
                        observerId = localUserId,
                        subjectId = finalSubj,
                        predicate = sa.predicate,
                        value = sa.value,
                        source = "GUIDED_WIZARD",
                        confidence = sa.confidence,
                        validityTimestampStart = System.currentTimeMillis(),
                        validityTimestampEnd = null,
                        visibilityPolicy = "PRIVATE"
                    )
                )
            }

            wizardStep++
            val nextPrompt = when (wizardStep) {
                2 -> "Awesome! Step 2 of 7: What foods do you dislike or want to avoid? (e.g. Spicy food, Fast Food, None)"
                3 -> "Got it! Step 3 of 7: Any dietary restrictions or medical allergies? (e.g. Vegan, Gluten-Free, Nut Allergy, Halal, Kosher, None)"
                4 -> "Step 4 of 7: 💵 What is your preferred budget or price tier for dinner? (e.g. $ Cheap/Under $15, $$ Moderate, $$$ Fine Dining)"
                5 -> "Step 5 of 7: ⏰ What days and time slots do you usually prefer for dining out? (e.g. Friday @ 7:30 PM, Weekend Brunch 11 AM, Weekdays 6-8 PM, Late Night)"
                6 -> "Step 6 of 7: What's your local zip code and maximum drive time for dinner? (e.g. 78626, under 20 minutes)"
                7 -> "Final Step 7 of 7: Do you prefer quiet, cozy spots or lively, vibrant atmospheres?"
                else -> {
                    wizardStep = 0
                    val targetName = if (wizardSubject == localUserId) "Your" else "$wizardSubject's"
                    "✨ $targetName profile setup is complete! All preferences (cuisines, dietary restrictions, budget, preferred dining times, drive radius, and vibe) have been structured and saved into your encrypted social graph database."
                }
            }

            val botMsg = ChatMessage(UUID.randomUUID().toString(), nextPrompt, false)
            _messages.value = _messages.value + botMsg
        }
    }
}
