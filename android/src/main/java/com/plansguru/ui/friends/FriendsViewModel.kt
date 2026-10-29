package com.plansguru.ui.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plansguru.data.model.Relationship
import com.plansguru.data.repository.GraphRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GraphRepository(application)

    private val _friendsState = MutableStateFlow<List<Relationship>>(emptyList())
    val friendsState: StateFlow<List<Relationship>> = _friendsState.asStateFlow()

    private val localUserId: String
        get() = getApplication<Application>().getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE).getString("user_id", "local_user") ?: "local_user"

    init {
        loadFriends()
    }

    fun loadFriends() {
        viewModelScope.launch {
            // For POC, load relationships for localUserId
            // In full app, queried from SQLCipher
            _friendsState.value = listOf(
                Relationship(localUserId, "friend_mike", "FRIEND", "VERIFIED", "ALL"),
                Relationship(localUserId, "friend_sarah", "FRIEND", "VERIFIED", "ALL"),
                Relationship(localUserId, "friend_jen", "FRIEND", "VERIFIED", "ALL")
            )
        }
    }

    fun addFriend(friendName: String) {
        val rel = Relationship(
            ownerId = localUserId,
            relatedPersonId = "friend_${System.currentTimeMillis()}",
            relationshipType = "FRIEND",
            trustState = "VERIFIED",
            sharingPermissions = "ALL"
        )
        viewModelScope.launch {
            repository.saveRelationship(rel)
            loadFriends()
        }
    }
}
