package me.seetch.chunkregenlib.api;

import java.util.Objects;

/**
 * Неизменяемый набор параметров одной операции регенерации чанка.
 * Создаётся через builder(), все поля имеют разумные дефолты.
 */
public final class RegenerationOptions {

    private final boolean regenerateBiomes;
    private final boolean regenerateStructures;
    private final ChunkGenStage targetStatus;
    private final boolean preservePlayers;
    private final EntityRegenPolicy entityPolicy;
    private final boolean rebuildPointsOfInterest;
    private final boolean relight;
    private final boolean notifyViewers;
    private final boolean saveAfter;
    private final long timeoutMillis;
    private final int minY;
    private final int maxY;

    private RegenerationOptions(Builder builder) {
        this.regenerateBiomes = builder.regenerateBiomes;
        this.regenerateStructures = builder.regenerateStructures;
        this.targetStatus = builder.targetStatus;
        this.preservePlayers = builder.preservePlayers;
        this.entityPolicy = builder.entityPolicy;
        this.rebuildPointsOfInterest = builder.rebuildPointsOfInterest;
        this.relight = builder.relight;
        this.notifyViewers = builder.notifyViewers;
        this.saveAfter = builder.saveAfter;
        this.timeoutMillis = builder.timeoutMillis;
        this.minY = builder.minY;
        this.maxY = builder.maxY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Все параметры по умолчанию — эквивалентно builder().build(). */
    public static RegenerationOptions defaults() {
        return builder().build();
    }

    public boolean regenerateBiomes() {
        return regenerateBiomes;
    }

    public boolean regenerateStructures() {
        return regenerateStructures;
    }

    public ChunkGenStage targetStatus() {
        return targetStatus;
    }

    public boolean preservePlayers() {
        return preservePlayers;
    }

    public EntityRegenPolicy entityPolicy() {
        return entityPolicy;
    }

    public boolean rebuildPointsOfInterest() {
        return rebuildPointsOfInterest;
    }

    public boolean relight() {
        return relight;
    }

    public boolean notifyViewers() {
        return notifyViewers;
    }

    public boolean saveAfter() {
        return saveAfter;
    }

    /** Предохранитель от зависания (генерация/пересвет), в миллисекундах. */
    public long timeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Нижняя граница (включительно) диапазона высот, который затрагивает регенерация,
     * в мировых координатах блока Y. По умолчанию — Integer.MIN_VALUE, что означает
     * "от низа мира" (адаптер сам ограничивает значение реальной минимальной высотой
     * конкретного мира). Регенерация всегда идёт по секциям (16 блоков) — секция,
     * пересекающая границу диапазона, будет затронута целиком.
     */
    public int minY() {
        return minY;
    }

    /** Верхняя граница (включительно) диапазона высот. По умолчанию — Integer.MAX_VALUE ("до верха мира"). */
    public int maxY() {
        return maxY;
    }

    public static final class Builder {

        private boolean regenerateBiomes = true;
        private boolean regenerateStructures = true;
        private ChunkGenStage targetStatus = ChunkGenStage.FULL;
        private boolean preservePlayers = true;
        private EntityRegenPolicy entityPolicy = EntityRegenPolicy.REMOVE_ALL_NON_PLAYER;
        private boolean rebuildPointsOfInterest = true;
        private boolean relight = true;
        private boolean notifyViewers = true;
        private boolean saveAfter = true;
        private long timeoutMillis = 5_000L;
        private int minY = Integer.MIN_VALUE;
        private int maxY = Integer.MAX_VALUE;

        private Builder() {
        }

        public Builder regenerateBiomes(boolean regenerateBiomes) {
            this.regenerateBiomes = regenerateBiomes;
            return this;
        }

        public Builder regenerateStructures(boolean regenerateStructures) {
            this.regenerateStructures = regenerateStructures;
            return this;
        }

        public Builder targetStatus(ChunkGenStage targetStatus) {
            this.targetStatus = Objects.requireNonNull(targetStatus, "targetStatus");
            return this;
        }

        public Builder preservePlayers(boolean preservePlayers) {
            this.preservePlayers = preservePlayers;
            return this;
        }

        public Builder entityPolicy(EntityRegenPolicy entityPolicy) {
            this.entityPolicy = Objects.requireNonNull(entityPolicy, "entityPolicy");
            return this;
        }

        public Builder rebuildPointsOfInterest(boolean rebuildPointsOfInterest) {
            this.rebuildPointsOfInterest = rebuildPointsOfInterest;
            return this;
        }

        public Builder relight(boolean relight) {
            this.relight = relight;
            return this;
        }

        public Builder notifyViewers(boolean notifyViewers) {
            this.notifyViewers = notifyViewers;
            return this;
        }

        public Builder saveAfter(boolean saveAfter) {
            this.saveAfter = saveAfter;
            return this;
        }

        public Builder timeoutMillis(long timeoutMillis) {
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("timeoutMillis must be positive: " + timeoutMillis);
            }
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        /** Ограничивает регенерацию диапазоном высот [minY, maxY] (включительно), по секциям. */
        public Builder yRange(int minY, int maxY) {
            if (minY > maxY) {
                throw new IllegalArgumentException("minY (" + minY + ") must not exceed maxY (" + maxY + ")");
            }
            this.minY = minY;
            this.maxY = maxY;
            return this;
        }

        public RegenerationOptions build() {
            return new RegenerationOptions(this);
        }
    }
}
