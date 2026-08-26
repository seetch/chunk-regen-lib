package me.seetch.chunkregenlib.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegenerationOptionsTest {

    @Test
    void defaultsMatchSpec() {
        RegenerationOptions options = RegenerationOptions.defaults();

        assertTrue(options.regenerateBiomes());
        assertTrue(options.regenerateStructures());
        assertEquals(ChunkGenStage.FULL, options.targetStatus());
        assertTrue(options.preservePlayers());
        assertEquals(EntityRegenPolicy.REMOVE_ALL_NON_PLAYER, options.entityPolicy());
        assertTrue(options.rebuildPointsOfInterest());
        assertTrue(options.relight());
        assertTrue(options.notifyViewers());
        assertTrue(options.saveAfter());
        assertEquals(5_000L, options.timeoutMillis());
        assertEquals(Integer.MIN_VALUE, options.minY());
        assertEquals(Integer.MAX_VALUE, options.maxY());
    }

    @Test
    void builderOverridesDefaults() {
        RegenerationOptions options = RegenerationOptions.builder()
                .regenerateBiomes(false)
                .targetStatus(ChunkGenStage.SURFACE)
                .entityPolicy(EntityRegenPolicy.KEEP_ALL)
                .timeoutMillis(15_000L)
                .build();

        assertEquals(false, options.regenerateBiomes());
        assertEquals(ChunkGenStage.SURFACE, options.targetStatus());
        assertEquals(EntityRegenPolicy.KEEP_ALL, options.entityPolicy());
        assertEquals(15_000L, options.timeoutMillis());
    }

    @Test
    void rejectsNonPositiveTimeout() {
        RegenerationOptions.Builder builder = RegenerationOptions.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.timeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> builder.timeoutMillis(-1));
    }

    @Test
    void yRangeIsApplied() {
        RegenerationOptions options = RegenerationOptions.builder()
                .yRange(50, 140)
                .build();

        assertEquals(50, options.minY());
        assertEquals(140, options.maxY());
    }

    @Test
    void rejectsInvertedYRange() {
        RegenerationOptions.Builder builder = RegenerationOptions.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.yRange(140, 50));
    }

    @Test
    void scopeIsDerivedFromOptions() {
        RegenerationOptions options = RegenerationOptions.builder()
                .regenerateBiomes(false)
                .entityPolicy(EntityRegenPolicy.KEEP_ALL)
                .build();

        RegenerationScope scope = RegenerationScope.from(options);

        assertTrue(scope.blocks());
        assertEquals(false, scope.biomes());
        assertEquals(false, scope.entities());
    }
}
