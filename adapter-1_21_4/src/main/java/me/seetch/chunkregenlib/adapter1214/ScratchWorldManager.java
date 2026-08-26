package me.seetch.chunkregenlib.adapter1214;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Держит по одному "черновому" (scratch) Bukkit-миру на каждый исходный мир,
 * с тем же generator/seed/environment (аналог временного мира, который
 * WorldEdit создаёт под capital "worldeditregentempworld" для //regen).
 *
 * Генерация в этом мире никогда не читает и не пишет персистентные данные
 * целевого мира — это единственный безопасный способ заставить чанк-систему
 * Paper/Leaf по-настоящему сгенерировать чанк заново (включая структуры),
 * не трогая приватные внутренности Moonrise напрямую: сервер сам решает,
 * генерировать чанк или загружать его с диска, основываясь на том, есть ли
 * для него персистентные данные — а у чернового мира их нет никогда.
 *
 * Черновые миры не удаляются автоматически при отключении плагина-владельца —
 * их файлы остаются на диске между перезапусками сервера. Это осознанное
 * упрощение первой версии генерации (см. ADAPTERS.md).
 */
final class ScratchWorldManager {

    private static final Map<UUID, CompletableFuture<World>> SCRATCH_WORLDS = new ConcurrentHashMap<>();

    private ScratchWorldManager() {
    }

    static CompletableFuture<World> getOrCreate(Plugin owner, World sourceWorld) {
        return SCRATCH_WORLDS.computeIfAbsent(sourceWorld.getUID(), worldId -> {
            CompletableFuture<World> future = new CompletableFuture<>();
            Runnable createTask = () -> createScratchWorld(sourceWorld, worldId, future);
            if (Bukkit.isPrimaryThread()) {
                createTask.run();
            } else {
                Bukkit.getScheduler().runTask(owner, createTask);
            }
            return future;
        });
    }

    private static void createScratchWorld(World sourceWorld, UUID worldId, CompletableFuture<World> future) {
        try {
            String name = "chunkregenlib_scratch_" + worldId;
            World existing = Bukkit.getWorld(name);
            if (existing != null) {
                future.complete(existing);
                return;
            }

            World scratch = new WorldCreator(name).copy(sourceWorld).createWorld();
            if (scratch == null) {
                future.completeExceptionally(new IllegalStateException("Failed to create scratch world " + name));
                return;
            }

            scratch.setAutoSave(false);
            scratch.setKeepSpawnInMemory(false);
            future.complete(scratch);
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }
}
