package dev.stevanmosquetti.openplaud.slugify

import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SlugifierTest {

    @Test
    fun derivesLowercaseAlphanumericLocalpart() {
        assertEquals("johndoe", Slugifier.slugify("John Doe"))
        assertEquals("john123", Slugifier.slugify("john 123"))
    }

    @Test
    fun stripsNonAlphanumericCharacters() {
        assertEquals("johndoedept", Slugifier.slugify("John.Doe/Dept"))
    }

    @Test
    fun acceptsExactlyTwoCharacters() {
        assertEquals("ab", Slugifier.slugify("aB"))
    }

    @Test
    fun acceptsExactlyThirtyCharacters() {
        val name = "a".repeat(30)
        assertEquals(name, Slugifier.slugify(name))
    }

    @Test
    fun rejectsSingleCharacterResult() {
        assertFailsWith<IllegalArgumentException> { Slugifier.slugify("a") }
    }

    @Test
    fun rejectsResultLongerThanThirtyCharacters() {
        assertFailsWith<IllegalArgumentException> { Slugifier.slugify("a".repeat(31)) }
    }

    @Test
    fun rejectsNamesWithoutAlphanumerics() {
        assertFailsWith<IllegalArgumentException> { Slugifier.slugify("!?..//") }
    }
}
