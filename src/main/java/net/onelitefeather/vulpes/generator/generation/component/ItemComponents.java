package net.onelitefeather.vulpes.generator.generation.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.item.ItemComponentEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static net.onelitefeather.vulpes.generator.generation.component.ItemComponentKeys.AMOUNT;
import static net.onelitefeather.vulpes.generator.generation.component.ItemComponentKeys.CUSTOM_NAMESPACE;
import static net.onelitefeather.vulpes.generator.generation.component.ItemComponentKeys.DEFAULT_AMOUNT;
import static net.onelitefeather.vulpes.generator.generation.component.ItemComponentKeys.DEFAULT_MATERIAL;
import static net.onelitefeather.vulpes.generator.generation.component.ItemComponentKeys.MATERIAL;

/**
 * The item data that is needed by the generator, split from the raw component entities of an item.
 * <p>
 * The material and the amount are decoded with their codecs. Every other component that is not part of the
 * {@link ItemComponentKeys#CUSTOM_NAMESPACE} namespace is kept as a {@link RawItemComponent} and sorted by key, so the
 * generated output is deterministic.
 *
 * @param material   the material of the item
 * @param amount     the stack amount of the item
 * @param components the remaining data components sorted by key, never {@code null}
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public record ItemComponents(Material material, int amount, List<RawItemComponent> components) {

    /**
     * Creates the item components and copies the given list.
     *
     * @param material   the material of the item
     * @param amount     the stack amount of the item
     * @param components the remaining data components, {@code null} is treated as an empty list
     */
    public ItemComponents {
        Objects.requireNonNull(material, "material must not be null");
        components = components == null ? List.of() : List.copyOf(components);
    }

    /**
     * Builds the item components from the raw component entities of an item.
     * <p>
     * A missing material falls back to {@link ItemComponentKeys#DEFAULT_MATERIAL}, a missing amount falls back to
     * {@link ItemComponentKeys#DEFAULT_AMOUNT}. A component with a {@code null} value is handled like a missing key
     * for the material and the amount and is skipped for all other keys.
     *
     * @param entities the component entities of the item, may be {@code null}
     * @return the decoded item components
     * @throws IllegalArgumentException if a key is {@code null} or duplicated, or if the material or amount value is invalid
     */
    public static ItemComponents from(@Nullable Collection<ItemComponentEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ItemComponents(DEFAULT_MATERIAL, DEFAULT_AMOUNT, List.of());
        }

        Material material = null;
        Integer amount = null;
        List<RawItemComponent> remaining = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (ItemComponentEntity entity : entities) {
            String key = entity.getComponentKey();
            if (key == null) {
                throw new IllegalArgumentException("item component key must not be null");
            }
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("duplicate item component key '" + key + "'");
            }

            String value = entity.getComponentValue();
            if (value == null) {
                continue;
            }

            if (MATERIAL.equals(key)) {
                material = decode(MaterialCodec.CODEC, key, value);
            } else if (AMOUNT.equals(key)) {
                amount = decode(AmountCodec.CODEC, key, value);
            } else if (!key.startsWith(CUSTOM_NAMESPACE + ":")) {
                remaining.add(new RawItemComponent(key, value));
            }
        }

        remaining.sort(Comparator.comparing(RawItemComponent::key));
        return new ItemComponents(
                material == null ? DEFAULT_MATERIAL : material,
                amount == null ? DEFAULT_AMOUNT : amount,
                remaining
        );
    }

    /**
     * Parses the raw JSON text of a component and decodes it with the given codec.
     *
     * @param codec    the codec for the component value
     * @param key      the component key, used in error messages
     * @param jsonText the raw JSON text of the component value
     * @param <T>      the decoded type
     * @return the decoded value
     * @throws IllegalArgumentException if the text is not valid JSON or the codec rejects the value
     */
    private static <T> T decode(Codec<T> codec, String key, String jsonText) {
        JsonElement json;
        try {
            json = JsonParser.parseString(jsonText);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("invalid JSON for item component '" + key + "': " + jsonText, e);
        }

        Result<T> result = codec.decode(Transcoder.JSON, json);
        if (result instanceof Result.Ok<T> ok) {
            return ok.value();
        }

        String reason = result instanceof Result.Error<T> error ? error.message() : "no value decoded";
        throw new IllegalArgumentException("invalid value for item component '" + key + "': " + jsonText + " (" + reason + ")");
    }
}
