package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.json;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;

/**
 * The fixtures of the resource pack tests: one texture, one sound and one icon of the project {@code cygnus}, with
 * their bytes, and a format 1 snapshot that holds one bitmap font and one sound event.
 *
 * @author TheMeinerLP
 */
final class PackTestData {

    static final UUID TEXTURE_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    static final UUID SOUND_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
    static final UUID ICON_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a3");
    static final String TEXTURE_PATH = "assets/cygnus/textures/font/glitch.png";
    static final String SOUND_PATH = "assets/cygnus/sounds/ambient/drone.ogg";
    static final byte[] TEXTURE = "glitch-png".getBytes(StandardCharsets.UTF_8);
    static final byte[] SOUND = "drone-ogg".getBytes(StandardCharsets.UTF_8);
    static final byte[] ICON = "icon-png".getBytes(StandardCharsets.UTF_8);

    /** The bytes of every asset of the fixtures, by asset id. */
    static final Map<UUID, byte[]> CONTENTS = Map.of(TEXTURE_ID, TEXTURE, SOUND_ID, SOUND, ICON_ID, ICON);

    /** A format 1 snapshot with the font {@code cygnus:glitch} and the sound event {@code cygnus:ambient.drone}. */
    static final String SNAPSHOT = """
            {"format":1,"projectId":"0f8fad5b-d9cb-469f-a165-8c0a7b6c3d11","namespace":"cygnus",
             "fonts":[{"key":"cygnus:glitch","font":"cygnus:glitch","type":"bitmap","file":"cygnus:font/glitch.png",
                       "ascent":7,"height":8,"chars":["\\ue000"]}],
             "sounds":[{"key":"cygnus:ambient.drone","subtitle":null,"replace":false,
                        "sounds":[{"name":"cygnus:ambient/drone","type":"file","volume":1.0,"pitch":1.0,"weight":1,
                                   "stream":false,"attenuationDistance":16,"preload":false}]}]}
            """;

    private PackTestData() {
    }

    /** The pack settings of the export: resource pack, format 46, no minor version, no features, the icon. */
    static JsonNode meta(String description) {
        return obj("id", "00000000-0000-4000-8000-0000000000b1", "packType", "RESOURCE",
                "description", obj("text", description), "minFormatMajor", 46, "minFormatMinor", 0,
                "maxFormatMajor", 46, "maxFormatMinor", null, "enabledFeatures", json("[]"),
                "iconAssetId", ICON_ID.toString());
    }

    /** The pack inputs of the fixtures: the settings, the texture and the sound, and the icon as {@code pack.png}. */
    static ResourcePackInputs inputs(JsonNode meta) {
        return new ResourcePackInputs(meta, java.util.List.of(
                asset(TEXTURE_PATH, TEXTURE_ID, TEXTURE),
                asset(SOUND_PATH, SOUND_ID, SOUND),
                asset("pack.png", ICON_ID, ICON)));
    }

    /** A pack input of one asset, with the SHA-1 and size of the given bytes as the export holds them. */
    static ResourcePackInputs.PackAsset asset(String path, UUID id, byte[] bytes) {
        return new ResourcePackInputs.PackAsset(path, id, sha1(bytes), bytes.length);
    }

    /** The SHA-1 of the bytes as lower-case hex, the form the export and release.json use. */
    static String sha1(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is required by every Java runtime", e);
        }
    }

    /** Reads every entry of a zip, in the order it is stored. */
    static Map<String, byte[]> entries(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries.put(entry.getName(), in.readAllBytes());
            }
        }
        return entries;
    }
}
