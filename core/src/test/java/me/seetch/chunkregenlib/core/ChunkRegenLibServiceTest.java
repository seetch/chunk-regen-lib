package me.seetch.chunkregenlib.core;

import me.seetch.chunkregenlib.api.ChunkRegenerator;
import me.seetch.chunkregenlib.api.exceptions.UnsupportedServerVersionException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkRegenLibServiceTest {

    private MockedStatic<Bukkit> bukkitMock;

    @AfterEach
    void tearDown() {
        if (bukkitMock != null) {
            bukkitMock.close();
        }
    }

    @Test
    void createForResolvesRegisteredAdapter() {
        bukkitMock = Mockito.mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getBukkitVersion).thenReturn("1.21.4-R0.1-SNAPSHOT");

        Plugin plugin = Mockito.mock(Plugin.class);
        ChunkRegenerator regenerator = ChunkRegenLibService.createFor(plugin);

        assertNotNull(regenerator);
    }

    @Test
    void createForThrowsWhenNoAdapterMatchesVersion() {
        bukkitMock = Mockito.mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getBukkitVersion).thenReturn("1.19.4-R0.1-SNAPSHOT");

        Plugin plugin = Mockito.mock(Plugin.class);

        assertThrows(UnsupportedServerVersionException.class, () -> ChunkRegenLibService.createFor(plugin));
    }

    @Test
    void createAndRegisterRegistersWithServicesManager() {
        bukkitMock = Mockito.mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getBukkitVersion).thenReturn("1.21.4-R0.1-SNAPSHOT");
        ServicesManager servicesManager = Mockito.mock(ServicesManager.class);
        bukkitMock.when(Bukkit::getServicesManager).thenReturn(servicesManager);

        Plugin plugin = Mockito.mock(Plugin.class);
        ChunkRegenerator regenerator = ChunkRegenLibService.createAndRegister(plugin);

        Mockito.verify(servicesManager).register(
                Mockito.eq(ChunkRegenerator.class),
                Mockito.eq(regenerator),
                Mockito.eq(plugin),
                Mockito.any()
        );
    }
}
