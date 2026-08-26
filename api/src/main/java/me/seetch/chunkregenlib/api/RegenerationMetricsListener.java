package me.seetch.chunkregenlib.api;

/**
 * Опциональный слушатель метрик регенерации. Потребитель библиотеки может
 * подписать свою реализацию, чтобы считать количество успешных/неуспешных
 * операций и среднее время выполнения — без обязательной интеграции
 * с конкретной metrics-системой внутри самой библиотеки.
 */
public interface RegenerationMetricsListener {

    /**
     * Вызывается после завершения операции регенерации (успешной, частичной или неуспешной).
     *
     * @param result        итог операции
     * @param durationMillis время выполнения операции в миллисекундах
     */
    void onRegenerationCompleted(RegenerationResult result, long durationMillis);
}
