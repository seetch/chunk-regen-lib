package me.seetch.chunkregenlib.adaptercommon;

import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Вспомогательные операции над соседними чанками, нужные адаптерам перед
 * пересветом (relight) границ регенерированного чанка. Не форсирует загрузку
 * соседей — только сообщает, какие из них недоступны, чтобы вызывающий адаптер
 * мог зафиксировать предупреждение в RegenerationResult, как того
 * требует спецификация (не грузить чанки "по умолчанию").
 */
public final class NeighborChunkSupport {

    private NeighborChunkSupport() {
    }

    /** Восемь соседей чанка (включая диагональные) — граница, которую задевает relight. */
    public static List<int[]> neighborCoordinates(int chunkX, int chunkZ) {
        List<int[]> neighbors = new ArrayList<>(8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                neighbors.add(new int[] {chunkX + dx, chunkZ + dz});
            }
        }
        return neighbors;
    }

    /**
     * Возвращает предупреждения для каждого не загруженного соседа —
     * пересвет по этой границе может остаться неполным.
     */
    public static List<String> unloadedNeighborWarnings(World world, int chunkX, int chunkZ) {
        List<String> warnings = new ArrayList<>();
        for (int[] neighbor : neighborCoordinates(chunkX, chunkZ)) {
            if (!world.isChunkLoaded(neighbor[0], neighbor[1])) {
                warnings.add("Neighbor chunk [" + neighbor[0] + ", " + neighbor[1]
                        + "] is not loaded, relight across this border may be incomplete");
            }
        }
        return warnings;
    }
}
