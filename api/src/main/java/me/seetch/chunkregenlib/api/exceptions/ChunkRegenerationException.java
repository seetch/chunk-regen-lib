package me.seetch.chunkregenlib.api.exceptions;

/**
 * Общая обёртка над техническими ошибками, произошедшими внутри адаптера
 * во время регенерации (например, неожиданная NMS-ошибка). Как правило,
 * потребитель не увидит это исключение напрямую — оно оседает
 * в RegenerationResult#throwable(), а не пробрасывается наружу
 * из ChunkRegenerator#regenerate(...).
 */
public class ChunkRegenerationException extends RuntimeException {

    public ChunkRegenerationException(String message) {
        super(message);
    }

    public ChunkRegenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
