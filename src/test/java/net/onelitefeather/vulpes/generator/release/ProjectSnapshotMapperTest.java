package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.id;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.json;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the mapping of backend entities to format 1. The mapper is pure, so every test builds its input in memory.
 * The project of the fixtures is {@code eldoria}, the namespace of its unqualified keys.
 *
 * @author TheMeinerLP
 */
class ProjectSnapshotMapperTest {

    private static final List<String> FOREIGN = List.of("minecraft", "olf");
    private static final String SHA1 = "3018dec4de6b2a4dedded228b5e716a17f64a507";

    @Test
    void snapshotOfProjectWithContentValidatesAgainstFormatOne() {
        byte[] snapshot = new ProjectSnapshotMapper(FOREIGN).map(SampleProject.load().build()).bytes();

        assertTrue(SnapshotSchema.violations(snapshot).isEmpty(),
                "the snapshot must validate against snapshot-1.schema.json, violations: " + SnapshotSchema.violations(snapshot));
    }

    @Test
    void snapshotIsFormatOneWithProjectIdAndNamespaceOfTheProjectKey() {
        JsonNode snapshot = map(eldoria());

        assertEquals(1, snapshot.get("format").asInt(), "format must be 1");
        assertEquals(PROJECT_ID.toString(), snapshot.get("projectId").asText(), "projectId must be the backend project id");
        assertEquals("eldoria", snapshot.get("namespace").asText(), "namespace must be the project key");
    }

    @Test
    void itemKeyWithoutNamespaceGetsTheProjectNamespace() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "statistics");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));

        assertEquals("eldoria:statistics", map(fixture).get("items").get(0).get("key").asText(),
                "an unqualified item key must get the project namespace");
    }

    @Test
    void soundKeyOfForeignNamespaceKeepsItsNamespace() {
        ProjectFixture fixture = eldoria();
        fixture.sounds().add(sound(id(1), "minecraft.music.game", "Music"));

        assertEquals("minecraft:music.game", map(fixture).get("sounds").get(0).get("key").asText(),
                "a key whose first segment is a foreign namespace must keep that namespace");
    }

    @Test
    void keyWithDotSegmentIsNotForeignWhenNotConfigured() {
        ProjectFixture fixture = eldoria();
        fixture.sounds().add(sound(id(1), "minecraft.music.game", "Music"));

        JsonNode snapshot = parse(new ProjectSnapshotMapper(List.of()).map(fixture.build()).bytes());

        assertEquals("eldoria:minecraft.music.game", snapshot.get("sounds").get(0).get("key").asText(),
                "without a configured foreign namespace the project namespace must be used");
    }

    @Test
    void itemMaterialComesFromStelarisMaterialAndAmountIsOne() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "statistics");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));

        JsonNode item = map(fixture).get("items").get(0);

        assertEquals("minecraft:book", item.get("material").asText(), "material must be the stelaris:material value");
        assertEquals(1, item.get("amount").asInt(), "amount must be 1");
        assertFalse(item.get("components").has("stelaris:material"), "stelaris:material must not stay a component");
    }

    @Test
    void itemLoreBecomesTextComponentsInOrderIndexOrder() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "statistics");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.lore(id(1)).add(obj("id", id(20), "text", "b", "orderIndex", 1));
        fixture.lore(id(1)).add(obj("id", id(21), "text", "a", "orderIndex", 0));

        JsonNode lore = map(fixture).get("items").get(0).get("components").get("minecraft:lore");

        assertEquals(json("[{\"text\":\"a\"},{\"text\":\"b\"}]"), lore,
                "lore lines must be text components in orderIndex order");
    }

    @Test
    void itemWithoutLoreHasNoLoreComponent() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "statistics");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));

        assertFalse(map(fixture).get("items").get(0).get("components").has("minecraft:lore"),
                "an item without lore must not get a minecraft:lore component");
    }

    @Test
    void otherComponentsAreKeptUnchanged() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "statistics");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.components(id(1)).add(component("minecraft:custom_name", "{\"text\":\"Statistics\",\"color\":\"gold\"}"));

        JsonNode components = map(fixture).get("items").get(0).get("components");

        assertEquals(json("{\"minecraft:custom_name\":{\"color\":\"gold\",\"text\":\"Statistics\"}}"), components,
                "every component except stelaris:material must be written unchanged");
    }

    @Test
    void itemWithoutMaterialIsRejectedWithUnsupportedContent() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "statistics");

        ReleaseRejectedException e = assertRejected(fixture);

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, e.getReason(),
                "an item without stelaris:material must be refused as unsupported content");
        assertTrue(e.getMessage().contains("eldoria:statistics"), "the message must name the item: " + e.getMessage());
    }

    @Test
    void itemWithLoreAndLoreComponentIsRejectedWithUnsupportedContent() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "statistics");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.components(id(1)).add(component("minecraft:lore", "[{\"text\":\"x\"}]"));
        fixture.lore(id(1)).add(obj("id", id(20), "text", "a", "orderIndex", 0));

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, assertRejected(fixture).getReason(),
                "lore must not silently replace a minecraft:lore component");
    }

    @Test
    void attributeDisplayNameIsUiNameAndTranslationKeyIsNullWhenAbsent() {
        ProjectFixture fixture = eldoria();
        fixture.attributes().add(obj("id", id(2), "uiName", "Adrenaline", "key", "adrenaline",
                "defaultValue", 0.2, "minimumValue", 0.0, "maximumValue", 1.0, "synced", false));

        JsonNode attribute = map(fixture).get("attributes").get(0);

        assertEquals("Adrenaline", attribute.get("displayName").asText(), "displayName must be the uiName");
        assertEquals("eldoria:adrenaline", attribute.get("key").asText(), "the key must get the project namespace");
        assertTrue(attribute.get("translationKey").isNull(), "a translation key that is not stored must be null");
    }

    @Test
    void bitmapFontSymbolHasNamespacedMapperAndGlyphsInOrderIndexOrder() {
        ProjectFixture fixture = eldoria();
        fixture.fonts().add(font(id(3), "glitch_marker", "glitch", "bitmap", "eldoria:font/glitch_marker.png"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "b", "orderIndex", 1));
        fixture.glyphRows(id(3)).add(obj("id", id(31), "line", "a", "orderIndex", 0));

        JsonNode symbol = map(fixture).get("fonts").get(0);

        assertEquals("eldoria:glitch", symbol.get("font").asText(), "font must be <namespace>:<mapper>");
        assertEquals("eldoria:font/glitch_marker.png", symbol.get("file").asText(), "file must be the texture path");
        assertEquals(json("[\"a\",\"b\"]"), symbol.get("chars"), "glyph rows must be in orderIndex order");
        assertEquals("bitmap", symbol.get("type").asText(), "type must be bitmap");
    }

    @Test
    void fontWithNonBitmapProviderIsRejectedWithUnsupportedContent() {
        ProjectFixture fixture = eldoria();
        fixture.fonts().add(font(id(3), "ttf_font", "ttf", "ttf", "eldoria:font/ttf.ttf"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "a", "orderIndex", 0));

        ReleaseRejectedException e = assertRejected(fixture);

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, e.getReason(),
                "a font provider other than bitmap must be refused as unsupported content");
        assertTrue(e.getMessage().contains("ttf"), "the message must name the provider: " + e.getMessage());
    }

    @Test
    void fontWithEmptyGlyphRowIsRejectedWithUnsupportedContent() {
        ProjectFixture fixture = eldoria();
        fixture.fonts().add(font(id(3), "glitch_marker", "glitch", "bitmap", "eldoria:font/glitch_marker.png"));
        fixture.glyphRows(id(3)).add(obj("id", id(30), "line", "", "orderIndex", 0));

        ReleaseRejectedException e = assertRejected(fixture);

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, e.getReason(),
                "an empty glyph row must be refused, because dropping it would shift the rows below");
        assertTrue(e.getMessage().contains("eldoria:glitch_marker"), "the message must name the font: " + e.getMessage());
    }

    @Test
    void fontWithoutGlyphRowsIsRejectedWithUnsupportedContent() {
        ProjectFixture fixture = eldoria();
        fixture.fonts().add(font(id(3), "glitch_marker", "glitch", "bitmap", "eldoria:font/glitch_marker.png"));

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, assertRejected(fixture).getReason(),
                "a font without glyph rows must be refused, the format needs at least one row");
    }

    @Test
    void textureAssetMapsToTexturesPathWithPngExtension() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:entity/equipment/empty", "TEXTURE", 88));

        JsonNode asset = map(fixture).get("assets").get(0);

        assertEquals("assets/eldoria/textures/entity/equipment/empty.png", asset.get("path").asText(),
                "a texture must be written under textures with a png extension");
        assertEquals("texture", asset.get("kind").asText(), "the kind must be lower case");
        assertEquals(16, asset.get("resolution").asInt(), "a texture needs an integer resolution, the pack resolution is 16");
        assertEquals(88, asset.get("size").asInt(), "the size must be the backend size");
        assertEquals(SHA1, asset.get("sha1").asText(), "the sha1 must be the backend sha1");
    }

    @Test
    void modelAssetMapsToModelsPathWithJsonExtensionAndNullResolution() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:item/sword", "MODEL", 120));

        JsonNode asset = map(fixture).get("assets").get(0);

        assertEquals("assets/eldoria/models/item/sword.json", asset.get("path").asText(),
                "a model must be written under models with a json extension");
        assertEquals("model", asset.get("kind").asText(), "the kind must be lower case");
        assertTrue(asset.get("resolution").isNull(), "a model has no texture resolution");
    }

    @Test
    void unsupportedAssetKindIsRejectedNamingItsKeys() {
        ProjectFixture fixture = eldoria();
        fixture.assets().add(asset(id(4), "eldoria:item/cube", "BBMODEL", 120));

        ReleaseRejectedException e = assertRejected(fixture);

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, e.getReason(),
                "an asset kind other than TEXTURE and MODEL must be refused");
        assertTrue(e.getMessage().contains("eldoria:item/cube"), "the message must name the asset key: " + e.getMessage());
    }

    @Test
    void blankSubtitleBecomesNull() {
        ProjectFixture fixture = eldoria();
        fixture.sounds().add(obj("id", id(5), "key", "vhs_static", "uiName", "vhs_static", "subTitle", "  "));

        assertTrue(map(fixture).get("sounds").get(0).get("subtitle").isNull(), "a blank subtitle must be null");
    }

    @Test
    void soundEventHasReplaceFalse() {
        ProjectFixture fixture = eldoria();
        fixture.sounds().add(sound(id(5), "vhs_static", "subtitles.vhs"));

        JsonNode sound = map(fixture).get("sounds").get(0);

        assertEquals("subtitles.vhs", sound.get("subtitle").asText(), "a stored subtitle must be kept");
        assertFalse(sound.get("replace").asBoolean(), "replace must be false, the backend cannot store it");
    }

    @Test
    void soundSourcesAreSortedByNameWithLowerCaseType() {
        ProjectFixture fixture = eldoria();
        fixture.sounds().add(sound(id(5), "vhs_static", "subtitles.vhs"));
        fixture.sources(id(5)).add(source(id(6), "eldoria:vhs_static_2", "FILE"));
        fixture.sources(id(5)).add(source(id(7), "eldoria:vhs_static_1", "FILE"));

        JsonNode sounds = map(fixture).get("sounds").get(0).get("sounds");

        assertEquals("eldoria:vhs_static_1", sounds.get(0).get("name").asText(), "sources must be sorted by name");
        assertEquals("file", sounds.get(0).get("type").asText(), "the source type must be lower case");
    }

    @Test
    void expressionUsesUiNameAndLowerCaseVariableTypesWithoutTestValue() {
        ProjectFixture fixture = eldoria();
        fixture.expressions().add(obj("id", id(8), "uiName", "pageProgress", "expression", "collected / total * 100",
                "comment", "Progress", "variables", json("[{\"name\":\"collected\",\"type\":\"NUMBER\",\"testValue\":\"3\"}]")));

        JsonNode expression = map(fixture).get("expressions").get(0);

        assertEquals("pageProgress", expression.get("name").asText(), "name must be the uiName");
        assertEquals("number", expression.get("returnType").asText(), "the return type must be number");
        assertEquals(json("[{\"name\":\"collected\",\"type\":\"number\"}]"), expression.get("variables"),
                "variables must carry name and lower case type, without the test value");
    }

    @Test
    void blankExpressionCommentBecomesNullDescription() {
        ProjectFixture fixture = eldoria();
        fixture.expressions().add(obj("id", id(8), "uiName", "pageProgress", "expression", "1 + 1",
                "comment", "   ", "variables", json("[]")));

        assertTrue(map(fixture).get("expressions").get(0).get("description").isNull(),
                "a blank comment must become a null description");
    }

    @Test
    void expressionFunctionMapsKindParametersAndTypesInLowerCase() {
        ProjectFixture fixture = eldoria();
        fixture.expressionFunctions().add(obj("id", id(9), "name", "dropRate", "kind", "FORMULA", "comment", "Drop",
                "parameters", json("[{\"name\":\"level\",\"type\":\"NUMBER\"}]"), "returnType", "NUMBER",
                "body", "level * 2"));

        JsonNode function = map(fixture).get("expressionFunctions").get(0);

        assertEquals("formula", function.get("kind").asText(), "FORMULA must be written as formula");
        assertEquals(json("[{\"name\":\"level\",\"type\":\"number\"}]"), function.get("parameters"),
                "parameters must carry lower case types in declaration order");
        assertEquals("number", function.get("returnType").asText(), "the return type must be lower case");
        assertEquals("level * 2", function.get("expression").asText(), "the expression must be the body");
    }

    @Test
    void advancementIsRejectedWithUnsupportedContentNamingIt() {
        ProjectFixture fixture = eldoria();
        fixture.advancements().add(obj("id", id(10), "key", "first_page"));

        ReleaseRejectedException e = assertRejected(fixture);

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, e.getReason(),
                "an advancement must be refused as unsupported content");
        assertTrue(e.getMessage().contains("first_page"), "the message must name the advancement: " + e.getMessage());
    }

    @Test
    void itemsAreSortedByKeyWhateverTheBackendOrder() {
        ProjectFixture fixture = eldoria();
        addItem(fixture, id(1), "zeta");
        addItem(fixture, id(2), "alpha");
        fixture.components(id(1)).add(component("stelaris:material", "\"minecraft:book\""));
        fixture.components(id(2)).add(component("stelaris:material", "\"minecraft:book\""));

        JsonNode items = map(fixture).get("items");

        assertEquals("eldoria:alpha", items.get(0).get("key").asText(), "items must be sorted by key");
        assertEquals("eldoria:zeta", items.get(1).get("key").asText(), "items must be sorted by key");
    }

    @Test
    void packDescriptionIsProjectDisplayNameAndResolutionIs16() {
        JsonNode pack = map(eldoria()).get("pack");

        assertEquals(json("{\"description\":{\"text\":\"Eldoria\"},\"resolutions\":[16]}"), pack,
                "the pack must carry the display name and the single resolution 16");
    }

    @Test
    void mappingTheSameInputTwiceYieldsIdenticalBytes() {
        ProjectSnapshotMapper mapper = new ProjectSnapshotMapper(FOREIGN);

        byte[] first = mapper.map(SampleProject.load().build()).bytes();
        byte[] second = mapper.map(SampleProject.load().build()).bytes();

        assertArrayEquals(first, second, "the same backend state must always yield the same bytes");
    }

    private static ProjectFixture eldoria() {
        return ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
    }

    private static JsonNode map(ProjectFixture fixture) {
        return parse(new ProjectSnapshotMapper(FOREIGN).map(fixture.build()).bytes());
    }

    private static ReleaseRejectedException assertRejected(ProjectFixture fixture) {
        return assertThrows(ReleaseRejectedException.class, () -> new ProjectSnapshotMapper(FOREIGN).map(fixture.build()));
    }

    private static JsonNode parse(byte[] snapshot) {
        return json(new String(snapshot, StandardCharsets.UTF_8));
    }

    private static void addItem(ProjectFixture fixture, String id, String key) {
        fixture.items().add(obj("id", id, "key", key, "uiName", key, "groupName", "meta"));
    }

    private static JsonNode component(String key, String valueJson) {
        return obj("id", id(900), "componentKey", key, "value", json(valueJson));
    }

    private static JsonNode sound(String id, String key, String subTitle) {
        return obj("id", id, "key", key, "uiName", key, "subTitle", subTitle);
    }

    private static JsonNode source(String id, String name, String type) {
        return obj("id", id, "name", name, "volume", 1.0, "pitch", 1.0, "weight", 1, "stream", false,
                "attenuationDistance", 16, "preload", false, "type", type);
    }

    private static JsonNode font(String id, String key, String mapper, String provider, String texturePath) {
        return obj("id", id, "key", key, "uiName", key, "provider", provider, "mapper", mapper,
                "texturePath", texturePath, "ascent", 7, "height", 8);
    }

    private static JsonNode asset(String id, String key, String kind, int size) {
        return obj("id", id, "key", key, "kind", kind, "sha1", SHA1, "size", size);
    }
}
