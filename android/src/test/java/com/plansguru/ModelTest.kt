package com.plansguru

import com.plansguru.data.model.Person
import com.plansguru.data.model.Assertion
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTest {
    @Test
    fun testPersonCreation() {
        val person = Person("p1", "Alice", "key123", 1000L)
        assertEquals("p1", person.localPersonId)
        assertEquals("Alice", person.displayName)
    }

    @Test
    fun testAssertionCreation() {
        val assertion = Assertion("a1", "p1", "p1", "likes_cuisine", "Italian", "SELF", 1.0f, 1000L, null, "PRIVATE")
        assertEquals("likes_cuisine", assertion.predicate)
        assertEquals("Italian", assertion.value)
    }
}
