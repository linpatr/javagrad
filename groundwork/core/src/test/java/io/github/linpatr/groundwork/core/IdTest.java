package io.github.linpatr.groundwork.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdTest {
    @Test
    void parsesNamespacedValues() {
        assertEquals(new Id("groundwork", "machines/smelter"), Id.parse("groundwork:machines/smelter"));
    }

    @Test
    void defaultsToMinecraftNamespace() {
        assertEquals(new Id("minecraft", "stone"), Id.parse("stone"));
    }

    @Test
    void rejectsCharactersMinecraftRejects() {
        assertThrows(IllegalArgumentException.class, () -> Id.parse("Minecraft:stone"));
        assertThrows(IllegalArgumentException.class, () -> Id.parse("minecraft:stone block"));
        assertThrows(IllegalArgumentException.class, () -> Id.parse("mod/ns:stone"));
        assertThrows(IllegalArgumentException.class, () -> Id.parse("minecraft:"));
    }

    @Test
    void roundTripsThroughToString() {
        assertEquals("groundwork:smelter", Id.parse("groundwork:smelter").toString());
    }
}
