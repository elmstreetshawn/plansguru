package com.plansguru

import android.app.Application
import com.plansguru.data.sync.EncryptedSyncClient

class PlansGuruApp : Application() {
    lateinit var syncClient: EncryptedSyncClient
        private set

    override fun onCreate() {
        super.onCreate()
        syncClient = EncryptedSyncClient(this, "local_user_session")
        syncClient.connect()
    }
}
