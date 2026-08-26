**English** | [Русский](README.ru.md)

# chunk-regen-lib

Standalone library for Paper/Leaf/Purpur (and Folia-compatible forks): reliable
regeneration of an already-loaded (active) chunk in place, without the deprecated
and unreliable `World#regenerateChunk`.

- Java 25 (development) / Java 21+ (runtime target).
- First supported server version: **LeafMC 1.21.4**.
- The public API never leaks NMS/obfuscated types — only Bukkit/Paper API types
  and the library's own DTOs.
- Generation regenerates the chunk for real in a throwaway scratch world (the
  same technique WorldEdit's `//regen` uses) and copies the result into the
  live chunk — see the [`ScratchWorldManager`](adapter-1_21_4/src/main/java/me/seetch/chunkregenlib/adapter1214/ScratchWorldManager.java)
  and [`ChunkRegenerator1214`](adapter-1_21_4/src/main/java/me/seetch/chunkregenlib/adapter1214/ChunkRegenerator1214.java)
  sources for how it works.
- The architecture is designed for supporting several MC/Paper versions at once
  (each as its own `adapter-<X_Y_Z>` module) and regionized multithreading (Folia).

## Project layout

```
chunk-regen-lib/
├── api/                 # public API, no NMS
├── core/                # adapter dispatch by server version, chunk locks
├── adapter-common/      # shared version-independent utilities for adapters
├── adapter-1_21_4/      # NMS implementation for LeafMC/Paper 1.21.4
└── plugin-bootstrap/    # optional plugin provider (registers the service)
```

## Pulling the library from repo.seetch.ru

Published to `https://repo.seetch.ru/releases`.

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

`chunkregenlib-adapter-common` must be listed explicitly in both cases:
`chunkregenlib-adapter-1_21_4` is published as a single reobfuscated jar with no
POM dependency metadata, so it won't be pulled in transitively.

### Javadoc

`api`, `core` and `adapter-common` publish a `-javadoc.jar` alongside the regular
jar. Reposilite renders it in the browser at
`https://repo.seetch.ru/javadoc/releases/me/seetch/<artifact>/<version>/`, e.g.
`https://repo.seetch.ru/javadoc/releases/me/seetch/chunkregenlib-api/1.0.0/`.

## Two ways to depend on it

### 1. Shaded dependency — a private copy embedded in your own plugin

Add the dependencies above (Gradle or Maven), then relocate the package so
several plugins bundling different versions of the library don't clash in the
classloader:

```kotlin
// Gradle + shadow plugin
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
</configuration>
```

```java
@Override
public void onEnable() {
    ChunkRegenerator regenerator = ChunkRegenLibService.createFor(this);
    // ...
}
```

### 2. Plugin provider (several plugins share one loaded version of the library)

Install the built `plugin-bootstrap` on the server (the jar is called
`chunk-regen-lib.jar`), then in your own plugin:

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

Declare a soft/hard dependency in your plugin's `plugin.yml`:

```yaml
depend: [ChunkRegenLib]
```

## Using the API

```java
RegenerationOptions options = RegenerationOptions.builder()
        .entityPolicy(EntityRegenPolicy.KEEP_NAMED_OR_TAMED)
        .targetStatus(ChunkGenStage.FULL)
        .timeoutMillis(10_000L)
        // Optional: restrict the swap to a Y range (section-granularity) instead of
        // replacing the whole chunk column — e.g. to avoid touching builds above/below
        // a bounded event area.
        .yRange(50, 140)
        .build();

regenerator.regenerate(chunk, options).thenAccept(result -> {
    if (result.success()) {
        // all requested steps completed — result.stepsCompleted()
    } else if (result.partial()) {
        // aborted by timeout, the chunk stayed consistent — result.stepsCompleted()
    } else {
        // the operation failed — result.throwable()
    }
    result.warnings().forEach(warning -> plugin.getLogger().warning(warning));
});
```

`ChunkRegenerator#isChunkBusy(world, chunkX, chunkZ)` — a fast, non-blocking check
for whether a chunk is currently being regenerated (no TOCTOU guarantees).

## Building

```bash
./gradlew build
```

- `:plugin-bootstrap:shadowJar` — a single jar of the plugin provider bundling all
  adapters (`plugin-bootstrap/build/libs/chunk-regen-lib-<version>.jar`).
- `:api:test`, `:core:test`, `:adapter-common:test` — unit tests that don't need a
  real server, and pass in full.

## License

[MIT](LICENSE)
