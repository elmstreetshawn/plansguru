package com.plansguru.ui.preferences

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.data.model.Assertion
import com.plansguru.data.repository.GraphRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GraphRepository(application)

    private val _assertionsState = MutableStateFlow<List<Assertion>>(emptyList())
    val assertionsState: StateFlow<List<Assertion>> = _assertionsState.asStateFlow()

    init {
        loadPreferences()
    }

    fun loadPreferences() {
        val localUserId = getApplication<Application>().getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE).getString("user_id", "local_user") ?: "local_user"
        viewModelScope.launch {
            val list = repository.getAssertionsForSubject(localUserId)
            _assertionsState.value = list
        }
    }

    fun addPreference(predicate: String, value: String) {
        val localUserId = getApplication<Application>().getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE).getString("user_id", "local_user") ?: "local_user"
        val assertion = Assertion(
            assertionId = UUID.randomUUID().toString(),
            observerId = localUserId,
            subjectId = localUserId,
            predicate = predicate,
            value = value,
            source = "SELF",
            confidence = 1.0f,
            validityTimestampStart = System.currentTimeMillis(),
            validityTimestampEnd = null,
            visibilityPolicy = "AUTHORIZED_PEERS"
        )
        viewModelScope.launch {
            repository.saveAssertion(assertion)
            loadPreferences()
        }
    }
}
