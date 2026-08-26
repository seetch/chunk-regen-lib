package me.seetch.chunkregenlib.core.support;

import me.seetch.chunkregenlib.api.ChunkRegenerator;
import me.seetch.chunkregenlib.api.RegenerationOptions;
import me.seetch.chunkregenlib.api.RegenerationResult;
import me.seetch.chunkregenlib.core.ChunkRegenAdapterProvider;
import me.seetch.chunkregenlib.core.MinecraftVersion;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Тестовый адаптер, регистрируемый через ServiceLoader (см. test-resources
 * META-INF/services), имитирующий адаптер под 1.21.4 без реального NMS.
 */
public final class StubAdapterProvider implements ChunkRegenAdapterProvider {

    @Override
    public MinecraftVersion supportedVersion() {
        return new MinecraftVersion(1, 21, 4);
    }

    @Override
    public ChunkRegenerator create(Plugin owner) {
        return new StubChunkRegenerator();
    }

    private static final class StubChunkRegenerator implements ChunkRegenerator {

        @Override
        public CompletableFuture<RegenerationResult> regenerate(World world, int chunkX, int chunkZ, RegenerationOptions options) {
            return CompletableFuture.completedFuture(RegenerationResult.success(Set.of(), java.util.List.of()));
        }

        @Override
        public CompletableFuture<RegenerationResult> regenerate(Chunk chunk, RegenerationOptions options) {
            return CompletableFuture.completedFuture(RegenerationResult.success(Set.of(), java.util.List.of()));
        }

        @Override
        public boolean isChunkBusy(World world, int chunkX, int chunkZ) {
            return false;
        }
    }
}
