package me.seetch.chunkregenlib.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkLockRegistryTest {

    private final ChunkKey key = new ChunkKey(UUID.randomUUID(), 4, -2);

    @Test
    void secondTryLockFailsWhileFirstHeld() {
        ChunkLockRegistry registry = new ChunkLockRegistry();

        assertTrue(registry.tryLock(key));
        assertFalse(registry.tryLock(key));
        assertTrue(registry.isBusy(key));
    }

    @Test
    void unlockAllowsReacquiring() {
        ChunkLockRegistry registry = new ChunkLockRegistry();

        assertTrue(registry.tryLock(key));
        registry.unlock(key);

        assertFalse(registry.isBusy(key));
        assertTrue(registry.tryLock(key));
    }

    @Test
    void isBusyIsFalseForUnknownChunk() {
        ChunkLockRegistry registry = new ChunkLockRegistry();

        assertFalse(registry.isBusy(key));
    }

    @Test
    void onlyOneThreadWinsUnderContention() throws InterruptedException {
        ChunkLockRegistry registry = new ChunkLockRegistry();
        int threadCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger acquired = new AtomicInteger();

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (registry.tryLock(key)) {
                        acquired.incrementAndGet();
                    }
                });
            }
            ready.await();
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, acquired.get());
    }
}
