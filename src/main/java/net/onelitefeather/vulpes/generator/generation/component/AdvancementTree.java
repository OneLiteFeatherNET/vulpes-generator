package net.onelitefeather.vulpes.generator.generation.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.advancement.AdvancementEntity;
import net.onelitefeather.vulpes.api.model.advancement.AdvancementFrameType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Validates the advancements of a project and orders them as a tree.
 * <p>
 * Every advancement is checked at generation time: the title and the description must be valid JSON text components,
 * the icon must be a known material, every parent must belong to the project and the parent relations must not form a
 * cycle. Any violation aborts the generation with an {@link IllegalArgumentException} that names the advancement key.
 * <p>
 * The result is ordered depth first from the roots: a parent always comes before its children, roots and siblings are
 * sorted by their key. Therefore the output is deterministic and both generators can emit the tree in one pass.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public final class AdvancementTree {

    private static final Material DEFAULT_MATERIAL = Material.PAPER;
    private static final String MINECRAFT_NAMESPACE = "minecraft";

    /**
     * Validates the given advancements and returns them in tree order.
     *
     * @param entities the advancements of one project
     * @return the advancements, parents before their children
     * @throws IllegalArgumentException if an advancement has an invalid title, description, icon or position, a parent
     *                                  outside of the given advancements, or if the parent relations form a cycle
     */
    public static List<AdvancementData> from(Collection<AdvancementEntity> entities) {
        Map<String, AdvancementEntity> byKey = indexByKey(entities);

        Map<String, List<String>> childrenOf = new TreeMap<>();
        List<String> roots = new ArrayList<>();
        for (Map.Entry<String, AdvancementEntity> entry : byKey.entrySet()) {
            AdvancementEntity parent = entry.getValue().getParent();
            if (parent == null) {
                roots.add(entry.getKey());
                continue;
            }
            String parentKey = parent.getKey();
            if (parentKey == null || !byKey.containsKey(parentKey)) {
                throw new IllegalArgumentException("advancement '" + entry.getKey() + "' has the parent '" + parentKey
                        + "' which is not part of the project");
            }
            childrenOf.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(entry.getKey());
        }

        List<String> order = new ArrayList<>();
        Map<String, String> rootOf = new TreeMap<>();
        for (String root : roots) {
            visit(root, root, childrenOf, rootOf, order);
        }

        for (String key : byKey.keySet()) {
            if (!rootOf.containsKey(key)) {
                throw new IllegalArgumentException("advancement '" + key + "' is part of a parent cycle");
            }
        }

        List<AdvancementData> result = new ArrayList<>(order.size());
        for (String key : order) {
            AdvancementEntity entity = byKey.get(key);
            AdvancementEntity parent = entity.getParent();
            String parentNamespacedKey = parent == null ? null : byKey.get(parent.getKey()).getNamespacedKey();
            String rootNamespacedKey = byKey.get(rootOf.get(key)).getNamespacedKey();
            result.add(toData(entity, parentNamespacedKey, rootNamespacedKey));
        }
        return List.copyOf(result);
    }

    /**
     * Resolves the material of an advancement icon. A missing value falls back to {@link Material#PAPER}. A value
     * without namespace is resolved in the {@code minecraft} namespace, so {@code STONE} becomes {@code minecraft:stone}.
     *
     * @param key      the advancement key, used in error messages
     * @param material the raw material value of the entity
     * @return the material of the icon
     * @throws IllegalArgumentException if the value does not name a known material
     */
    static Material resolveMaterial(String key, @Nullable String material) {
        if (material == null || material.isBlank()) {
            return DEFAULT_MATERIAL;
        }

        String normalized = material.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = MINECRAFT_NAMESPACE + ":" + normalized;
        }

        Result<Material> result;
        try {
            result = MaterialCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive(normalized));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("advancement '" + key + "' uses unknown material '" + material + "' (" + e.getMessage() + ")", e);
        }

        if (result instanceof Result.Ok<Material> ok) {
            return ok.value();
        }
        String reason = result instanceof Result.Error<Material> error ? error.message() : "no value decoded";
        throw new IllegalArgumentException("advancement '" + key + "' uses unknown material '" + material + "' (" + reason + ")");
    }

    /**
     * Maps the frame type of the database to the frame type of Minestom. A missing value is a task.
     *
     * @param frameType the frame type of the entity
     * @return the Minestom frame type
     */
    static FrameType toMinestomFrameType(@Nullable AdvancementFrameType frameType) {
        if (frameType == null) {
            return FrameType.TASK;
        }
        return switch (frameType) {
            case TASK -> FrameType.TASK;
            case GOAL -> FrameType.GOAL;
            case CHALLENGE -> FrameType.CHALLENGE;
        };
    }

    /**
     * Parses a title or description as JSON text component. A missing or blank value yields {@code null}.
     *
     * @param key   the advancement key, used in error messages
     * @param field the name of the field, used in error messages
     * @param text  the raw JSON text of the component
     * @return the parsed JSON, or {@code null} if the value is missing
     * @throws IllegalArgumentException if the text is not valid JSON or does not decode as a component
     */
    @Nullable
    static JsonElement parseTextComponent(String key, String field, @Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        JsonElement json;
        try {
            json = JsonParser.parseString(text);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("advancement '" + key + "' has invalid JSON for " + field + ": " + text, e);
        }

        Result<Component> result;
        try {
            result = Codec.COMPONENT.decode(Transcoder.JSON, json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("advancement '" + key + "' has an invalid " + field + ": " + text + " (" + e.getMessage() + ")", e);
        }
        if (result instanceof Result.Error<Component> error) {
            throw new IllegalArgumentException("advancement '" + key + "' has an invalid " + field + ": " + text + " (" + error.message() + ")");
        }
        return json;
    }

    private static Map<String, AdvancementEntity> indexByKey(Collection<AdvancementEntity> entities) {
        Map<String, AdvancementEntity> byKey = new TreeMap<>();
        Set<String> variableNames = new HashSet<>();
        for (AdvancementEntity entity : entities) {
            String key = entity.getKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("an advancement without a key cannot be generated");
            }
            if (byKey.containsKey(key)) {
                throw new IllegalArgumentException("advancement key '" + key + "' is used more than once");
            }
            if (!variableNames.add(entity.getVariableName())) {
                throw new IllegalArgumentException("advancement '" + key + "' has the same variable name as another advancement");
            }
            byKey.put(key, entity);
        }
        return byKey;
    }

    /**
     * Visits an advancement and then its children, sorted by key. Visited nodes are appended to the order list.
     */
    private static void visit(String key, String rootKey, Map<String, List<String>> childrenOf,
                              Map<String, String> rootOf, List<String> order) {
        rootOf.put(key, rootKey);
        order.add(key);
        for (String child : childrenOf.getOrDefault(key, List.of())) {
            visit(child, rootKey, childrenOf, rootOf, order);
        }
    }

    private static AdvancementData toData(AdvancementEntity entity, @Nullable String parentNamespacedKey, String rootNamespacedKey) {
        String key = entity.getKey();
        float x = entity.getX();
        float y = entity.getY();
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("advancement '" + key + "' must have a finite position but was x=" + x + ", y=" + y);
        }

        boolean root = parentNamespacedKey == null;
        return new AdvancementData(
                key,
                entity.getNamespacedKey(),
                entity.getVariableName(),
                entity.getUiName(),
                parentNamespacedKey,
                rootNamespacedKey,
                resolveMaterial(key, entity.getMaterial()),
                toMinestomFrameType(entity.getFrameType()),
                parseTextComponent(key, "title", entity.getTitle()),
                parseTextComponent(key, "description", entity.getDescription()),
                root ? blankToNull(entity.getBackground()) : null,
                x,
                y,
                entity.isShowToast(),
                entity.isAnnounceToChat(),
                entity.isHidden()
        );
    }

    @Nullable
    private static String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AdvancementTree() {
        // Utility class, no instances
    }
}
