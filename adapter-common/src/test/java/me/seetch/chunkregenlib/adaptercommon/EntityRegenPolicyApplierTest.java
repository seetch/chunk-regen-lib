package me.seetch.chunkregenlib.adaptercommon;

import me.seetch.chunkregenlib.api.EntityRegenPolicy;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntityRegenPolicyApplierTest {

    @Test
    void keepAllRemovesNothing() {
        Chunk chunk = mock(Chunk.class);
        Entity zombie = mock(Entity.class);
        when(chunk.getEntities()).thenReturn(new Entity[] {zombie});

        int removed = EntityRegenPolicyApplier.apply(chunk, EntityRegenPolicy.KEEP_ALL);

        assertEquals(0, removed);
        verify(zombie, never()).remove();
    }

    @Test
    void removeAllNonPlayerSkipsPlayersOnly() {
        Chunk chunk = mock(Chunk.class);
        Entity zombie = mock(Entity.class);
        Player player = mock(Player.class);
        when(chunk.getEntities()).thenReturn(new Entity[] {zombie, player});

        int removed = EntityRegenPolicyApplier.apply(chunk, EntityRegenPolicy.REMOVE_ALL_NON_PLAYER);

        assertEquals(1, removed);
        verify(zombie).remove();
        verify(player, never()).remove();
    }

    @Test
    void keepNamedOrTamedKeepsNamedAndTamedButRemovesPlain() {
        Chunk chunk = mock(Chunk.class);

        Entity plain = mock(Entity.class);
        when(plain.customName()).thenReturn(null);

        Entity named = mock(Entity.class);
        when(named.customName()).thenReturn(Component.text("Bob"));

        Tameable tamed = mock(Tameable.class);
        when(tamed.customName()).thenReturn(null);
        when(tamed.isTamed()).thenReturn(true);

        when(chunk.getEntities()).thenReturn(new Entity[] {plain, named, tamed});

        int removed = EntityRegenPolicyApplier.apply(chunk, EntityRegenPolicy.KEEP_NAMED_OR_TAMED);

        assertEquals(1, removed);
        verify(plain).remove();
        verify(named, never()).remove();
        verify(tamed, never()).remove();
    }
}
