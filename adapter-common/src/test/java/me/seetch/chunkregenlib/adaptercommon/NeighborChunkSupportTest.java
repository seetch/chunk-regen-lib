package me.seetch.chunkregenlib.adaptercommon;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NeighborChunkSupportTest {

    @Test
    void returnsEightNeighbors() {
        List<int[]> neighbors = NeighborChunkSupport.neighborCoordinates(10, -5);

        assertEquals(8, neighbors.size());
    }

    @Test
    void reportsNoWarningsWhenAllNeighborsLoaded() {
        World world = mock(World.class);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);

        List<String> warnings = NeighborChunkSupport.unloadedNeighborWarnings(world, 0, 0);

        assertTrue(warnings.isEmpty());
    }

    @Test
    void reportsWarningForEachUnloadedNeighbor() {
        World world = mock(World.class);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);

        List<String> warnings = NeighborChunkSupport.unloadedNeighborWarnings(world, 0, 0);

        assertEquals(8, warnings.size());
    }
}
