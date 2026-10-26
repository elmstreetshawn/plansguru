package com.plansguru.data.db

import android.content.Context
import com.plansguru.data.crypto.KeyManager
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

class EncryptedGraphDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    private val passphrase = KeyManager.getOrCreateDatabasePassphrase(context)

    companion object {
        private const val DATABASE_NAME = "plansguru_secure.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS persons (
                localPersonId TEXT PRIMARY KEY,
                displayName TEXT NOT NULL,
                publicIdentityKey TEXT NOT NULL,
                creationTimestamp INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS assertions (
                assertionId TEXT PRIMARY KEY,
                observerId TEXT NOT NULL,
                subjectId TEXT NOT NULL,
                predicate TEXT NOT NULL,
                value TEXT NOT NULL,
                source TEXT NOT NULL,
                confidence REAL NOT NULL,
                validityTimestampStart INTEGER NOT NULL,
                validityTimestampEnd INTEGER,
                visibilityPolicy TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS relationships (
                ownerId TEXT NOT NULL,
                relatedPersonId TEXT NOT NULL,
                relationshipType TEXT NOT NULL,
                trustState TEXT NOT NULL,
                sharingPermissions TEXT NOT NULL,
                PRIMARY KEY (ownerId, relatedPersonId)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS assertions")
        db.execSQL("DROP TABLE IF EXISTS relationships")
        db.execSQL("DROP TABLE IF EXISTS persons")
        onCreate(db)
    }

    fun getWritableEncryptedDatabase(): SQLiteDatabase {
        return super.getWritableDatabase(passphrase)
    }

    fun getReadableEncryptedDatabase(): SQLiteDatabase {
        return super.getReadableDatabase(passphrase)
    }
}
