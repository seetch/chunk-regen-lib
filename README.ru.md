[English](README.md) | **Русский**

# chunk-regen-lib

Standalone-библиотека для Paper/Leaf/Purpur (и Folia-совместимых форков):
надёжная регенерация уже загруженного (активного) чанка на месте, без
устаревшего и ненадёжного `World#regenerateChunk`.

- Java 25 (разработка) / Java 21+ (таргет рантайма).
- Первая поддерживаемая версия сервера: **LeafMC 1.21.4**.
- Публичный API не течёт NMS/obfuscated-типами — только Bukkit/Paper API и DTO библиотеки.
- Генерация по-настоящему регенерирует чанк в одноразовом черновом мире (тот же
  приём, что у `//regen` в WorldEdit) и переносит результат в живой чанк — как
  это работает, см. исходники
  [`ScratchWorldManager`](adapter-1_21_4/src/main/java/me/seetch/chunkregenlib/adapter1214/ScratchWorldManager.java)
  и [`ChunkRegenerator1214`](adapter-1_21_4/src/main/java/me/seetch/chunkregenlib/adapter1214/ChunkRegenerator1214.java).
- Архитектура закладывает поддержку нескольких версий MC/Paper одновременно
  (каждая — отдельный модуль `adapter-<X_Y_Z>`) и региональной многопоточности (Folia).

## Структура проекта

```
chunk-regen-lib/
├── api/                 # публичный API, без NMS
├── core/                # диспетчеризация адаптеров по версии сервера, блокировки чанков
├── adapter-common/      # общие версионно-независимые утилиты для адаптеров
├── adapter-1_21_4/      # NMS-реализация под LeafMC/Paper 1.21.4
└── plugin-bootstrap/    # опциональный плагин-провайдер (регистрирует сервис)
```

## Откуда тянуть библиотеку (repo.seetch.ru)

Публикуется в `https://repo.seetch.ru/releases`.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.seetch.ru/releases")
}

dependencies {
    implementation("me.seetch:chunkregenlib-api:1.0.0")
    implementation("me.seetch:chunkregenlib-core:1.0.0")
    implementation("me.seetch:chunkregenlib-adapter-common:1.0.0")
    implementation("me.seetch:chunkregenlib-adapter-1_21_4:1.0.0")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>seetch-repo-releases</id>
        <url>https://repo.seetch.ru/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>me.seetch</groupId>
        <artifactId>chunkregenlib-api</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>me.seetch</groupId>
        <artifactId>chunkregenlib-core</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>me.seetch</groupId>
        <artifactId>chunkregenlib-adapter-common</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>me.seetch</groupId>
        <artifactId>chunkregenlib-adapter-1_21_4</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

`chunkregenlib-adapter-common` нужно указывать явно в обоих случаях:
`chunkregenlib-adapter-1_21_4` публикуется как один переотображённый jar без
метаданных о зависимостях в POM, поэтому транзитивно он не подтянется.

### Javadoc

`api`, `core` и `adapter-common` публикуют `-javadoc.jar` вместе с обычным jar'ом.
Reposilite рендерит его в браузере по адресу
`https://repo.seetch.ru/javadoc/releases/me/seetch/<артефакт>/<версия>/`,
например `https://repo.seetch.ru/javadoc/releases/me/seetch/chunkregenlib-api/1.0.0/`.

## Два способа подключения

### 1. Зашитая (shaded) зависимость — приватная копия внутри своего плагина

Добавить зависимости выше (Gradle или Maven), затем релоцировать пакет, чтобы
несколько плагинов с разными версиями библиотеки не конфликтовали в classloader'е:

```kotlin
// Gradle + shadow-плагин
tasks.shadowJar {
    relocate("me.seetch.chunkregenlib", "com.example.myplugin.shaded.chunkregenlib")
}
```

```xml
<!-- Maven + maven-shade-plugin -->
<configuration>
    <relocations>
        <relocation>
            <pattern>me.seetch.chunkregenlib</pattern>
            <shadedPattern>com.example.myplugin.shaded.chunkregenlib</shadedPattern>
        </relocation>
    </relocations>
    <!-- Без этого shade релоцирует классы, но оставляет
         META-INF/services/me.seetch.chunkregenlib.core.ChunkRegenAdapterProvider
         (и имя файла, и его содержимое) указывающим на старый пакет.
         ServiceLoader ищет файл по релоцированному имени интерфейса, ничего
         не находит, и regenerate() всегда падает с UnsupportedServerVersionException. -->
    <transformers>
        <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer" />
    </transformers>
</configuration>
```

shadow-плагин (Gradle) делает это автоматически, maven-shade-plugin — только если
явно подключить `ServicesResourceTransformer`.

```java
@Override
public void onEnable() {
    ChunkRegenerator regenerator = ChunkRegenLibService.createFor(this);
    // ...
}
```

### 2. Плагин-провайдер (несколько плагинов шарят одну версию библиотеки)

Установить на сервер собранный `plugin-bootstrap` (jar называется `chunk-regen-lib.jar`),
затем в своём плагине:

```java
@Override
public void onEnable() {
    ChunkRegenerator regenerator = Bukkit.getServicesManager()
            .load(ChunkRegenerator.class);
    if (regenerator == null) {
        getLogger().severe("ChunkRegenLib is not installed");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }
    // ...
}
```

В `plugin.yml` своего плагина указать мягкую/жёсткую зависимость:

```yaml
depend: [ChunkRegenLib]
```

## Использование API

```java
RegenerationOptions options = RegenerationOptions.builder()
        .entityPolicy(EntityRegenPolicy.KEEP_NAMED_OR_TAMED)
        .targetStatus(ChunkGenStage.FULL)
        .timeoutMillis(10_000L)
        // Опционально: ограничить замену диапазоном Y (по секциям) вместо замены
        // всей колонки чанка — например, чтобы не трогать постройки выше/ниже
        // ограниченной по высоте зоны ивента.
        .yRange(50, 140)
        .build();

regenerator.regenerate(chunk, options).thenAccept(result -> {
    if (result.success()) {
        // все шаги выполнены — result.stepsCompleted()
    } else if (result.partial()) {
        // прервано по таймауту, чанк остался консистентным — result.stepsCompleted()
    } else {
        // операция упала — result.throwable()
    }
    result.warnings().forEach(warning -> plugin.getLogger().warning(warning));
});
```

`ChunkRegenerator#isChunkBusy(world, chunkX, chunkZ)` — быстрая неблокирующая
проверка, идёт ли уже регенерация для чанка (без гарантий TOCTOU).

## Сборка

```bash
./gradlew build
```

- `:plugin-bootstrap:shadowJar` — единый jar плагина-провайдера со всеми адаптерами
  (`plugin-bootstrap/build/libs/chunk-regen-lib-<версия>.jar`).
- `:api:test`, `:core:test`, `:adapter-common:test` — unit-тесты без реального сервера,
  проходят полностью.

## Лицензия

[MIT](LICENSE)
