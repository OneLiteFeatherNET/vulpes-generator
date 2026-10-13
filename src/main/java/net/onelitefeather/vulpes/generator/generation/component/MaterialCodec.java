package net.onelitefeather.vulpes.generator.generation.component;

import net.minestom.server.codec.Codec;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.ApiStatus;

/**
 * Codec for {@link Material} values that are stored as namespaced key strings, for example {@code "minecraft:stone"}.
 * <p>
 * Decoding is strict: an unknown or empty key produces an error result instead of falling back to a default material.
 * Encoding writes the namespaced key of the material.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public final class MaterialCodec {

    /**
     * The codec which maps a JSON string key to a {@link Material} and back.
     */
    public static final Codec<Material> CODEC = Codec.STRING.transform(
            MaterialCodec::decodeKey,
            material -> material.key().asString()
    );

    /**
     * Resolves a material from its namespaced key.
     *
     * @param key the namespaced key, for example {@code "minecraft:stone"}
     * @return the material registered for the key
     * @throws IllegalArgumentException if the key is empty, malformed or does not belong to a known material
     */
    private static Material decodeKey(String key) {
        if (key.isBlank()) {
            throw new IllegalArgumentException("material key must not be empty");
        }

        Material material;
        try {
            material = Material.fromKey(key);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("malformed material key '" + key + "': " + e.getMessage(), e);
        }

        if (material == null) {
            throw new IllegalArgumentException("unknown material key '" + key + "'");
        }
        return material;
    }

    private MaterialCodec() {
        // Utility class, no instances
    }
}
