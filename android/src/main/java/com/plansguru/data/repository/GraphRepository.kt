package com.plansguru.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.plansguru.data.db.EncryptedGraphDatabase
import com.plansguru.data.model.Assertion
import com.plansguru.data.model.Person
import com.plansguru.data.model.Relationship

class GraphRepository(context: Context) {
    private val dbHelper = EncryptedGraphDatabase(context)

    fun savePerson(person: Person) {
        val db = dbHelper.getWritableEncryptedDatabase()
        val values = ContentValues().apply {
            put("localPersonId", person.localPersonId)
            put("displayName", person.displayName)
            put("publicIdentityKey", person.publicIdentityKey)
            put("creationTimestamp", person.creationTimestamp)
            put("locationZip", person.locationZip)
        }
        db.insertWithOnConflict("persons", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getPerson(personId: String): Person? {
        val db = dbHelper.getReadableEncryptedDatabase()
        val cursor = db.query("persons", null, "localPersonId = ?", arrayOf(personId), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) {
                val zipIdx = it.getColumnIndex("locationZip")
                val zip = if (zipIdx != -1) it.getString(zipIdx) else "90210"
                Person(
                    localPersonId = it.getString(it.getColumnIndexOrThrow("localPersonId")),
                    displayName = it.getString(it.getColumnIndexOrThrow("displayName")),
                    publicIdentityKey = it.getString(it.getColumnIndexOrThrow("publicIdentityKey")),
                    creationTimestamp = it.getLong(it.getColumnIndexOrThrow("creationTimestamp")),
                    locationZip = zip ?: "90210"
                )
            } else {
                null
            }
        }
    }

    fun getAllPersons(): List<Person> {
        val db = dbHelper.getReadableEncryptedDatabase()
        val list = mutableListOf<Person>()
        val cursor = db.query("persons", null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                val zipIdx = it.getColumnIndex("locationZip")
                val zip = if (zipIdx != -1) it.getString(zipIdx) else "90210"
                list.add(
                    Person(
                        localPersonId = it.getString(it.getColumnIndexOrThrow("localPersonId")),
                        displayName = it.getString(it.getColumnIndexOrThrow("displayName")),
                        publicIdentityKey = it.getString(it.getColumnIndexOrThrow("publicIdentityKey")),
                        creationTimestamp = it.getLong(it.getColumnIndexOrThrow("creationTimestamp")),
                        locationZip = zip ?: "90210"
                    )
                )
            }
        }
        return list
    }

    fun saveAssertion(assertion: Assertion) {
        val db = dbHelper.getWritableEncryptedDatabase()
        val values = ContentValues().apply {
            put("assertionId", assertion.assertionId)
            put("observerId", assertion.observerId)
            put("subjectId", assertion.subjectId)
            put("predicate", assertion.predicate)
            put("value", assertion.value)
            put("source", assertion.source)
            put("confidence", assertion.confidence)
            put("validityTimestampStart", assertion.validityTimestampStart)
            put("validityTimestampEnd", assertion.validityTimestampEnd ?: -1L)
            put("visibilityPolicy", assertion.visibilityPolicy)
        }
        db.insertWithOnConflict("assertions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAssertionsForSubject(subjectId: String): List<Assertion> {
        val db = dbHelper.getReadableEncryptedDatabase()
        val list = mutableListOf<Assertion>()
        val cursor = db.query("assertions", null, "subjectId = ?", arrayOf(subjectId), null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                val end = it.getLong(it.getColumnIndexOrThrow("validityTimestampEnd"))
                list.add(
                    Assertion(
                        assertionId = it.getString(it.getColumnIndexOrThrow("assertionId")),
                        observerId = it.getString(it.getColumnIndexOrThrow("observerId")),
                        subjectId = it.getString(it.getColumnIndexOrThrow("subjectId")),
                        predicate = it.getString(it.getColumnIndexOrThrow("predicate")),
                        value = it.getString(it.getColumnIndexOrThrow("value")),
                        source = it.getString(it.getColumnIndexOrThrow("source")),
                        confidence = it.getFloat(it.getColumnIndexOrThrow("confidence")),
                        validityTimestampStart = it.getLong(it.getColumnIndexOrThrow("validityTimestampStart")),
                        validityTimestampEnd = if (end == -1L) null else end,
                        visibilityPolicy = it.getString(it.getColumnIndexOrThrow("visibilityPolicy"))
                    )
                )
            }
        }
        return list
    }

    fun saveRelationship(relationship: Relationship) {
        val db = dbHelper.getWritableEncryptedDatabase()
        val values = ContentValues().apply {
            put("ownerId", relationship.ownerId)
            put("relatedPersonId", relationship.relatedPersonId)
            put("relationshipType", relationship.relationshipType)
            put("trustState", relationship.trustState)
            put("sharingPermissions", relationship.sharingPermissions)
        }
        db.insertWithOnConflict("relationships", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteGeneralPreferences() {
        val db = dbHelper.getWritableEncryptedDatabase()
        db.delete("assertions", "predicate = ?", arrayOf("general_preference"))
    }

    fun deleteAssertion(assertionId: String) {
        val db = dbHelper.getWritableEncryptedDatabase()
        db.delete("assertions", "assertionId = ?", arrayOf(assertionId))
    }

    fun deletePerson(personId: String) {
        val db = dbHelper.getWritableEncryptedDatabase()
        db.delete("persons", "localPersonId = ?", arrayOf(personId))
        db.delete("assertions", "subjectId = ?", arrayOf(personId))
        db.delete("relationships", "relatedPersonId = ?", arrayOf(personId))
    }
}
