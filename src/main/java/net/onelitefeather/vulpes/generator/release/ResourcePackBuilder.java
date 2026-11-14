package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.onelitefeather.vulpes.generator.generation.font.BitmapFontProviderCodec;
import net.onelitefeather.vulpes.generator.generation.font.BitmapFontProviderCodec.BitmapFontProvider;
import net.onelitefeather.vulpes.generator.generation.sound.SoundEntry;
import net.onelitefeather.vulpes.generator.generation.sound.SoundEventDefinition;
import net.onelitefeather.vulpes.generator.generation.sound.SoundEventDefinitionCodec;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.INVALID_CONTENT;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT;

/**
 * Writes the resource pack ZIP of a project. The pack holds {@code pack.mcmeta} from the pack settings, every asset of
 * the pack inputs at its path, one {@code sounds.json} per namespace with sound events and one font JSON per bitmap
 * font. The font and sound JSON are written by the codecs of the generation package, in GSON's pretty form, the same
 * as every generated font.
 * <p>
 * The ZIP is deterministic: entries are sorted by path, every entry carries the timestamp 1980-01-01T00:00, and the
 * JSON of {@code pack.mcmeta} has sorted keys. The same content always gives the same bytes.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.0.0
 */
final class ResourcePackBuilder {

    /** The timestamp of every entry, the earliest one a ZIP can carry. */
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(1980, 1, 1, 0, 0);
    private static final ObjectMapper JSON = new ObjectMapper();

    private ResourcePackBuilder() {
    }

    /**
     * Builds the ZIP of a pack.
     *
     * @param snapshot the format 1 snapshot of the project, which holds the fonts and the sound events
     * @param inputs   the pack settings and the asset files; the settings must be present
     * @param contents the bytes of each asset, by asset id, already checked against the export
     * @return the ZIP bytes
     * @throws ReleaseRejectedException with {@code UNSUPPORTED_CONTENT} when two files share one path, or
     *                                  {@code INVALID_CONTENT} when a font or sound event cannot be written
     */
    static byte[] build(byte[] snapshot, ResourcePackInputs inputs, Map<UUID, byte[]> contents) {
        if (inputs.meta() == null) {
            throw new IllegalArgumentException("a resource pack needs the pack settings of the project");
        }
        Map<String, byte[]> files = new TreeMap<>();
        add(files, "pack.mcmeta", mcmeta(inputs.meta()));
        for (ResourcePackInputs.PackAsset asset : inputs.assets()) {
            add(files, asset.path(), contents.get(asset.assetId()));
        }
        JsonNode root = parse(snapshot);
        fonts(root, files);
        sounds(root, files);
        return zip(files);
    }

    /** Writes the pack meta: the description, the formats and the enabled features of the settings. */
    private static byte[] mcmeta(JsonNode meta) {
        ObjectNode root = JSON.createObjectNode();
        ObjectNode pack = root.putObject("pack");
        pack.set("description", meta.path("description"));
        pack.set("min_format", format(meta.path("minFormatMajor").asInt(), meta.path("minFormatMinor").asInt()));
        JsonNode maxMinor = meta.get("maxFormatMinor");
        pack.set("max_format", maxMinor == null || maxMinor.isNull()
                ? JSON.getNodeFactory().numberNode(meta.path("maxFormatMajor").asInt())
                : format(meta.path("maxFormatMajor").asInt(), maxMinor.asInt()));
        JsonNode features = meta.path("enabledFeatures");
        if (features.isArray() && !features.isEmpty()) {
            ArrayNode enabled = root.putObject("features").putArray("enabled");
            features.forEach(feature -> enabled.add(feature.asText()));
        }
        return CanonicalJson.write(root);
    }

    /** A format as an integer when its minor version is zero, otherwise as {@code [major, minor]}. */
    private static JsonNode format(int major, int minor) {
        if (minor == 0) {
            return JSON.getNodeFactory().numberNode(major);
        }
        ArrayNode pair = JSON.createArrayNode();
        pair.add(major).add(minor);
        return pair;
    }

    /** Writes one {@code assets/<namespace>/font/<name>.json} per font key, with one provider per glyph row group. */
    private static void fonts(JsonNode root, Map<String, byte[]> files) {
        Map<String, JsonArray> providersByFont = new TreeMap<>();
        for (JsonNode font : root.path("fonts")) {
            String key = font.path("font").asText();
            List<String> chars = new ArrayList<>();
            font.path("chars").forEach(row -> chars.add(row.asText()));
            BitmapFontProvider provider;
            try {
                provider = new BitmapFontProvider(font.path("file").asText(), font.path("ascent").asInt(),
                        font.path("height").asInt(), chars);
            } catch (IllegalArgumentException e) {
                throw new ReleaseRejectedException(INVALID_CONTENT, "font " + key + " cannot be written: " + e.getMessage());
            }
            providersByFont.computeIfAbsent(key, k -> new JsonArray()).add(BitmapFontProviderCodec.toJson(provider));
        }
        providersByFont.forEach((key, providers) -> {
            JsonObject font = new JsonObject();
            font.add("providers", providers);
            add(files, assetPath(key, "font/", ".json"), GsonHolder.GSON.toJson(font).getBytes(StandardCharsets.UTF_8));
        });
    }

    /** Writes one {@code assets/<namespace>/sounds.json} per namespace that has sound events. */
    private static void sounds(JsonNode root, Map<String, byte[]> files) {
        Map<String, JsonObject> eventsByNamespace = new TreeMap<>();
        for (JsonNode sound : root.path("sounds")) {
            String key = sound.path("key").asText();
            JsonElement definition = encodeEvent(key, sound);
            String namespace = namespaceOf(key);
            eventsByNamespace.computeIfAbsent(namespace, n -> new JsonObject())
                    .add(pathOf(key), definition);
        }
        eventsByNamespace.forEach((namespace, events) ->
                add(files, "assets/" + namespace + "/sounds.json",
                        GsonHolder.GSON.toJson(events).getBytes(StandardCharsets.UTF_8)));
    }

    private static JsonElement encodeEvent(String key, JsonNode sound) {
        List<SoundEntry> entries = new ArrayList<>();
        for (JsonNode source : sound.path("sounds")) {
            entries.add(new SoundEntry(source.path("name").asText(),
                    (float) source.path("volume").asDouble(1.0),
                    (float) source.path("pitch").asDouble(1.0),
                    source.path("weight").asInt(1),
                    source.path("stream").asBoolean(false),
                    source.path("attenuationDistance").asInt(16),
                    source.path("preload").asBoolean(false),
                    source.path("type").asText("file")));
        }
        JsonNode subtitle = sound.get("subtitle");
        SoundEventDefinition definition = new SoundEventDefinition(sound.path("replace").asBoolean(false),
                subtitle == null || subtitle.isNull() ? null : subtitle.asText(), entries);
        Result<JsonElement> result;
        try {
            result = SoundEventDefinitionCodec.CODEC.encode(Transcoder.JSON, definition);
        } catch (IllegalArgumentException e) {
            throw new ReleaseRejectedException(INVALID_CONTENT, "sound " + key + " cannot be written: " + e.getMessage());
        }
        if (result instanceof Result.Ok<JsonElement> ok) {
            return ok.value();
        }
        String reason = result instanceof Result.Error<JsonElement> error ? error.message() : "no value encoded";
        throw new ReleaseRejectedException(INVALID_CONTENT, "sound " + key + " cannot be written: " + reason);
    }

    /** The path of a font JSON, {@code assets/<namespace>/<folder><name><extension>}, for a key {@code namespace:name}. */
    private static String assetPath(String key, String folder, String extension) {
        return "assets/" + namespaceOf(key) + "/" + folder + pathOf(key) + extension;
    }

    private static String namespaceOf(String key) {
        return key.substring(0, key.indexOf(':'));
    }

    private static String pathOf(String key) {
        return key.substring(key.indexOf(':') + 1);
    }

    private static void add(Map<String, byte[]> files, String path, byte[] bytes) {
        if (files.putIfAbsent(path, bytes) != null) {
            throw new ReleaseRejectedException(UNSUPPORTED_CONTENT, "two files of the resource pack are written to " + path);
        }
    }

    private static JsonNode parse(byte[] snapshot) {
        try {
            return JSON.readTree(snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("the snapshot of a release must be valid JSON", e);
        }
    }

    private static byte[] zip(Map<String, byte[]> files) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTimeLocal(ENTRY_TIME);
                entry.setMethod(ZipEntry.DEFLATED);
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("the resource pack cannot be written to memory", e);
        }
        return out.toByteArray();
    }
}
