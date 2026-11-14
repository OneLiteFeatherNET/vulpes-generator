package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.PackTestData.CONTENTS;
import static net.onelitefeather.vulpes.generator.release.PackTestData.ICON;
import static net.onelitefeather.vulpes.generator.release.PackTestData.SNAPSHOT;
import static net.onelitefeather.vulpes.generator.release.PackTestData.SOUND_ID;
import static net.onelitefeather.vulpes.generator.release.PackTestData.SOUND_PATH;
import static net.onelitefeather.vulpes.generator.release.PackTestData.TEXTURE;
import static net.onelitefeather.vulpes.generator.release.PackTestData.TEXTURE_ID;
import static net.onelitefeather.vulpes.generator.release.PackTestData.TEXTURE_PATH;
import static net.onelitefeather.vulpes.generator.release.PackTestData.asset;
import static net.onelitefeather.vulpes.generator.release.PackTestData.entries;
import static net.onelitefeather.vulpes.generator.release.PackTestData.inputs;
import static net.onelitefeather.vulpes.generator.release.PackTestData.meta;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.json;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the bytes of the resource pack ZIP that {@link ResourcePackBuilder} writes. The builder is pure: the snapshot,
 * the pack inputs and the asset bytes are all in memory.
 *
 * @author TheMeinerLP
 */
class ResourcePackBuilderTest {

    @Test
    void projectWithFontAndSoundHoldsTheEntriesOfTheScenario() throws IOException {
        Map<String, byte[]> entries = entries(build(inputs(meta("Cygnus")), SNAPSHOT));

        assertEquals(Set.of("pack.mcmeta", TEXTURE_PATH, "assets/cygnus/font/glitch.json", SOUND_PATH,
                        "assets/cygnus/sounds.json", "pack.png"), entries.keySet(),
                "the pack must hold the meta, the assets, the icon and the font and sound JSON of the project");
    }

    @Test
    void fontJsonHoldsOneBitmapProviderOfTheFont() throws IOException {
        JsonNode font = json(text(entries(build(inputs(meta("Cygnus")), SNAPSHOT)).get("assets/cygnus/font/glitch.json")));

        JsonNode provider = font.get("providers").get(0);
        assertEquals("bitmap", provider.get("type").asText(), "the provider must be a bitmap provider");
        assertEquals("cygnus:font/glitch.png", provider.get("file").asText(), "the provider must name the texture");
        assertEquals(7, provider.get("ascent").asInt(), "the ascent must be the one of the font");
        assertTrue(provider.get("height") == null,
                "a height equal to the vanilla default 8 is left out of the provider, as the codec writes it");
        assertEquals("", provider.get("chars").get(0).asText(), "the chars must be the glyph rows of the font");
    }

    @Test
    void soundsJsonNamesTheEventWithItsSourceFile() throws IOException {
        JsonNode sounds = json(text(entries(build(inputs(meta("Cygnus")), SNAPSHOT)).get("assets/cygnus/sounds.json")));

        assertEquals("cygnus:ambient/drone", sounds.get("ambient.drone").get("sounds").get(0).asText(),
                "the event must play the sound file of its source, written as a plain name when all values are the defaults");
    }

    @Test
    void packMetaCarriesTheDescriptionAndTheFormats() throws IOException {
        JsonNode pack = json(text(entries(build(inputs(meta("Cygnus")), SNAPSHOT)).get("pack.mcmeta"))).get("pack");

        assertEquals("Cygnus", pack.get("description").get("text").asText(), "the description must be the one of the settings");
        assertEquals(46, pack.get("min_format").asInt(), "min_format must be the minimum format");
        assertEquals(46, pack.get("max_format").asInt(), "max_format must be the maximum format");
    }

    @Test
    void formatWithMinorVersionIsWrittenAsPair() throws IOException {
        JsonNode meta = obj("id", "00000000-0000-4000-8000-0000000000b1", "packType", "RESOURCE",
                "description", obj("text", "Cygnus"), "minFormatMajor", 46, "minFormatMinor", 1,
                "maxFormatMajor", 46, "maxFormatMinor", 2, "enabledFeatures", json("[]"), "iconAssetId", null);

        JsonNode pack = json(text(entries(build(new ResourcePackInputs(meta, List.of()), SNAPSHOT)).get("pack.mcmeta")))
                .get("pack");

        assertEquals(List.of(46, 1), List.of(pack.get("min_format").get(0).asInt(), pack.get("min_format").get(1).asInt()),
                "a minimum format with a minor version must be written as [major, minor]");
        assertEquals(List.of(46, 2), List.of(pack.get("max_format").get(0).asInt(), pack.get("max_format").get(1).asInt()),
                "a maximum format with a minor version must be written as [major, minor]");
    }

    @Test
    void iconIsWrittenAsPackPng() throws IOException {
        Map<String, byte[]> entries = entries(build(inputs(meta("Cygnus")), SNAPSHOT));

        assertArrayEquals(ICON, entries.get("pack.png"), "pack.png must be the bytes of the icon asset");
        assertArrayEquals(TEXTURE, entries.get(TEXTURE_PATH), "a texture must be written with its own bytes");
    }

    @Test
    void enabledFeaturesAreListedUnderFeaturesEnabled() throws IOException {
        JsonNode meta = obj("id", "00000000-0000-4000-8000-0000000000b1", "packType", "RESOURCE",
                "description", obj("text", "Cygnus"), "minFormatMajor", 46, "minFormatMinor", 0,
                "maxFormatMajor", 46, "maxFormatMinor", null, "enabledFeatures", json("[\"minecraft:wrapper\"]"),
                "iconAssetId", null);

        JsonNode mcmeta = json(text(entries(build(new ResourcePackInputs(meta, List.of()), SNAPSHOT)).get("pack.mcmeta")));

        assertEquals("minecraft:wrapper", mcmeta.get("features").get("enabled").get(0).asText(),
                "the enabled features must be listed under features.enabled");
    }

    @Test
    void sameContentGivesTheSameBytes() {
        ResourcePackInputs inputs = inputs(meta("Cygnus"));

        assertArrayEquals(build(inputs, SNAPSHOT), build(inputs, SNAPSHOT),
                "two builds of the same content must give the same bytes");
    }

    @Test
    void entriesAreSortedByPathAndHaveTheFixedTimestamp() throws IOException {
        List<String> names = new ArrayList<>();
        List<LocalDateTime> times = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(build(inputs(meta("Cygnus")), SNAPSHOT)))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
                times.add(entry.getTimeLocal());
            }
        }

        assertEquals(names.stream().sorted().toList(), names, "the entries must be sorted by path");
        assertTrue(times.stream().allMatch(LocalDateTime.of(1980, 1, 1, 0, 0)::equals),
                "every entry must carry the fixed timestamp 1980-01-01T00:00, not the system clock: " + times);
    }

    @Test
    void projectWithoutAssetsHoldsOnlyThePackMeta() throws IOException {
        String empty = "{\"format\":1,\"namespace\":\"cygnus\",\"fonts\":[],\"sounds\":[]}";

        Map<String, byte[]> entries = entries(build(new ResourcePackInputs(meta("Cygnus"), List.of()), empty));

        assertEquals(List.of("pack.mcmeta"), List.copyOf(entries.keySet()),
                "a project without assets, fonts or sounds must give a pack with only pack.mcmeta");
    }

    @Test
    void twoFilesAtOnePathAreRefused() {
        ResourcePackInputs clash = new ResourcePackInputs(meta("Cygnus"), List.of(
                asset(TEXTURE_PATH, TEXTURE_ID, TEXTURE),
                asset(TEXTURE_PATH, SOUND_ID, PackTestData.SOUND)));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> build(clash, SNAPSHOT));

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, e.getReason(),
                "a path written twice must refuse the release instead of overwriting one file");
    }

    private static byte[] build(ResourcePackInputs inputs, String snapshot) {
        return ResourcePackBuilder.build(snapshot.getBytes(StandardCharsets.UTF_8), inputs, CONTENTS);
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
