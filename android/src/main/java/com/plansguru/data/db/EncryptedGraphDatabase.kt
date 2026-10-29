package com.plansguru.data.db

import android.content.Context
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import com.plansguru.data.crypto.KeyManager

class EncryptedGraphDatabase(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        SQLiteDatabase.loadLibs(context)
    }

    companion object {
        private const val DATABASE_NAME = "plansguru_secure.db"
        private const val DATABASE_VERSION = 3
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS persons (
                localPersonId TEXT PRIMARY KEY,
                displayName TEXT NOT NULL,
                publicIdentityKey TEXT NOT NULL,
                creationTimestamp INTEGER NOT NULL,
                locationZip TEXT
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

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS plans (
                planId TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                restaurantName TEXT NOT NULL,
                cuisine TEXT NOT NULL,
                price TEXT NOT NULL,
                participants TEXT NOT NULL,
                consensusScore TEXT NOT NULL,
                date TEXT NOT NULL,
                isUpcoming INTEGER NOT NULL DEFAULT 1
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE persons ADD COLUMN locationZip TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS plans (
                        planId TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        restaurantName TEXT NOT NULL,
                        cuisine TEXT NOT NULL,
                        price TEXT NOT NULL,
                        participants TEXT NOT NULL,
                        consensusScore TEXT NOT NULL,
                        date TEXT NOT NULL,
                        isUpcoming INTEGER NOT NULL DEFAULT 1
                    )
                """)
            } catch (_: Exception) {}
        }
    }

    fun getWritableEncryptedDatabase(): SQLiteDatabase {
        val passphrase = KeyManager.getSqlCipherPassphrase(context)
        return try {
            getWritableDatabase(passphrase)
        } catch (e: Exception) {
            context.deleteDatabase(DATABASE_NAME)
            getWritableDatabase(passphrase)
        }
    }

    fun getReadableEncryptedDatabase(): SQLiteDatabase {
        val passphrase = KeyManager.getSqlCipherPassphrase(context)
        return try {
            getReadableDatabase(passphrase)
        } catch (e: Exception) {
            context.deleteDatabase(DATABASE_NAME)
            getReadableDatabase(passphrase)
        }
    }
}
