package me.seetch.chunkregenlib.api.exceptions;

/**
 * Выбрасывается, если для текущей версии сервера не зарегистрирован ни один адаптер.
 * Библиотека никогда не пытается выполнить операцию регенерации "на удачу"
 * на неподдерживаемой версии — вместо этого явно отказывает при инициализации.
 */
public final class UnsupportedServerVersionException extends RuntimeException {

    public UnsupportedServerVersionException(String detectedVersion) {
        super("No ChunkRegenLib adapter registered for server version: " + detectedVersion);
    }
}
