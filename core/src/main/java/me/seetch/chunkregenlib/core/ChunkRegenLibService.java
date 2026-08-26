package me.seetch.chunkregenlib.core;

import me.seetch.chunkregenlib.api.ChunkRegenerator;
import me.seetch.chunkregenlib.api.exceptions.UnsupportedServerVersionException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.ServiceLoader;

/**
 * Точка входа библиотеки. Определяет версию сервера, находит подходящий
 * ChunkRegenAdapterProvider через ServiceLoader и создаёт (при необходимости —
 * регистрирует в ServicesManager) реализацию ChunkRegenerator.
 *
 * Никогда не пытается выполнить операцию на версии, для которой нет
 * зарегистрированного адаптера — в этом случае выбрасывает
 * UnsupportedServerVersionException.
 */
public final class ChunkRegenLibService {

    private ChunkRegenLibService() {
    }

    /**
     * Создаёт ChunkRegenerator для текущей версии сервера без регистрации
     * в ServicesManager — подходит для режима "зашитой" (shaded) зависимости,
     * когда библиотека используется приватно одним плагином.
     *
     * @throws UnsupportedServerVersionException если под текущую версию сервера
     *         не найдено ни одного адаптера
     */
    public static ChunkRegenerator createFor(Plugin owner) {
        MinecraftVersion serverVersion = MinecraftVersion.parse(Bukkit.getBukkitVersion());

        return ServiceLoader.load(ChunkRegenAdapterProvider.class, ChunkRegenLibService.class.getClassLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.supportedVersion().equals(serverVersion))
                .findFirst()
                .map(provider -> provider.create(owner))
                .orElseThrow(() -> new UnsupportedServerVersionException(serverVersion.toString()));
    }

    /**
     * Как createFor(Plugin), но дополнительно регистрирует результат
     * в Bukkit.getServicesManager(), чтобы другие плагины могли получить
     * ту же самую загруженную версию через
     * Bukkit.getServicesManager().load(ChunkRegenerator.class) —
     * используется режимом плагина-провайдера (plugin-bootstrap).
     */
    public static ChunkRegenerator createAndRegister(Plugin owner) {
        ChunkRegenerator regenerator = createFor(owner);
        Bukkit.getServicesManager().register(ChunkRegenerator.class, regenerator, owner, ServicePriority.Normal);
        return regenerator;
    }
}
