package com.typewritermc.mythicmobs.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SpawnerStateKeyTest {
    @Test
    fun `without a group the key is the historical world and spawner key`() {
        assertEquals("world_crypt", spawnerStateKey("world", "crypt", null))
    }

    @Test
    fun `two groups near the same spawner get separate state`() {
        assertNotEquals(
            spawnerStateKey("world", "crypt", "party_a"),
            spawnerStateKey("world", "crypt", "party_b"),
        )
    }

    @Test
    fun `a group never collides with the groupless state`() {
        assertNotEquals(spawnerStateKey("world", "crypt", null), spawnerStateKey("world", "crypt", ""))
    }

    @Test
    fun `the same group in two worlds has two states`() {
        assertNotEquals(
            spawnerStateKey("world", "crypt", "party_a"),
            spawnerStateKey("nether", "crypt", "party_a"),
        )
    }
}
