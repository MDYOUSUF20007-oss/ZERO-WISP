package com.zerowisp.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextCleanerTest {

    @Test
    fun `removes blank audio marker`() {
        val result = TextCleaner.clean("[BLANK_AUDIO]")
        assertEquals("", result.trim())
    }

    @Test
    fun `removes music marker`() {
        val result = TextCleaner.clean("[Music] hello world")
        assertFalse(result.contains("[Music]", ignoreCase = true))
        assertTrue(result.contains("hello", ignoreCase = true))
    }

    @Test
    fun `removes filler words when enabled`() {
        val result = TextCleaner.clean("Um, I think, uh, we should go", cleanFillers = true)
        assertFalse(result.contains("um", ignoreCase = true))
        assertFalse(result.contains("uh", ignoreCase = true))
        assertTrue(result.contains("think"))
        assertTrue(result.contains("go"))
    }

    @Test
    fun `keeps filler words when disabled`() {
        val result = TextCleaner.clean("Um, I think", cleanFillers = false)
        assertTrue(result.contains("Um", ignoreCase = true))
    }

    @Test
    fun `collapses ellipsis`() {
        val result = TextCleaner.clean("Hello... world")
        assertFalse(result.contains("..."))
        assertTrue(result.contains("Hello"))
        assertTrue(result.contains("world"))
    }

    @Test
    fun `capitalizes first letter`() {
        val result = TextCleaner.clean("hello world")
        assertTrue(result.startsWith("H"))
    }

    @Test
    fun `handles empty string`() {
        val result = TextCleaner.clean("")
        assertEquals("", result)
    }

    @Test
    fun `removes inaudible marker`() {
        val result = TextCleaner.clean("I said [inaudible] hello")
        assertFalse(result.contains("[inaudible]", ignoreCase = true))
        assertTrue(result.contains("hello"))
    }

    @Test
    fun `normalizes multiple spaces`() {
        val result = TextCleaner.clean("hello   world")
        assertFalse(result.contains("  "))
    }

    @Test
    fun `handles you know filler`() {
        val result = TextCleaner.clean("you know what I mean", cleanFillers = true)
        assertFalse(result.lowercase().startsWith("you know"))
        assertTrue(result.contains("mean"))
    }
}
