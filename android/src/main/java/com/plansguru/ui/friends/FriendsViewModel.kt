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

    init {
        loadFriends()
    }

    fun loadFriends() {
        viewModelScope.launch {
            // For POC, load relationships for local_user
            // In full app, queried from SQLCipher
            _friendsState.value = listOf(
                Relationship("local_user", "friend_mike", "FRIEND", "VERIFIED", "ALL"),
                Relationship("local_user", "friend_sarah", "FRIEND", "VERIFIED", "ALL"),
                Relationship("local_user", "friend_jen", "FRIEND", "VERIFIED", "ALL")
            )
        }
    }

    fun addFriend(friendName: String) {
        val rel = Relationship(
            ownerId = "local_user",
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
