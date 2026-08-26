package me.seetch.chunkregenlib.api;

/**
 * Отдельный шаг конвейера регенерации чанка. Набор завершённых шагов
 * возвращается в RegenerationResult#stepsCompleted() и позволяет
 * потребителю понять, что именно было сделано, даже если операция
 * завершилась не полностью успешно (см. RegenerationResult#partial()).
 */
public enum RegenStep {

    /** Секции блоков (рельеф) заменены на свежесгенерированные. */
    BLOCKS,

    /** Хранилище биомов заменено. */
    BIOMES,

    /** Heightmaps пересчитаны/заменены. */
    HEIGHTMAPS,

    /** Block entities в границах чанка пересозданы из свежих данных. */
    BLOCK_ENTITIES,

    /** Сущности обработаны согласно EntityRegenPolicy. */
    ENTITIES,

    /** Points of interest (POI) для чанка пересобраны. */
    POINTS_OF_INTEREST,

    /** Выполнен пересвет (relight) чанка и затронутых границ. */
    RELIGHT,

    /** Свежий чанк разослан игрокам-наблюдателям. */
    NOTIFY_VIEWERS,

    /** Чанк помечен как несохранённый / сохранение форсировано. */
    SAVE
}
