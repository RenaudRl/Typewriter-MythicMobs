package com.typewritermc.mythicmobs.services

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupStateRegistryTest {

    private val keyA = spawnerStateKey("world", "spawner", "party_a")
    private val keyB = spawnerStateKey("world", "spawner", "party_b")
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()
    private val carol = UUID.randomUUID()

    @Test
    fun `a key is kept while one of its members is online`() {
        val registry = GroupStateRegistry()
        registry.record(keyA, alice)
        registry.record(keyA, bob)

        assertTrue(registry.release(setOf(bob)).isEmpty())
    }

    @Test
    fun `a key is released once its last member is gone, only once`() {
        val registry = GroupStateRegistry()
        registry.record(keyA, alice)
        registry.record(keyB, carol)

        assertEquals(listOf(keyA), registry.release(setOf(carol)))
        assertTrue(registry.release(setOf(carol)).isEmpty())
    }

    @Test
    fun `a member seen again after the release registers the key anew`() {
        val registry = GroupStateRegistry()
        registry.record(keyA, alice)
        registry.release(emptySet())

        registry.record(keyA, alice)

        assertEquals(listOf(keyA), registry.release(emptySet()))
    }

    @Test
    fun `nothing is released when nothing was recorded`() {
        assertTrue(GroupStateRegistry().release(emptySet()).isEmpty())
    }
}
