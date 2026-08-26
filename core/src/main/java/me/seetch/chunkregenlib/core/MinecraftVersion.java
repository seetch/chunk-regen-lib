package me.seetch.chunkregenlib.core;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Версия Minecraft/Bukkit сервера в виде major.minor.patch, разобранная из строки
 * вида "1.21.4-R0.1-SNAPSHOT" (формат Bukkit#getBukkitVersion()).
 */
public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public MinecraftVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components must not be negative: " + major + "." + minor + "." + patch);
        }
    }

    /**
     * Разбирает версию из произвольной строки, содержащей паттерн X.Y[.Z],
     * например Bukkit#getBukkitVersion() ("1.21.4-R0.1-SNAPSHOT") или
     * Bukkit#getMinecraftVersion() ("1.21.4").
     */
    public static MinecraftVersion parse(String versionString) {
        Objects.requireNonNull(versionString, "versionString");
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Cannot parse Minecraft version from: " + versionString);
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return new MinecraftVersion(major, minor, patch);
    }

    /** Ключ вида "1_21_4", удобный для сопоставления с именем adapter-модуля. */
    public String asKey() {
        return major + "_" + minor + "_" + patch;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        int result = Integer.compare(major, other.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(minor, other.minor);
        if (result != 0) {
            return result;
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
