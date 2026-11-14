package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT;

/**
 * Checks the content of an assembled format 1 snapshot before a release or a local build. Every problem is collected
 * in one pass: references to assets, unique and valid keys and paths, and the consistency of bitmap font glyphs.
 * <p>
 * A reference is resolved against the {@code assets} of the snapshot, by the path a pack would write it to:
 * a texture {@code ns:path} is {@code assets/ns/textures/path} and a sound file {@code ns:path} is
 * {@code assets/ns/sounds/path.ogg}. Item models name item model definitions, which live in the game's own pack and
 * are not project assets, so they are not resolved.
 * References in the {@code minecraft} namespace are vanilla resources and are not checked.
 * <p>
 * Each problem is a {@link ReleaseDiagnostic} without file and line; its message starts with the content entry, for
 * example {@code sound cygnus:vhs_static: ...}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
final class ContentValidator {

    private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern ASSET_PATH = Pattern.compile("assets/[a-z0-9_.-]+/[a-z0-9_./-]+\\.(png|json|ogg)");
    private static final String VANILLA = "minecraft";
    private static final ObjectMapper JSON = new ObjectMapper();

    private ContentValidator() {
    }

    /**
     * Validates the content of a snapshot.
     *
     * @param snapshot the canonical snapshot bytes
     * @return every problem in snapshot order, empty when the content is valid
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#UNSUPPORTED_FORMAT} when the bytes
     *                                  are not a JSON object
     */
    static List<ReleaseDiagnostic> validate(byte[] snapshot) {
        JsonNode root = parse(snapshot);
        Problems problems = new Problems();
        Set<String> assetPaths = assetPaths(root, problems);
        items(root, problems);
        fonts(root, assetPaths, problems);
        sounds(root, assetPaths, problems);
        return problems.list();
    }

    private static Set<String> assetPaths(JsonNode root, Problems problems) {
        Set<String> paths = new HashSet<>();
        for (JsonNode asset : array(root, "assets")) {
            String path = asset.path("path").asText("");
            String entry = "asset " + path;
            if (!ASSET_PATH.matcher(path).matches()) {
                problems.add(entry, "the path is not a valid asset path");
            }
            if (!paths.add(path)) {
                problems.add(entry, "is listed more than once");
            }
        }
        return paths;
    }

    private static void items(JsonNode root, Problems problems) {
        Set<String> keys = new HashSet<>();
        for (JsonNode item : array(root, "items")) {
            String key = item.path("key").asText("");
            String entry = "item " + key;
            checkKey(entry, key, keys, problems);
        }
    }

    private static void fonts(JsonNode root, Set<String> assetPaths, Problems problems) {
        Set<String> keys = new HashSet<>();
        Map<String, Map<Integer, String>> glyphOwners = new HashMap<>();
        for (JsonNode font : array(root, "fonts")) {
            String key = font.path("key").asText("");
            String entry = "font " + key;
            checkKey(entry, key, keys, problems);
            resolve(entry, font.path("file").asText(""), "textures/", "", assetPaths, problems);
            Map<Integer, String> owners = glyphOwners.computeIfAbsent(font.path("font").asText(""), fontKey -> new HashMap<>());
            glyphs(entry, key, font, owners, problems);
        }
    }

    /** Checks that the glyph rows have one width and that no character of the font key is used twice. */
    private static void glyphs(String entry, String key, JsonNode font, Map<Integer, String> owners, Problems problems) {
        int width = -1;
        boolean uneven = false;
        for (JsonNode row : array(font, "chars")) {
            String line = row.asText("");
            int length = line.codePointCount(0, line.length());
            if (width < 0) {
                width = length;
            } else if (length != width && !uneven) {
                uneven = true;
                problems.add(entry, "glyph rows have different widths, " + width + " and " + length);
            }
            line.codePoints().forEach(codePoint -> {
                String prior = owners.putIfAbsent(codePoint, key);
                if (prior != null) {
                    problems.add(entry, "character " + codePointName(codePoint) + " is also used by " + prior
                            + " under the font " + font.path("font").asText(""));
                }
            });
        }
    }

    private static void sounds(JsonNode root, Set<String> assetPaths, Problems problems) {
        Set<String> keys = new HashSet<>();
        for (JsonNode sound : array(root, "sounds")) {
            String key = sound.path("key").asText("");
            String entry = "sound " + key;
            checkKey(entry, key, keys, problems);
            for (JsonNode source : array(sound, "sounds")) {
                if ("file".equals(source.path("type").asText(""))) {
                    resolve(entry, source.path("name").asText(""), "sounds/", ".ogg", assetPaths, problems);
                }
            }
        }
    }

    private static void checkKey(String entry, String key, Set<String> keys, Problems problems) {
        if (!RESOURCE_LOCATION.matcher(key).matches()) {
            problems.add(entry, "the key is not a valid resource location");
        }
        if (!keys.add(key)) {
            problems.add(entry, "the key is used by more than one entry");
        }
    }

    /** Resolves a reference to the asset path a pack would write it to, unless it is vanilla. */
    private static void resolve(String entry, String reference, String folder, String extension,
                                Set<String> assetPaths, Problems problems) {
        if (!RESOURCE_LOCATION.matcher(reference).matches()) {
            problems.add(entry, "refers to " + reference + ", which is not a valid resource location");
            return;
        }
        int colon = reference.indexOf(':');
        String namespace = reference.substring(0, colon);
        if (VANILLA.equals(namespace)) {
            return;
        }
        String path = "assets/" + namespace + "/" + folder + reference.substring(colon + 1) + extension;
        if (!assetPaths.contains(path)) {
            problems.add(entry, "refers to " + reference + ", which has no asset entry (" + path + ")");
        }
    }

    private static String codePointName(int codePoint) {
        return String.format("U+%04X", codePoint);
    }

    private static JsonNode parse(byte[] snapshot) {
        try {
            JsonNode root = JSON.readTree(snapshot);
            if (root == null || !root.isObject()) {
                throw new ReleaseRejectedException(UNSUPPORTED_FORMAT, "the snapshot is not a JSON object");
            }
            return root;
        } catch (IOException e) {
            throw new ReleaseRejectedException(UNSUPPORTED_FORMAT, "the snapshot is not valid JSON");
        }
    }

    private static List<JsonNode> array(JsonNode node, String field) {
        List<JsonNode> entries = new ArrayList<>();
        JsonNode value = node.get(field);
        if (value != null && value.isArray()) {
            value.forEach(entries::add);
        }
        return entries;
    }

    /** Collects the problems of one validation in the order they were found. */
    private static final class Problems {
        private final List<ReleaseDiagnostic> diagnostics = new ArrayList<>();

        void add(String entry, String what) {
            diagnostics.add(new ReleaseDiagnostic(null, 0, entry + ": " + what));
        }

        List<ReleaseDiagnostic> list() {
            return List.copyOf(diagnostics);
        }
    }
}
