package me.seetch.chunkregenlib.adapter1214;

import me.seetch.chunkregenlib.api.ChunkRegenerator;
import me.seetch.chunkregenlib.core.ChunkRegenAdapterProvider;
import me.seetch.chunkregenlib.core.MinecraftVersion;
import org.bukkit.plugin.Plugin;

/**
 * SPI-провайдер адаптера под LeafMC/Paper 1.21.4. Регистрируется через
 * META-INF/services/me.seetch.chunkregenlib.core.ChunkRegenAdapterProvider,
 * чтобы ChunkRegenLibService нашёл его через java.util.ServiceLoader.
 */
public final class Adapter1214Provider implements ChunkRegenAdapterProvider {

    @Override
    public MinecraftVersion supportedVersion() {
        return new MinecraftVersion(1, 21, 4);
    }

    @Override
    public ChunkRegenerator create(Plugin owner) {
        return new ChunkRegenerator1214(owner);
    }
}
