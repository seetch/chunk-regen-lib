package me.seetch.chunkregenlib.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegenerationResultTest {

    @Test
    void successHasNoThrowableAndIsNotPartial() {
        RegenerationResult result = RegenerationResult.success(Set.of(RegenStep.BLOCKS), List.of());

        assertTrue(result.success());
        assertFalse(result.partial());
        assertTrue(result.throwable().isEmpty());
        assertEquals(Set.of(RegenStep.BLOCKS), result.stepsCompleted());
    }

    @Test
    void partialIsNotSuccessAndHasNoThrowable() {
        RegenerationResult result = RegenerationResult.partial(
                Set.of(RegenStep.BLOCKS, RegenStep.BIOMES),
                List.of("relight timed out")
        );

        assertFalse(result.success());
        assertTrue(result.partial());
        assertTrue(result.throwable().isEmpty());
        assertEquals(List.of("relight timed out"), result.warnings());
    }

    @Test
    void failureCarriesThrowableAndIsNotPartial() {
        RuntimeException cause = new RuntimeException("boom");
        RegenerationResult result = RegenerationResult.failure(Set.of(), List.of(), cause);

        assertFalse(result.success());
        assertFalse(result.partial());
        assertEquals(cause, result.throwable().orElseThrow());
    }

    @Test
    void stepsCompletedIsImmutable() {
        RegenerationResult result = RegenerationResult.success(Set.of(RegenStep.BLOCKS), List.of());

        assertTrue(result.stepsCompleted() instanceof Set<RegenStep>);
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> result.stepsCompleted().add(RegenStep.RELIGHT)
        );
    }
}
