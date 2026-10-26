package com.plansguru.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.data.model.Person
import com.plansguru.data.repository.GraphRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GraphRepository(application)

    private val _profileState = MutableStateFlow<Person?>(null)
    val profileState: StateFlow<Person?> = _profileState.asStateFlow()

    init {
        loadProfile("local_user")
    }

    fun loadProfile(personId: String) {
        viewModelScope.launch {
            val person = repository.getPerson(personId)
            if (person != null) {
                _profileState.value = person
            } else {
                // Create default local profile
                val newPerson = Person(
                    localPersonId = personId,
                    displayName = "Me (PlansGuru)",
                    publicIdentityKey = UUID.randomUUID().toString(),
                    creationTimestamp = System.currentTimeMillis()
                )
                repository.savePerson(newPerson)
                _profileState.value = newPerson
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
}
