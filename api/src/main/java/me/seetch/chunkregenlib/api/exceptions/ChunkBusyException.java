package me.seetch.chunkregenlib.api.exceptions;

/**
 * Выбрасывается внутри адаптера, если не удалось захватить блокировку чанка —
 * над ним уже выполняется другая операция регенерации. Как и другие исключения
 * пайплайна, наружу из ChunkRegenerator#regenerate(...) не пробрасывается:
 * оседает в RegenerationResult#throwable().
 */
public final class ChunkBusyException extends ChunkRegenerationException {

    public ChunkBusyException(int chunkX, int chunkZ) {
        super("Chunk [" + chunkX + ", " + chunkZ + "] is already being regenerated");
    }
}
