package me.seetch.chunkregenlib.api;

import java.util.Objects;

/**
 * Сводка того, какие подсистемы чанка затрагивает операция регенерации:
 * блоки, биомы, структуры/фичи, сущности, points of interest.
 *
 * В отличие от RegenerationOptions, который описывает полный набор
 * параметров запроса, RegenerationScope — это производное read-only
 * представление, удобное для логирования, метрик и адаптеров, которым нужно
 * быстро проверить "затрагивается ли подсистема X", не заглядывая во все поля опций.
 */
public record RegenerationScope(
        boolean blocks,
        boolean biomes,
        boolean structures,
        boolean entities,
        boolean pointsOfInterest
) {

    public static RegenerationScope from(RegenerationOptions options) {
        Objects.requireNonNull(options, "options");
        return new RegenerationScope(
                true,
                options.regenerateBiomes(),
                options.regenerateStructures(),
                options.entityPolicy() != EntityRegenPolicy.KEEP_ALL,
                options.rebuildPointsOfInterest()
        );
    }
}
