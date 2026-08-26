package me.seetch.chunkregenlib.core;

import me.seetch.chunkregenlib.api.ChunkRegenerator;
import org.bukkit.plugin.Plugin;

/**
 * SPI, реализуемый каждым adapter-* модулем. Регистрируется через
 * META-INF/services/me.seetch.chunkregenlib.core.ChunkRegenAdapterProvider,
 * чтобы ChunkRegenLibService мог найти его через java.util.ServiceLoader
 * без прямой compile-зависимости core от конкретного adapter-модуля.
 */
public interface ChunkRegenAdapterProvider {

    /** Версия сервера, под которую написан адаптер. */
    MinecraftVersion supportedVersion();

    /**
     * Создаёт реализацию ChunkRegenerator для плагина-владельца.
     *
     * @param owner плагин, от имени которого адаптер будет планировать задачи
     *              (используется как контекст для BukkitScheduler/RegionScheduler)
     */
    ChunkRegenerator create(Plugin owner);
}
