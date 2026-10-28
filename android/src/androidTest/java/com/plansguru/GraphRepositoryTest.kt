package com.plansguru

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plansguru.data.model.Assertion
import com.plansguru.data.model.Person
import com.plansguru.data.repository.GraphRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GraphRepositoryTest {
    private lateinit var graphRepository: GraphRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        graphRepository = GraphRepository(context)
    }

    @Test
    fun testSaveAndGetPerson() {
        val person = Person("test_user", "Test User", "pub_key_123", System.currentTimeMillis())
        graphRepository.savePerson(person)

        val retrieved = graphRepository.getPerson("test_user")
        assertNotNull(retrieved)
        assertEquals("Test User", retrieved?.displayName)
    }

    @Test
    fun testSaveAndGetAssertions() {
        val assertion = Assertion(
            assertionId = "test_assertion_1",
            observerId = "test_user",
            subjectId = "test_user",
            predicate = "likes_cuisine",
            value = "Japanese",
            source = "SELF",
            confidence = 1.0f,
            validityTimestampStart = System.currentTimeMillis(),
            validityTimestampEnd = null,
            visibilityPolicy = "PRIVATE"
        )
        graphRepository.saveAssertion(assertion)

        val assertions = graphRepository.getAssertionsForSubject("test_user")
        assertEquals(1, assertions.size)
        assertEquals("Japanese", assertions[0].value)
    }
}
