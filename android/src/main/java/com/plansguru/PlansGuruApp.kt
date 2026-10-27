package com.plansguru

import android.app.Application
import com.plansguru.data.sync.EncryptedSyncClient

class PlansGuruApp : Application() {
    lateinit var syncClient: EncryptedSyncClient
        private set

    override fun onCreate() {
        super.onCreate()
        syncClient = EncryptedSyncClient("local_user")
        syncClient.connect("ws://192.168.2.37:8000/ws/local_user")
    }
}
