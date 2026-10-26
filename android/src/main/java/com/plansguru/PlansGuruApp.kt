package com.plansguru

import android.app.Application

class PlansGuruApp : Application() {
    override fun onCreate() {
        super.onCreate()
        net.zetetic.database.sqlcipher.SQLiteDatabase.loadLibs(this)
    }
}
