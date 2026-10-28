package com.plansguru.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.data.api.ServerApiClient
import com.plansguru.data.crypto.KeyManager
import com.plansguru.data.model.Person
import com.plansguru.data.repository.GraphRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GraphRepository(application)
    private val serverApi = ServerApiClient(application)

    private val _profileState = MutableStateFlow<Person?>(null)
    val profileState: StateFlow<Person?> = _profileState.asStateFlow()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    init {
        loadProfile("local_user")
    }

    fun loadProfile(personId: String) {
        viewModelScope.launch {
            val person = repository.getPerson(personId)
            if (person != null) {
                _profileState.value = person
            } else {
                // Create default local profile with proper keypair
                registerNewUser("Me")
            }
        }
    }

    fun registerNewUser(displayName: String) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Registering

            try {
                // Generate keypair locally
                val keyPair = KeyManager.getOrCreateIdentityKeyPair(getApplication())
                val publicKeyHex = keyPair.public.encoded.joinToString("") { "%02x".format(it) }
                val userId = UUID.randomUUID().toString()

                // Save to local database first
                val newPerson = Person(
                    localPersonId = userId,
                    displayName = displayName,
                    publicIdentityKey = publicKeyHex,
                    creationTimestamp = System.currentTimeMillis()
                )
                repository.savePerson(newPerson)
                _profileState.value = newPerson

                // Store userId and publicKey in preferences for later use
                val prefs = getApplication<Application>().getSharedPreferences(
                    "plansguru_prefs",
                    android.content.Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString("user_id", userId)
                    .putString("public_key", publicKeyHex)
                    .apply()

                // Register with server
                val result = serverApi.registerUser(userId, publicKeyHex, displayName)
                result.onSuccess { response ->
                    _registrationState.value = RegistrationState.Success(userId)
                }.onFailure { error ->
                    // Local save succeeded even if server registration failed
                    // User can still use app locally
                    _registrationState.value = RegistrationState.PartialSuccess(
                        userId,
                        "Registered locally. Server sync will retry: ${error.message}"
                    )
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateDisplayName(newName: String) {
        val current = _profileState.value ?: return
        val updated = current.copy(displayName = newName)
        viewModelScope.launch {
            repository.savePerson(updated)
            _profileState.value = updated
        }
    }

    fun getUserId(): String? {
        val prefs = getApplication<Application>().getSharedPreferences(
            "plansguru_prefs",
            android.content.Context.MODE_PRIVATE
        )
        return prefs.getString("user_id", null)
    }

    fun getPublicKey(): String? {
        val prefs = getApplication<Application>().getSharedPreferences(
            "plansguru_prefs",
            android.content.Context.MODE_PRIVATE
        )
        return prefs.getString("public_key", null)
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Registering : RegistrationState()
    data class Success(val userId: String) : RegistrationState()
    data class PartialSuccess(val userId: String, val message: String) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}

