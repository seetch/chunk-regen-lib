package me.seetch.chunkregenlib.bootstrap;

import me.seetch.chunkregenlib.api.ChunkRegenerator;
import me.seetch.chunkregenlib.api.exceptions.UnsupportedServerVersionException;
import me.seetch.chunkregenlib.core.ChunkRegenLibService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Тонкий плагин-провайдер: единственная задача — определить версию сервера,
 * создать подходящий адаптер и зарегистрировать ChunkRegenerator
 * в ServicesManager, чтобы несколько плагинов на сервере могли
 * получить одну и ту же загруженную версию библиотеки:
 *
 * ChunkRegenerator regenerator = Bukkit.getServicesManager().load(ChunkRegenerator.class);
 *
 * Плагины, встраивающие библиотеку приватно (shaded), этот модуль не используют —
 * вместо этого вызывают ChunkRegenLibService.createFor(this) напрямую
 * в своём onEnable().
 */
public final class ChunkRegenLibPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            ChunkRegenLibService.createAndRegister(this);
            getLogger().info("ChunkRegenerator registered via ServicesManager.");
        } catch (UnsupportedServerVersionException exception) {
            getLogger().severe(exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}
