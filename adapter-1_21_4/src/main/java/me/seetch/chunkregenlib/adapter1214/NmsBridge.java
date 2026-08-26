package me.seetch.chunkregenlib.adapter1214;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;

/**
 * Единственная точка перехода между публичными Bukkit-типами и внутренними
 * NMS-типами (Mojang mappings) для этого адаптера. Держим все приведения
 * в одном месте, чтобы при обновлении маппингов/версии править только этот файл.
 *
 * Сверено компиляцией и построчным чтением декомпилированного dev-bundle
 * 1.21.4-R0.1-SNAPSHOT (Mojang mappings): CraftBukkit с Paper 1.20.5+ действительно
 * безверсионный (org.bukkit.craftbukkit.*); CraftChunk#getHandle(ChunkStatus)
 * при уже загруженном чанке идёт быстрым путём через
 * ServerLevel#getChunkIfLoaded(x, z) и всегда возвращает именно LevelChunk
 * (объявленный тип возврата — ChunkAccess, отсюда явный каст ниже).
 */
final class NmsBridge {

    private NmsBridge() {
    }

    static ServerLevel toServerLevel(World world) {
        return ((CraftWorld) world).getHandle();
    }

    static LevelChunk toLevelChunk(Chunk chunk) {
        return (LevelChunk) ((CraftChunk) chunk).getHandle(ChunkStatus.FULL);
    }
}
