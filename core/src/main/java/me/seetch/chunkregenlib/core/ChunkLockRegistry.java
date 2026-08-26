package me.seetch.chunkregenlib.core;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр блокировок "один чанк — одна одновременная операция регенерации".
 *
 * Намеренно не использует ReentrantLock: одна логическая операция регенерации
 * захватывает блокировку на вызывающем потоке (regenerate()), а освобождает —
 * в CompletableFuture#whenComplete на потоке, которым завершилась цепочка
 * (owning-thread адаптера, а не исходный вызывающий поток). У ReentrantLock
 * освобождение с чужого потока — no-op (см. isHeldByCurrentThread()), из-за чего
 * блокировка осталась бы захваченной навсегда. Маркер занятости в
 * ConcurrentHashMap не имеет привязки к потоку и не создаёт эту проблему.
 */
public final class ChunkLockRegistry {

    private final ConcurrentHashMap<ChunkKey, Boolean> busy = new ConcurrentHashMap<>();

    public boolean isBusy(ChunkKey key) {
        return busy.containsKey(key);
    }

    /** Пытается захватить блокировку для чанка, не дожидаясь освобождения. */
    public boolean tryLock(ChunkKey key) {
        return busy.putIfAbsent(key, Boolean.TRUE) == null;
    }

    /** Освобождает блокировку для чанка. */
    public void unlock(ChunkKey key) {
        busy.remove(key);
    }
}
