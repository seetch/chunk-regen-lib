package me.seetch.chunkregenlib.adapter1214;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import me.seetch.chunkregenlib.adaptercommon.EntityRegenPolicyApplier;
import me.seetch.chunkregenlib.adaptercommon.NeighborChunkSupport;
import me.seetch.chunkregenlib.adaptercommon.SchedulingSupport;
import me.seetch.chunkregenlib.api.ChunkGenStage;
import me.seetch.chunkregenlib.api.ChunkRegenerator;
import me.seetch.chunkregenlib.api.RegenStep;
import me.seetch.chunkregenlib.api.RegenerationOptions;
import me.seetch.chunkregenlib.api.RegenerationResult;
import me.seetch.chunkregenlib.api.exceptions.ChunkBusyException;
import me.seetch.chunkregenlib.api.exceptions.ChunkRegenerationException;
import me.seetch.chunkregenlib.core.ChunkKey;
import me.seetch.chunkregenlib.core.ChunkLockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Адаптер регенерации активного чанка под LeafMC/Paper 1.21.4.
 *
 * Весь класс проверен компиляцией и сверен построчно с декомпилированными
 * исходниками сервера (dev-bundle 1.21.4-R0.1-SNAPSHOT, Mojang mappings).
 *
 * Генерация свежих данных (см. ScratchWorldManager) не воспроизводит ванильный
 * пайплайн ChunkGenerator/WorldGenRegion вручную — это оказалось непрактично:
 * WorldGenRegion требует StaticCache2D&lt;GenerationChunkHolder&gt; с состоянием
 * соседей, а GenerationChunkHolder на Paper/Leaf почти полностью выпотрошен
 * ("Paper - rewrite chunk system") в пользу проприетарной чанк-системы Moonrise
 * (ca.spottedleaf.moonrise.patches.chunk_system.*) — его дата-методы кастуют себя
 * в непубличный, версиоспецифичный ChunkSystemChunkHolder. Вместо этого,
 * как и штатный //regen в WorldEdit, используется черновой (scratch) Bukkit-мир
 * с тем же generator/seed: чанк-система сама решает генерировать чанк или
 * загружать его с диска на основании того, есть ли для него персистентные
 * данные — а у чернового мира их никогда нет, поэтому запрос чанка там всегда
 * идёт по-настоящему через полный ChunkGenerator-пайплайн (со структурами),
 * штатным, стабильным путём (World#getChunkAtAsync), без обращения к приватным
 * классам чанк-системы напрямую. См. ADAPTERS.md, раздел 3, для истории решения.
 */
final class ChunkRegenerator1214 implements ChunkRegenerator {

    private final Plugin owner;
    private final ChunkLockRegistry lockRegistry = new ChunkLockRegistry();

    ChunkRegenerator1214(Plugin owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public CompletableFuture<RegenerationResult> regenerate(World world, int chunkX, int chunkZ, RegenerationOptions options) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(options, "options");

        ChunkKey key = new ChunkKey(world.getUID(), chunkX, chunkZ);
        if (!lockRegistry.tryLock(key)) {
            return CompletableFuture.completedFuture(
                    RegenerationResult.failure(Set.of(), List.of(), new ChunkBusyException(chunkX, chunkZ)));
        }

        // Генерация в черновом мире занимает реальное время, и без явного тикета
        // ничто не мешает чанк-системе выгрузить целевой чанк за это время, если
        // рядом нет игроков (частый случай для регенерации по расписанию/на старте
        // сервера). Тикет форсирует загрузку чанка и удерживает его загруженным
        // до конца операции, независимо от того, стоит ли там кто то.
        CompletableFuture<RegenerationResult> future = SchedulingSupport
                .callOnOwningThread(owner, world, chunkX, chunkZ, () -> world.addPluginChunkTicket(chunkX, chunkZ, owner))
                .thenCompose(ignored -> generateFreshDataAsync(world, chunkX, chunkZ, options))
                .orTimeout(options.timeoutMillis(), TimeUnit.MILLISECONDS)
                .thenCompose(freshData -> SchedulingSupport
                        .callOnOwningThread(owner, world, chunkX, chunkZ,
                                () -> beginApplyToLiveChunk(world, chunkX, chunkZ, freshData, options))
                        .thenCompose(stage -> stage))
                .exceptionally(this::toFailureResult);

        future.whenComplete((result, throwable) -> {
            lockRegistry.unlock(key);
            SchedulingSupport.callOnOwningThread(owner, world, chunkX, chunkZ,
                    () -> world.removePluginChunkTicket(chunkX, chunkZ, owner));
        });
        return future;
    }

    @Override
    public CompletableFuture<RegenerationResult> regenerate(Chunk chunk, RegenerationOptions options) {
        Objects.requireNonNull(chunk, "chunk");
        return regenerate(chunk.getWorld(), chunk.getX(), chunk.getZ(), options);
    }

    @Override
    public boolean isChunkBusy(World world, int chunkX, int chunkZ) {
        Objects.requireNonNull(world, "world");
        return lockRegistry.isBusy(new ChunkKey(world.getUID(), chunkX, chunkZ));
    }

    private RegenerationResult toFailureResult(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException completionException && completionException.getCause() != null
                ? completionException.getCause()
                : throwable;

        if (cause instanceof TimeoutException) {
            // Таймаут сработал ДО применения данных к живому чанку — он остался нетронутым,
            // поэтому это partial, а не failure (см. RegenerationResult#partial()).
            return RegenerationResult.partial(
                    Set.of(),
                    List.of("Generation exceeded timeoutMillis and was aborted before touching the live chunk"));
        }
        return RegenerationResult.failure(Set.of(), List.of(), cause);
    }

    /**
     * Генерация свежих данных: не трогает живой чанк/сущности. Генерирует чанк с нуля в черновом мире (см. ScratchWorldManager) и захватывает
     * результат в переносимый снимок (FreshChunkData), не трогая живой мир.
     *
     * targetStatus/regenerateStructures в этой версии не влияют на глубину генерации —
     * черновой чанк всегда генерируется штатным путём до ChunkStatus.FULL (со структурами),
     * это дешевле и безопаснее, чем частично воспроизводить пайплайн вручную. Расхождение
     * с запрошенными опциями отмечается предупреждением в RegenerationResult.
     */
    private CompletableFuture<FreshChunkData> generateFreshDataAsync(World world, int chunkX, int chunkZ, RegenerationOptions options) {
        return ScratchWorldManager.getOrCreate(owner, world)
                .thenCompose(scratchWorld -> scratchWorld.getChunkAtAsync(chunkX, chunkZ, true, false)
                        .thenCompose(scratchChunk -> SchedulingSupport.callOnOwningThread(
                                owner, scratchWorld, chunkX, chunkZ,
                                () -> captureFreshData(scratchChunk))));
    }

    /** Снимает секции/block entities/heightmaps чернового чанка на его владеющем потоке. */
    private FreshChunkData captureFreshData(Chunk scratchChunk) {
        ServerLevel scratchLevel = NmsBridge.toServerLevel(scratchChunk.getWorld());
        LevelChunk scratchLevelChunk = NmsBridge.toLevelChunk(scratchChunk);
        SerializableChunkData snapshot = SerializableChunkData.copyOf(scratchLevel, scratchLevelChunk);
        return new FreshChunkData(withoutNullPostProcessing(snapshot));
    }

    /**
     * SerializableChunkData#copyOf оставляет в postProcessingSections null для секций
     * без отложенных обновлений (обычный случай для большинства секций). Это нормально
     * для пути "запись в NBT на диск и обратно" (там пустые списки просто не пишутся),
     * но SerializableChunkData#read не проверяет элементы массива на null перед тем,
     * как передать их в addPackedPostProcess, и падает с NullPointerException внутри
     * ShortList#addAll. Живой снимок через copyOf этот путь не проходит, поэтому
     * баг проявляется только здесь, а не при обычной загрузке чанка с диска.
     */
    private static SerializableChunkData withoutNullPostProcessing(SerializableChunkData snapshot) {
        ShortList[] original = snapshot.postProcessingSections();
        ShortList[] patched = new ShortList[original.length];
        for (int i = 0; i < original.length; i++) {
            patched[i] = original[i] != null ? original[i] : new ShortArrayList();
        }
        return new SerializableChunkData(
                snapshot.biomeRegistry(),
                snapshot.chunkPos(),
                snapshot.minSectionY(),
                snapshot.lastUpdateTime(),
                snapshot.inhabitedTime(),
                snapshot.chunkStatus(),
                snapshot.blendingData(),
                snapshot.belowZeroRetrogen(),
                snapshot.upgradeData(),
                snapshot.carvingMask(),
                snapshot.heightmaps(),
                snapshot.packedTicks(),
                patched,
                snapshot.lightCorrect(),
                snapshot.sectionData(),
                snapshot.entities(),
                snapshot.blockEntities(),
                snapshot.structureData(),
                snapshot.persistentDataContainer()
        );
    }

    /** Применение к живому чанку — выполняется на владеющем потоке. */
    private CompletableFuture<RegenerationResult> beginApplyToLiveChunk(
            World world, int chunkX, int chunkZ, FreshChunkData freshData, RegenerationOptions options
    ) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return CompletableFuture.completedFuture(RegenerationResult.failure(
                    Set.of(),
                    List.of(),
                    new ChunkRegenerationException(
                            "Chunk [" + chunkX + ", " + chunkZ + "] was unloaded before regeneration could be applied")));
        }

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        EnumSet<RegenStep> completed = EnumSet.noneOf(RegenStep.class);
        List<String> warnings = new ArrayList<>();

        try {
            ServerLevel level = NmsBridge.toServerLevel(world);
            LevelChunk levelChunk = NmsBridge.toLevelChunk(chunk);

            // Замена секций блоков/биом-хранилища/heightmaps/block entities.
            applyFreshDataToLiveChunk(level, levelChunk, freshData, options);
            completed.add(RegenStep.BLOCKS);
            completed.add(RegenStep.HEIGHTMAPS);
            completed.add(RegenStep.BLOCK_ENTITIES);
            if (options.regenerateBiomes()) {
                completed.add(RegenStep.BIOMES);
            } else {
                // Известное ограничение: биомы хранятся внутри LevelChunkSection вместе
                // с блоками (PalettedContainer<Holder<Biome>> — часть секции, не отдельный
                // объект чанка), поэтому текущая реализация переносит секцию целиком и не
                // умеет выборочно сохранить старые биомы при замене блоков.
                warnings.add("regenerateBiomes=false is not honored yet: biomes are replaced together with blocks (see ADAPTERS.md)");
            }
            if (!options.regenerateStructures() || options.targetStatus() != ChunkGenStage.FULL) {
                // Черновой мир всегда генерирует до ChunkStatus.FULL (см. generateFreshDataAsync).
                warnings.add("regenerateStructures/targetStatus are not honored yet: scratch generation always runs to FULL (see ADAPTERS.md)");
            }

            // Сущности — публичный Bukkit API, версионно-независимая логика,
            // общая для всех адаптеров (см. adapter-common). Игроков не трогает никогда.
            EntityRegenPolicyApplier.apply(chunk, options.entityPolicy());
            completed.add(RegenStep.ENTITIES);

            if (options.rebuildPointsOfInterest()) {
                rebuildPointsOfInterest(level, levelChunk);
                completed.add(RegenStep.POINTS_OF_INTEREST);
            }

            if (options.relight()) {
                // Не форсируем загрузку соседей — только фиксируем предупреждение,
                // если пересвет по границе может остаться неполным.
                warnings.addAll(NeighborChunkSupport.unloadedNeighborWarnings(world, chunkX, chunkZ));
                completed.add(RegenStep.RELIGHT);

                EnumSet<RegenStep> completedAfterRelight = completed;
                List<String> warningsAfterRelight = warnings;
                return relightChunkAsync(level, chunkX, chunkZ)
                        .thenCompose(relitCount -> SchedulingSupport.callOnOwningThread(
                                owner, world, chunkX, chunkZ,
                                () -> finishRegeneration(world, chunkX, chunkZ, options, completedAfterRelight, warningsAfterRelight)));
            }

            return CompletableFuture.completedFuture(finishRegeneration(world, chunkX, chunkZ, options, completed, warnings));
        } catch (Throwable throwable) {
            return CompletableFuture.completedFuture(RegenerationResult.failure(completed, warnings, throwable));
        }
    }

    private RegenerationResult finishRegeneration(
            World world, int chunkX, int chunkZ, RegenerationOptions options, EnumSet<RegenStep> completed, List<String> warnings
    ) {
        try {
            if (options.notifyViewers()) {
                // Штатный чанк-трекер сервера (CraftWorld#refreshChunk -> ChunkHolder#getChunkToSend
                // + FeatureHooks#sendChunkRefreshPackets) — совместим с async/streamed chunk sending Leaf,
                // так как не собирает пакеты вручную. Проверено декомпиляцией CraftWorld 1.21.4.
                world.refreshChunk(chunkX, chunkZ);
                completed.add(RegenStep.NOTIFY_VIEWERS);
            }

            if (options.saveAfter() && world.isChunkLoaded(chunkX, chunkZ)) {
                markChunkUnsaved(NmsBridge.toLevelChunk(world.getChunkAt(chunkX, chunkZ)));
                completed.add(RegenStep.SAVE);
            }

            return RegenerationResult.success(completed, warnings);
        } catch (Throwable throwable) {
            return RegenerationResult.failure(completed, warnings, throwable);
        }
    }

    /**
     * Переносит данные из снимка чернового чанка (FreshChunkData) в живой LevelChunk:
     * секции блоков (несут в себе и биомы), heightmaps, block entities.
     *
     * SerializableChunkData#read(...) — тот же метод, которым чанк-система читает
     * персистентные данные с диска в живой мир; для статуса FULL он строит новый
     * LevelChunk, обёрнутый в ImposterProtoChunk (см. декомпилированный
     * SerializableChunkData.read). Мы не подменяем объект живого чанка целиком
     * (это потребовало бы переподключения к ChunkHolder/ChunkMap) — вместо этого
     * копируем содержимое полученного LevelChunk в уже существующий, чтобы не
     * трогать внутреннее состояние чанк-системы.
     */
    private void applyFreshDataToLiveChunk(ServerLevel level, LevelChunk levelChunk, FreshChunkData freshData, RegenerationOptions options) {
        RegionStorageInfo storageInfo = new RegionStorageInfo(level.getWorld().getName(), level.dimension(), "chunk");
        ChunkPos pos = levelChunk.getPos();

        ProtoChunk reconstructed = freshData.snapshot().read(level, level.getPoiManager(), storageInfo, pos);
        LevelChunk freshChunk = ((ImposterProtoChunk) reconstructed).getWrapped();

        // RegenerationOptions#minY()/maxY() ограничивают замену диапазоном высот, по секциям
        // (16 блоков) — секция, пересекающая границу диапазона, заменяется целиком. Значения
        // по умолчанию (MIN_VALUE/MAX_VALUE) после клампа дают весь доступный диапазон секций
        // мира, т.е. поведение "заменить всю колонку" сохраняется, если minY/maxY не заданы.
        int minSectionY = Math.max(Math.floorDiv(options.minY(), 16), level.getMinSectionY());
        int maxSectionY = Math.min(Math.floorDiv(options.maxY(), 16), level.getMaxSectionY());

        LevelChunkSection[] liveSections = levelChunk.getSections();
        LevelChunkSection[] freshSections = freshChunk.getSections();
        for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
            int index = levelChunk.getSectionIndexFromSectionY(sectionY);
            if (index >= 0 && index < liveSections.length && index < freshSections.length) {
                liveSections[index] = freshSections[index];
            }
        }

        int minBlockY = minSectionY << 4;
        int maxBlockY = (maxSectionY << 4) + 15;
        List<BlockPos> blockEntitiesToRemove = new ArrayList<>();
        for (BlockPos blockPos : levelChunk.getBlockEntities().keySet()) {
            if (blockPos.getY() >= minBlockY && blockPos.getY() <= maxBlockY) {
                blockEntitiesToRemove.add(blockPos);
            }
        }
        blockEntitiesToRemove.forEach(levelChunk::removeBlockEntity);
        for (BlockEntity blockEntity : freshChunk.getBlockEntities().values()) {
            BlockPos blockPos = blockEntity.getBlockPos();
            if (blockPos.getY() >= minBlockY && blockPos.getY() <= maxBlockY) {
                levelChunk.setBlockEntity(blockEntity);
            }
        }

        Heightmap.primeHeightmaps(levelChunk, EnumSet.allOf(Heightmap.Types.class));
    }

    /**
     * Пересборка points of interest для всех секций чанка. Проверено декомпиляцией:
     * PoiManager#checkConsistencyWithBlocks(SectionPos, LevelChunkSection) пересканирует
     * блоки секции и обновляет POI-индекс — операция не требует WorldGenRegion/контекста
     * генерации, поэтому применима сразу после того, как в чанке актуализированы блоки.
     */
    private void rebuildPointsOfInterest(ServerLevel level, LevelChunk levelChunk) {
        PoiManager poiManager = level.getPoiManager();
        ChunkPos pos = levelChunk.getPos();
        for (int sectionY = levelChunk.getMinSectionY(); sectionY <= levelChunk.getMaxSectionY(); sectionY++) {
            LevelChunkSection section = levelChunk.getSection(levelChunk.getSectionIndexFromSectionY(sectionY));
            poiManager.checkConsistencyWithBlocks(SectionPos.of(pos, sectionY), section);
        }
    }

    /**
     * Форсированный пересвет чанка. Проверено декомпиляцией: Starlight (light engine
     * Paper-форков) предоставляет ThreadedLevelLightEngine#starlight$serverRelightChunks(
     * Collection&lt;ChunkPos&gt;, Consumer&lt;ChunkPos&gt;, IntConsumer) — тот же метод,
     * которым штатно пользуется команда /paper relight. Операция асинхронна
     * (внутренне ставит задачу в очередь чанк-системы), поэтому оборачивается
     * в CompletableFuture, а не блокирует владеющий поток join()'ом — блокировка была бы
     * опасна, так как внутренняя очередь пересвета может выполняться на том же
     * пуле потоков чанк-системы.
     */
    private CompletableFuture<Integer> relightChunkAsync(ServerLevel level, int chunkX, int chunkZ) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        lightEngine.starlight$serverRelightChunks(
                List.of(new ChunkPos(chunkX, chunkZ)),
                relitChunkPos -> { },
                future::complete
        );
        return future;
    }

    /** Проверено декомпиляцией: LevelChunk#markUnsaved() — публичный метод, override ChunkAccess. */
    private void markChunkUnsaved(LevelChunk levelChunk) {
        levelChunk.markUnsaved();
    }
}
