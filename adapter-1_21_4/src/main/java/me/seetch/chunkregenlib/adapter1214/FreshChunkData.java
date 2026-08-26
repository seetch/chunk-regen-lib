package me.seetch.chunkregenlib.adapter1214;

import net.minecraft.world.level.chunk.storage.SerializableChunkData;

/**
 * Снимок свежесгенерированного чанка (секции, block entities, heightmaps),
 * захваченный из чернового мира (см. ScratchWorldManager) через
 * SerializableChunkData#copyOf. Не привязан к конкретному ServerLevel —
 * содержит независимые копии данных, безопасен для передачи между потоками.
 * Внутренний тип адаптера, не часть публичного API.
 */
record FreshChunkData(SerializableChunkData snapshot) {
}
