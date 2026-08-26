package me.seetch.chunkregenlib.core;

import java.util.Objects;
import java.util.UUID;

/**
 * Идентификатор чанка, независимый от живого объекта World —
 * безопасен для использования как ключ карты, не удерживает мир от выгрузки.
 */
public record ChunkKey(UUID worldId, int chunkX, int chunkZ) {

    public ChunkKey {
        Objects.requireNonNull(worldId, "worldId");
    }
}
