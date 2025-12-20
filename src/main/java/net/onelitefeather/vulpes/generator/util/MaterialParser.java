package net.onelitefeather.vulpes.generator.util;

import net.minestom.server.item.Material;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for parsing {@link Material} instances from string keys.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 */
@ApiStatus.Internal
public final class MaterialParser {

    /**
     * Parses a material from a given key.
     *
     * @param key the key to parse the material from, can be null or empty.
     * @return the parsed material, or {@link Material#STONE} if the key is null or empty, or if the key does not correspond to a valid material.
     */
    public static @NotNull Material fromKey(@Nullable String key) {
        if (key == null || key.isEmpty()) {
            return Material.STONE; // Default material if key is null or empty
        }

        Material material = Material.fromKey(key);

        if (material == null) {
            return Material.STONE;
        }

        return material;
    }

    private MaterialParser() {
        // Private constructor to prevent instantiation
    }
}
