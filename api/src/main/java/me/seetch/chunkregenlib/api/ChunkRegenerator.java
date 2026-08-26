package me.seetch.chunkregenlib.api;

import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

/**
 * Точка входа библиотеки: надёжная регенерация уже загруженного (активного)
 * чанка на месте, без использования устаревшего World#regenerateChunk.
 *
 * Реализация получается через Bukkit.getServicesManager().load(ChunkRegenerator.class)
 * (если библиотека используется как плагин-провайдер) либо напрямую от
 * ChunkRegenLibService, если библиотека зашита (shaded) внутрь плагина-потребителя.
 *
 * Все методы потокобезопасны и могут вызываться из любого потока — сама операция
 * будет переставлена на поток/регион, которому принадлежит чанк.
 */
public interface ChunkRegenerator {

    /**
     * Запускает регенерацию чанка по мировым координатам чанка.
     *
     * @param world   мир, которому принадлежит чанк
     * @param chunkX  координата чанка по X (не блока)
     * @param chunkZ  координата чанка по Z (не блока)
     * @param options параметры операции
     * @return future, завершающийся результатом регенерации; никогда не завершается
     *         исключением напрямую — ошибки инкапсулированы в RegenerationResult#throwable()
     */
    CompletableFuture<RegenerationResult> regenerate(World world, int chunkX, int chunkZ, RegenerationOptions options);

    /**
     * Запускает регенерацию уже загруженного чанка.
     *
     * @param chunk   загруженный чанк
     * @param options параметры операции
     * @return future, завершающийся результатом регенерации
     */
    CompletableFuture<RegenerationResult> regenerate(Chunk chunk, RegenerationOptions options);

    /**
     * Проверяет, выполняется ли в данный момент операция регенерации для указанного чанка.
     * Не даёт гарантий на будущее (TOCTOU) — используется как быстрая неблокирующая проверка,
     * а не как замена внутренней синхронизации.
     */
    boolean isChunkBusy(World world, int chunkX, int chunkZ);
}
