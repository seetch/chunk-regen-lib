package me.seetch.chunkregenlib.adaptercommon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Определяет, требуется ли выполнение на конкретном региональном потоке
 * (Folia-подобные форки), и переставляет задачу на владеющий поток чанка,
 * не блокируя вызывающий поток.
 *
 * На обычном (нерегионализированном) Paper/Leaf задача просто выполняется
 * на главном потоке через BukkitScheduler.
 */
public final class SchedulingSupport {

    private static final boolean REGIONIZED = detectRegionizedThreading();

    private SchedulingSupport() {
    }

    /** true, если сервер использует регионализированную многопоточность (Folia и форки). */
    public static boolean isRegionized() {
        return REGIONIZED;
    }

    /**
     * Выполняет task на потоке, владеющем указанным чанком, и возвращает
     * future с результатом. На Folia использует RegionScheduler, привязанный
     * к местоположению чанка; на обычном Paper/Leaf — главный поток сервера.
     */
    public static <T> CompletableFuture<T> callOnOwningThread(
            Plugin plugin,
            World world,
            int chunkX,
            int chunkZ,
            Supplier<T> task
    ) {
        CompletableFuture<T> future = new CompletableFuture<>();

        if (REGIONIZED) {
            // Координата Y не влияет на выбор региона по X/Z, но обязательна для Location.
            Location regionAnchor = new Location(world, (chunkX << 4) + 8, 64, (chunkZ << 4) + 8);
            Bukkit.getRegionScheduler().run(plugin, regionAnchor, scheduledTask -> completeSafely(future, task));
        } else if (Bukkit.isPrimaryThread()) {
            completeSafely(future, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> completeSafely(future, task));
        }

        return future;
    }

    private static <T> void completeSafely(CompletableFuture<T> future, Supplier<T> task) {
        try {
            future.complete(task.get());
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    private static boolean detectRegionizedThreading() {
        // TODO: сверить точное имя класса-маркера Folia в целевой версии Paper API
        // перед компиляцией — использовавшееся ранее io.papermc.paper.threadedregions.RegionizedServer
        // относится к внутренним пакетам Paper и может измениться между версиями.
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
