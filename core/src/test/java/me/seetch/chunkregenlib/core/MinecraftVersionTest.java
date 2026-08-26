package me.seetch.chunkregenlib.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftVersionTest {

    @Test
    void parsesBukkitVersionString() {
        MinecraftVersion version = MinecraftVersion.parse("1.21.4-R0.1-SNAPSHOT");

        assertEquals(new MinecraftVersion(1, 21, 4), version);
        assertEquals("1_21_4", version.asKey());
    }

    @Test
    void parsesVersionWithoutPatch() {
        MinecraftVersion version = MinecraftVersion.parse("1.21");

        assertEquals(new MinecraftVersion(1, 21, 0), version);
    }

    @Test
    void rejectsUnparsableString() {
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.parse("not-a-version"));
    }

    @Test
    void comparesByMajorMinorPatch() {
        MinecraftVersion older = MinecraftVersion.parse("1.21.3");
        MinecraftVersion newer = MinecraftVersion.parse("1.21.4");

        assertTrue(older.compareTo(newer) < 0);
        assertTrue(newer.compareTo(older) > 0);
    }
}
