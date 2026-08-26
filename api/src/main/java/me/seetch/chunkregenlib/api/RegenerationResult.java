package me.seetch.chunkregenlib.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Итог одной операции регенерации чанка.
 *
 * Три возможных исхода: success() — все запрошенные шаги выполнены полностью;
 * partial() — операция прервана (например, по таймауту), но чанк остался
 * в консистентном состоянии, часть шагов не выполнена; ни то ни другое —
 * операция завершилась исключением, см. throwable().
 */
public final class RegenerationResult {

    private final boolean success;
    private final boolean partial;
    private final Set<RegenStep> stepsCompleted;
    private final List<String> warnings;
    private final Throwable throwable;

    private RegenerationResult(
            boolean success,
            boolean partial,
            Set<RegenStep> stepsCompleted,
            List<String> warnings,
            Throwable throwable
    ) {
        this.success = success;
        this.partial = partial;
        this.stepsCompleted = Set.copyOf(stepsCompleted);
        this.warnings = List.copyOf(warnings);
        this.throwable = throwable;
    }

    public static RegenerationResult success(Set<RegenStep> stepsCompleted, List<String> warnings) {
        Objects.requireNonNull(stepsCompleted, "stepsCompleted");
        Objects.requireNonNull(warnings, "warnings");
        return new RegenerationResult(true, false, stepsCompleted, warnings, null);
    }

    /** Операция прервана (например, по RegenerationOptions#timeoutMillis()), чанк остался консистентным. */
    public static RegenerationResult partial(Set<RegenStep> stepsCompleted, List<String> warnings) {
        Objects.requireNonNull(stepsCompleted, "stepsCompleted");
        Objects.requireNonNull(warnings, "warnings");
        return new RegenerationResult(false, true, stepsCompleted, warnings, null);
    }

    public static RegenerationResult failure(Set<RegenStep> stepsCompleted, List<String> warnings, Throwable throwable) {
        Objects.requireNonNull(stepsCompleted, "stepsCompleted");
        Objects.requireNonNull(warnings, "warnings");
        Objects.requireNonNull(throwable, "throwable");
        return new RegenerationResult(false, false, stepsCompleted, warnings, throwable);
    }

    public boolean success() {
        return success;
    }

    /** true, если операция была прервана до завершения, но не упала с исключением. */
    public boolean partial() {
        return partial;
    }

    public Set<RegenStep> stepsCompleted() {
        return stepsCompleted;
    }

    public List<String> warnings() {
        return warnings;
    }

    public Optional<Throwable> throwable() {
        return Optional.ofNullable(throwable);
    }
}
