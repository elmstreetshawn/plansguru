package com.plansguru

import android.app.Application
import android.content.Context
import com.plansguru.data.sync.EncryptedSyncClient

class PlansGuruApp : Application() {
    lateinit var syncClient: EncryptedSyncClient
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", "local_user") ?: "local_user"
        val sessionTopic = "${userId}_session"
        syncClient = EncryptedSyncClient(this, sessionTopic)
        syncClient.connect()
    }
}
