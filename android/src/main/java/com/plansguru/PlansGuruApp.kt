package com.plansguru

import android.app.Application

class PlansGuruApp : Application() {
    override fun onCreate() {
        super.onCreate()
        net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
    }
}
