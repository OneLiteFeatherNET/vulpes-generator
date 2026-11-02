package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT;

/**
 * Maps the entities of one project to a canonical format 1 snapshot. The mapping is pure: it reads the
 * {@link ProjectData} and never talks to the backend.
 * <p>
 * Keys without a namespace get the project key as namespace. A key whose first dot-separated segment is one of the
 * foreign namespaces gets that segment as namespace, because the backend cannot store a second namespace in a key.
 * Content that the format cannot express is rejected with {@link ReleaseRejectedException.Reason#UNSUPPORTED_CONTENT},
 * never dropped.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
final class ProjectSnapshotMapper {

    /**
     * The resolution written for every texture. The schema requires an integer resolution for textures and the
     * backend stores none; the pack is built for the single resolution {@code 16}.
     */
    static final int TEXTURE_RESOLUTION = 16;

    private static final String MATERIAL = "stelaris:material";
    private static final String LORE = "minecraft:lore";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Set<String> foreignNamespaces;

    /**
     * Creates the mapper.
     *
     * @param foreignNamespaces the first key segments that are namespaces of their own
     */
    ProjectSnapshotMapper(List<String> foreignNamespaces) {
        this.foreignNamespaces = Set.copyOf(foreignNamespaces);
    }

    /**
     * Maps one project to its canonical snapshot.
     *
     * @param data the entities of the project
     * @return the snapshot bytes and the project key
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#UNSUPPORTED_CONTENT} when the project
     *                                  has content the format cannot express
     */
    AssembledSnapshot map(ProjectData data) {
        String projectId = requiredText(data.project(), "id");
        String namespace = requiredText(data.project(), "key");
        String displayName = requiredText(data.project(), "displayName");

        ObjectNode root = JSON.createObjectNode();
        root.put("format", 1);
        root.put("projectId", projectId);
        root.put("namespace", namespace);
        root.set("items", items(data, namespace));
        root.set("attributes", attributes(data, namespace));
        root.set("fonts", fonts(data, namespace));
        root.set("sounds", sounds(data, namespace));
        root.set("advancements", advancements(data));
        root.set("expressionFunctions", expressionFunctions(data));
        root.set("expressions", expressions(data));
        root.set("assets", assets(data, namespace));
        root.set("pack", pack(displayName));
        return new AssembledSnapshot(namespace, CanonicalJson.write(root));
    }

    private ArrayNode items(ProjectData data, String namespace) {
        List<ObjectNode> result = new ArrayList<>();
        for (JsonNode item : data.items()) {
            String id = requiredText(item, "id");
            String key = key(requiredText(item, "key"), namespace);
            ObjectNode components = JSON.createObjectNode();
            String material = null;
            for (JsonNode component : data.components().getOrDefault(id, List.of())) {
                String name = requiredText(component, "componentKey");
                JsonNode value = component.get("value");
                if (MATERIAL.equals(name)) {
                    material = value != null && value.isTextual() ? value.textValue() : null;
                } else {
                    components.set(name, value);
                }
            }
            if (material == null) {
                throw unsupported("item " + key + " has no " + MATERIAL + " text component, the release needs its material");
            }
            List<JsonNode> lore = sortedByOrderIndex(data.lore().getOrDefault(id, List.of()));
            if (!lore.isEmpty()) {
                if (components.has(LORE)) {
                    throw unsupported("item " + key + " has lore and a " + LORE + " component, only one may describe its lore");
                }
                ArrayNode lines = JSON.createArrayNode();
                for (JsonNode line : lore) {
                    lines.addObject().put("text", requiredText(line, "text"));
                }
                components.set(LORE, lines);
            }
            ObjectNode entry = JSON.createObjectNode();
            entry.put("id", id);
            entry.set("previousKeys", JSON.createArrayNode());
            entry.put("key", key);
            entry.put("material", key(material, namespace));
            entry.put("amount", 1);
            entry.set("components", components);
            result.add(entry);
        }
        return sortedBy(result, "key");
    }

    private ArrayNode attributes(ProjectData data, String namespace) {
        List<ObjectNode> result = new ArrayList<>();
        for (JsonNode attribute : data.attributes()) {
            ObjectNode entry = JSON.createObjectNode();
            entry.put("id", requiredText(attribute, "id"));
            entry.set("previousKeys", JSON.createArrayNode());
            entry.put("key", key(requiredText(attribute, "key"), namespace));
            entry.put("displayName", requiredText(attribute, "uiName"));
            entry.put("translationKey", text(attribute, "translationKey"));
            entry.set("defaultValue", attribute.get("defaultValue"));
            entry.set("minimumValue", attribute.get("minimumValue"));
            entry.set("maximumValue", attribute.get("maximumValue"));
            entry.set("synced", attribute.get("synced"));
            result.add(entry);
        }
        return sortedBy(result, "key");
    }

    private ArrayNode fonts(ProjectData data, String namespace) {
        List<ObjectNode> result = new ArrayList<>();
        for (JsonNode font : data.fonts()) {
            String id = requiredText(font, "id");
            String key = key(requiredText(font, "key"), namespace);
            String provider = text(font, "provider");
            if (!"bitmap".equals(provider)) {
                throw unsupported("font " + key + " uses provider " + provider + ", only bitmap fonts are supported");
            }
            String mapper = text(font, "mapper");
            if (mapper == null) {
                throw unsupported("font " + key + " has no mapper, the mapper names its font");
            }
            ArrayNode chars = JSON.createArrayNode();
            for (JsonNode row : sortedByOrderIndex(data.glyphRows().getOrDefault(id, List.of()))) {
                String line = text(row, "line");
                if (line == null || line.isEmpty()) {
                    throw unsupported("font " + key + " has an empty glyph row, dropping it would shift the rows below");
                }
                chars.add(line);
            }
            if (chars.isEmpty()) {
                throw unsupported("font " + key + " has no glyph rows, the format needs at least one");
            }
            ObjectNode entry = JSON.createObjectNode();
            entry.put("id", id);
            entry.set("previousKeys", JSON.createArrayNode());
            entry.put("key", key);
            entry.put("font", namespace + ':' + mapper);
            entry.put("type", "bitmap");
            entry.put("file", requiredText(font, "texturePath"));
            entry.set("ascent", font.get("ascent"));
            entry.set("height", font.get("height"));
            entry.set("chars", chars);
            result.add(entry);
        }
        return sortedBy(result, "key");
    }

    private ArrayNode sounds(ProjectData data, String namespace) {
        List<ObjectNode> result = new ArrayList<>();
        for (JsonNode sound : data.sounds()) {
            String id = requiredText(sound, "id");
            ArrayNode entries = JSON.createArrayNode();
            List<ObjectNode> sources = new ArrayList<>();
            for (JsonNode source : data.sources().getOrDefault(id, List.of())) {
                ObjectNode entry = JSON.createObjectNode();
                entry.put("name", key(requiredText(source, "name"), namespace));
                entry.put("type", lower(requiredText(source, "type")));
                entry.set("volume", source.get("volume"));
                entry.set("pitch", source.get("pitch"));
                entry.set("weight", source.get("weight"));
                entry.set("stream", source.get("stream"));
                entry.set("attenuationDistance", source.get("attenuationDistance"));
                entry.set("preload", source.get("preload"));
                sources.add(entry);
            }
            sortedBy(sources, "name").forEach(entries::add);

            ObjectNode entry = JSON.createObjectNode();
            entry.put("id", id);
            entry.set("previousKeys", JSON.createArrayNode());
            entry.put("key", key(requiredText(sound, "key"), namespace));
            entry.put("subtitle", blankToNull(text(sound, "subTitle")));
            entry.put("replace", false);
            entry.set("sounds", entries);
            result.add(entry);
        }
        return sortedBy(result, "key");
    }

    private ArrayNode advancements(ProjectData data) {
        if (!data.advancements().isEmpty()) {
            List<String> names = new ArrayList<>();
            data.advancements().forEach(advancement -> names.add(text(advancement, "key")));
            throw unsupported("advancements are not supported yet, the project has: " + String.join(", ", names));
        }
        return JSON.createArrayNode();
    }

    private ArrayNode expressionFunctions(ProjectData data) {
        List<ObjectNode> result = new ArrayList<>();
        for (JsonNode function : data.expressionFunctions()) {
            String name = requiredText(function, "name");
            ObjectNode entry = JSON.createObjectNode();
            entry.put("id", requiredText(function, "id"));
            entry.set("previousNames", JSON.createArrayNode());
            entry.put("name", name);
            entry.put("kind", functionKind(name, requiredText(function, "kind")));
            entry.put("description", blankToNull(text(function, "comment")));
            entry.set("parameters", typedNames(function.get("parameters"), "name", "type"));
            entry.put("returnType", lower(requiredText(function, "returnType")));
            entry.put("expression", text(function, "body"));
            result.add(entry);
        }
        return sortedBy(result, "name");
    }

    private ArrayNode expressions(ProjectData data) {
        List<ObjectNode> result = new ArrayList<>();
        for (JsonNode expression : data.expressions()) {
            ObjectNode entry = JSON.createObjectNode();
            entry.put("id", requiredText(expression, "id"));
            entry.set("previousNames", JSON.createArrayNode());
            entry.put("name", requiredText(expression, "uiName"));
            entry.put("description", blankToNull(text(expression, "comment")));
            entry.set("variables", typedNames(expression.get("variables"), "name", "type"));
            entry.put("returnType", "number");
            entry.put("expression", requiredText(expression, "expression"));
            result.add(entry);
        }
        return sortedBy(result, "name");
    }

    private ArrayNode assets(ProjectData data, String projectNamespace) {
        List<ObjectNode> result = new ArrayList<>();
        List<String> rejectedAssets = new ArrayList<>();
        for (JsonNode asset : data.assets()) {
            String rawKey = requiredText(asset, "key");
            String kind = requiredText(asset, "kind");
            String qualified = key(rawKey, projectNamespace);
            int colon = qualified.indexOf(':');
            String namespace = qualified.substring(0, colon);
            String path = qualified.substring(colon + 1);
            String packPath;
            String packKind;
            Integer resolution;
            switch (kind) {
                case "TEXTURE" -> {
                    packPath = "assets/" + namespace + "/textures/" + path + ".png";
                    packKind = "texture";
                    resolution = TEXTURE_RESOLUTION;
                }
                case "MODEL" -> {
                    packPath = "assets/" + namespace + "/models/" + path + ".json";
                    packKind = "model";
                    resolution = null;
                }
                default -> {
                    rejectedAssets.add(rawKey + " (" + kind + ")");
                    continue;
                }
            }
            ObjectNode entry = JSON.createObjectNode();
            entry.put("path", packPath);
            entry.put("sha1", requiredText(asset, "sha1"));
            entry.set("size", asset.get("size"));
            entry.put("kind", packKind);
            if (resolution == null) {
                entry.putNull("resolution");
            } else {
                entry.put("resolution", resolution);
            }
            result.add(entry);
        }
        if (!rejectedAssets.isEmpty()) {
            throw unsupported("assets of these kinds cannot be released yet: " + String.join(", ", rejectedAssets));
        }
        return sortedBy(result, "path");
    }

    private static ObjectNode pack(String displayName) {
        ObjectNode pack = JSON.createObjectNode();
        pack.putObject("description").put("text", displayName);
        pack.putArray("resolutions").add(TEXTURE_RESOLUTION);
        return pack;
    }

    /** Maps {@code [{name, type}]} entries in their order, with the type in lower case. */
    private static ArrayNode typedNames(JsonNode entries, String nameField, String typeField) {
        ArrayNode result = JSON.createArrayNode();
        if (entries == null) {
            return result;
        }
        for (JsonNode entry : entries) {
            ObjectNode typed = result.addObject();
            typed.put("name", requiredText(entry, nameField));
            typed.put("type", lower(requiredText(entry, typeField)));
        }
        return result;
    }

    private static String functionKind(String name, String kind) {
        return switch (kind) {
            case "FORMULA" -> "formula";
            case "INTERFACE" -> "interface";
            default -> throw unsupported("expression function " + name + " has the kind " + kind + ", only FORMULA and INTERFACE are supported");
        };
    }

    /**
     * Qualifies a key. A key that already names a namespace is kept; a foreign first segment becomes the namespace;
     * anything else gets the project namespace.
     */
    private String key(String raw, String namespace) {
        if (raw.indexOf(':') >= 0) {
            return raw;
        }
        int dot = raw.indexOf('.');
        if (dot > 0 && foreignNamespaces.contains(raw.substring(0, dot))) {
            return raw.substring(0, dot) + ':' + raw.substring(dot + 1);
        }
        return namespace + ':' + raw;
    }

    private static List<JsonNode> sortedByOrderIndex(List<JsonNode> entries) {
        List<JsonNode> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(entry -> entry.path("orderIndex").asInt()));
        return sorted;
    }

    private static ArrayNode sortedBy(List<ObjectNode> entries, String field) {
        List<ObjectNode> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(entry -> entry.get(field).asText()));
        ArrayNode array = JSON.createArrayNode();
        sorted.forEach(array::add);
        return array;
    }

    private static String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            throw new ReleaseRejectedException(SOURCE_UNAVAILABLE,
                    "backend answered an entry without " + field);
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static ReleaseRejectedException unsupported(String message) {
        return new ReleaseRejectedException(UNSUPPORTED_CONTENT, message);
    }
}
