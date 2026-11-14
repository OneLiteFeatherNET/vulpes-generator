package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.id;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.json;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the content validation of an assembled snapshot. The snapshots are built in memory by the mapper, so every
 * test states exactly the content it checks. The project of the fixtures is {@code eldoria}.
 *
 * @author TheMeinerLP
 */
class ContentValidatorTest {

    private static final List<String> FOREIGN = List.of("minecraft", "olf");
    private static final String SHA1 = "3018dec4de6b2a4dedded228b5e716a17f64a507";

    @Test
    void validContentReportsNoProblem() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "statistics");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.assets().add(asset(id(4), "eldoria:font/glitch_marker", "TEXTURE"));
        fixture.fonts().add(font(id(3), "glitch_marker", "glitch", "eldoria:font/glitch_marker.png"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "", "orderIndex", 0));

        assertTrue(validate(fixture).isEmpty(), "content whose references all resolve must report no problem: " + validate(fixture));
    }

    @Test
    void missingFontFileIsReportedNamingTheFontAndTheFile() {
        ProjectFixture fixture = eldoria();
        fixture.fonts().add(font(id(3), "glitch_marker", "glitch", "eldoria:font/glitch_marker.png"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "", "orderIndex", 0));

        List<String> messages = messages(validate(fixture));

        assertEquals(1, messages.size(), "one missing font file must be one problem: " + messages);
        assertTrue(messages.get(0).startsWith("font eldoria:glitch_marker:"), "the problem must name the font: " + messages);
        assertTrue(messages.get(0).contains("eldoria:font/glitch_marker.png"), "the problem must name the file: " + messages);
    }

    @Test
    void missingSoundFileIsReportedOncePerFileNamingTheSound() {
        ProjectFixture fixture = eldoria();
        fixture.sounds().add(sound(id(5), "vhs_static"));
        fixture.sources(id(5)).add(source(id(6), "eldoria:vhs_static_1", "FILE"));
        fixture.sources(id(5)).add(source(id(7), "eldoria:vhs_static_2", "FILE"));
        fixture.sources(id(5)).add(source(id(8), "eldoria:vhs_static_3", "FILE"));

        List<String> messages = messages(validate(fixture));

        assertEquals(3, messages.size(), "three missing files must be three problems: " + messages);
        assertTrue(messages.get(0).startsWith("sound eldoria:vhs_static:"), "the problem must name the sound: " + messages);
        assertTrue(messages.get(0).contains("eldoria:vhs_static_1"), "the first problem must name its file: " + messages);
        assertTrue(messages.get(2).contains("eldoria:vhs_static_3"), "the last problem must name its file: " + messages);
    }

    @Test
    void soundEventEntryNeedsNoAssetFile() {
        ProjectFixture fixture = eldoria();
        fixture.sounds().add(sound(id(5), "vhs_static"));
        fixture.sources(id(5)).add(source(id(6), "minecraft:music.creative", "EVENT"));

        assertTrue(validate(fixture).isEmpty(), "an entry of type event names another sound and must not need a file");
    }

    @Test
    void sourceFileWithAnAssetEntryIsNotReported() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:ambient/drone", "SOUND"));
        fixture.sounds().add(sound(id(5), "drone"));
        fixture.sources(id(5)).add(source(id(6), "eldoria:ambient/drone", "FILE"));

        assertTrue(validate(fixture).isEmpty(), "a sound file that is an asset of the project must resolve");
    }

    @Test
    void itemModelNamingADefinitionReportsNoProblem() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "page_1");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.components(id(1)).add(component("minecraft:item_model", "\"eldoria:page_1\""));

        assertTrue(validate(fixture).isEmpty(), "an item model names a definition in assets/<ns>/items, which is not a project asset and must not be checked");
    }

    @Test
    void vanillaItemModelIsNotChecked() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "page");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.components(id(1)).add(component("minecraft:item_model", "\"minecraft:item/book\""));

        assertTrue(validate(fixture).isEmpty(), "a reference in the minecraft namespace is vanilla and must not be checked");
    }

    @Test
    void duplicateItemKeyIsReportedNamingTheKey() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "page");
        addItem(fixture, id(2), "page");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.components(id(2)).add(component("stelaris:material", "\"minecraft:book\""));

        List<String> messages = messages(validate(fixture));

        assertEquals(1, messages.size(), "one shared key must be one problem: " + messages);
        assertTrue(messages.get(0).contains("eldoria:page"), "the problem must name the shared key: " + messages);
    }

    @Test
    void invalidItemKeyIsReportedNamingTheItem() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "Cygnus:Page");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));

        List<String> messages = messages(validate(fixture));

        assertEquals(1, messages.size(), "one invalid key must be one problem: " + messages);
        assertTrue(messages.get(0).startsWith("item Cygnus:Page:"), "the problem must name the item: " + messages);
    }

    @Test
    void invalidSoundKeyIsReported() {
        ProjectFixture fixture = eldoria();
        fixture.sounds().add(sound(id(5), "VhsStatic"));

        List<String> messages = messages(validate(fixture));

        assertEquals(1, messages.size(), "one invalid sound key must be one problem: " + messages);
        assertTrue(messages.get(0).startsWith("sound eldoria:VhsStatic:"), "the problem must name the sound: " + messages);
    }

    @Test
    void duplicateAssetPathIsReported() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:ambient/drone", "SOUND"));
        fixture.assets().add(asset(id(5), "eldoria:ambient/drone", "SOUND"));

        List<String> messages = messages(validate(fixture));

        assertEquals(1, messages.size(), "one repeated path must be one problem: " + messages);
        assertTrue(messages.get(0).contains("assets/eldoria/sounds/ambient/drone.ogg"),
                "the problem must name the path: " + messages);
    }

    @Test
    void assetPathWithUppercaseLetterIsReportedNamingTheAsset() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:Font/Glitch", "TEXTURE"));

        List<String> messages = messages(validate(fixture));

        assertEquals(1, messages.size(), "one invalid asset path must be one problem: " + messages);
        assertTrue(messages.get(0).startsWith("asset assets/eldoria/textures/Font/Glitch.png:"),
                "the problem must name the asset path: " + messages);
    }

    @Test
    void unevenGlyphRowsAreReportedNamingTheFont() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:font/glitch_marker", "TEXTURE"));
        fixture.fonts().add(font(id(3), "glitch_marker", "glitch", "eldoria:font/glitch_marker.png"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "", "orderIndex", 0));
        fixture.glyphRows(id(3)).add(obj("id", id(31), "line", "", "orderIndex", 1));

        List<String> messages = messages(validate(fixture));

        assertEquals(1, messages.size(), "uneven rows must be one problem: " + messages);
        assertTrue(messages.get(0).startsWith("font eldoria:glitch_marker:"), "the problem must name the font: " + messages);
    }

    @Test
    void characterUsedTwiceUnderOneFontKeyIsReported() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:font/glitch_marker", "TEXTURE"));
        fixture.assets().add(asset(id(5), "eldoria:font/icons_builder", "TEXTURE"));
        fixture.fonts().add(font(id(3), "glitch_marker", "glyph", "eldoria:font/glitch_marker.png"));
        fixture.fonts().add(font(id(6), "icons_builder", "glyph", "eldoria:font/icons_builder.png"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "", "orderIndex", 0));
        fixture.glyphRows(id(6)).add(obj("id", id(60), "line", "", "orderIndex", 0));

        List<String> messages = messages(validate(fixture));

        assertEquals(1, messages.size(), "one character used twice must be one problem: " + messages);
        assertTrue(messages.get(0).contains("eldoria:icons_builder") && messages.get(0).contains("eldoria:glyph"),
                "the problem must name the font and its key: " + messages);
        assertTrue(messages.get(0).contains("U+E000"), "the problem must name the character: " + messages);
    }

    @Test
    void sameCharacterUnderDifferentFontKeysIsNotReported() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:font/glitch_marker", "TEXTURE"));
        fixture.assets().add(asset(id(5), "eldoria:font/icons_builder", "TEXTURE"));
        fixture.fonts().add(font(id(3), "glitch_marker", "glyph", "eldoria:font/glitch_marker.png"));
        fixture.fonts().add(font(id(6), "icons_builder", "icons", "eldoria:font/icons_builder.png"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "", "orderIndex", 0));
        fixture.glyphRows(id(6)).add(obj("id", id(60), "line", "", "orderIndex", 0));

        assertTrue(validate(fixture).isEmpty(), "a character may be used by fonts of different font keys");
    }

    @Test
    void severalProblemsAreReportedInOnePass() {
        ProjectFixture fixture = eldoria();
        fixture.fonts().add(font(id(3), "glitch_marker", "glitch", "eldoria:font/glitch_marker.png"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "", "orderIndex", 0));
        addItem(fixture, id(1), "page");
        addItem(fixture, id(2), "page");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.components(id(2)).add(component("stelaris:material", "\"minecraft:book\""));

        List<String> messages = messages(validate(fixture));

        assertEquals(2, messages.size(), "the missing font file and the shared key must both be reported: " + messages);
    }

    @Test
    void cygnusSampleReportsTheThreeMissingSoundFilesAndTheMissingIconTexture() {
        byte[] snapshot = new ProjectSnapshotMapper(FOREIGN).map(SampleProject.load().build()).bytes();

        List<String> messages = messages(ContentValidator.validate(snapshot));

        assertEquals(4, messages.size(), "the sample has three missing sound files and one missing font texture: " + messages);
        assertEquals(3, messages.stream().filter(m -> m.startsWith("sound cygnus:vhs_static:")).count(),
                "each missing sound file must be named under the sound: " + messages);
        assertTrue(messages.stream().anyMatch(m -> m.startsWith("font cygnus:icons_icons_builder:")),
                "the missing icon texture must be named under its font: " + messages);
    }

    private static List<ReleaseDiagnostic> validate(ProjectFixture fixture) {
        return ContentValidator.validate(new ProjectSnapshotMapper(FOREIGN).map(fixture.build()).bytes());
    }

    private static List<String> messages(List<ReleaseDiagnostic> diagnostics) {
        return diagnostics.stream().map(ReleaseDiagnostic::message).toList();
    }

    private static ProjectFixture eldoria() {
        return ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
    }

    private static void addItem(ProjectFixture fixture, String id, String key) {
        fixture.items().add(obj("id", id, "key", key, "uiName", key, "groupName", "meta"));
    }

    private static JsonNode component(String key, String valueJson) {
        return obj("id", id(900), "componentKey", key, "value", json(valueJson));
    }

    private static JsonNode sound(String id, String key) {
        return obj("id", id, "key", key, "uiName", key, "subTitle", null);
    }

    private static JsonNode source(String id, String name, String type) {
        return obj("id", id, "name", name, "volume", 1.0, "pitch", 1.0, "weight", 1, "stream", false,
                "attenuationDistance", 16, "preload", false, "type", type);
    }

    private static JsonNode font(String id, String key, String mapper, String texturePath) {
        return obj("id", id, "key", key, "uiName", key, "provider", "bitmap", "mapper", mapper,
                "texturePath", texturePath, "ascent", 7, "height", 8);
    }

    private static JsonNode asset(String id, String key, String kind) {
        return obj("id", id, "key", key, "kind", kind, "sha1", SHA1, "size", 88);
    }
}
