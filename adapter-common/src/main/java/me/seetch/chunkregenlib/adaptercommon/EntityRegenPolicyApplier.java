package me.seetch.chunkregenlib.adaptercommon;

import me.seetch.chunkregenlib.api.EntityRegenPolicy;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

/**
 * Применяет EntityRegenPolicy к сущностям чанка через публичный Bukkit API —
 * версионно-независимая логика, общая для всех adapter-модулей.
 */
public final class EntityRegenPolicyApplier {

    private EntityRegenPolicyApplier() {
    }

    /**
     * Удаляет сущности чанка согласно политике. Игроков не трогает никогда —
     * их сохранность регулируется RegenerationOptions#preservePlayers()
     * на уровне более высокого шага пайплайна, а не этим методом.
     *
     * @return количество удалённых сущностей
     */
    public static int apply(Chunk chunk, EntityRegenPolicy policy) {
        if (policy == EntityRegenPolicy.KEEP_ALL) {
            return 0;
        }

        int removed = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            if (policy == EntityRegenPolicy.KEEP_NAMED_OR_TAMED && shouldKeep(entity)) {
                continue;
            }
            entity.remove();
            removed++;
        }
        return removed;
    }

    private static boolean shouldKeep(Entity entity) {
        if (entity.customName() != null) {
            return true;
        }
        return entity instanceof Tameable tameable && tameable.isTamed();
    }
}
