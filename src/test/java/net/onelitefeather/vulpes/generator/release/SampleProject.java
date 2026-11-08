package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.JSON;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;

/**
 * The project captured from backend 3.3.0 (Cygnus), read from {@code src/test/resources/release/backend-3.3.0}.
 * <p>
 * The captures are single pages, so their {@code totalSize} is larger than their content; the fixture holds
 * the captured content only. Two captures are completed, because the captured project is otherwise not releasable:
 * <ul>
 *     <li>{@code how_to_play} has no component capture, so it gets the {@code stelaris:material} the release needs;</li>
 *     <li>{@code icons_icons_builder} has no glyph capture, so it gets one private-use glyph.</li>
 * </ul>
 * The captured component and lore records are attached to {@code statistics} and the glyph row to
 * {@code glitch_glitch_marker}, because the captures carry no parent id and their ids follow those parents.
 * <p>
 * The content export of the same project, which the release reads, is committed as
 * {@code release/content-export/cygnus-content.json} and is built from these captures with the same completions;
 * see {@link #export()}.
 *
 * @author TheMeinerLP
 */
final class SampleProject {

    static final UUID ID = UUID.fromString("01a193c1-fc5d-7546-8749-03c442235f29");

    private SampleProject() {
    }

    /**
     * Loads the completed sample project.
     *
     * @return a fresh fixture that the test may still change
     */
    static ProjectFixture load() {
        ProjectFixture fixture = new ProjectFixture(read("project.json"));
        fixture.items().addAll(content("items.json"));
        String statistics = idOf(fixture.items(), "statistics");
        String howToPlay = idOf(fixture.items(), "how_to_play");
        fixture.components(statistics).addAll(content("item-components.json"));
        fixture.lore(statistics).addAll(content("item-lore.json"));
        fixture.components(howToPlay).add(obj("id", ProjectFixture.id(900),
                "componentKey", "stelaris:material", "value", "minecraft:writable_book"));

        fixture.attributes().addAll(content("attributes.json"));
        fixture.fonts().addAll(content("fonts.json"));
        String glitch = idOf(fixture.fonts(), "glitch_glitch_marker");
        String icons = idOf(fixture.fonts(), "icons_icons_builder");
        fixture.glyphRows(glitch).addAll(content("font-chars.json"));
        fixture.glyphRows(icons).add(obj("id", ProjectFixture.id(901), "line", "\ue001", "orderIndex", 0));

        fixture.sounds().addAll(content("sounds.json"));
        String vhsStatic = idOf(fixture.sounds(), "vhs_static");
        fixture.sources(vhsStatic).addAll(content("sound-sources.json"));

        fixture.expressions().addAll(content("expressions.json"));
        fixture.assets().addAll(content("assets.json"));
        return fixture;
    }

    /**
     * Reads the committed content export of the sample project, as {@code GET /project/{id}/content} answers it.
     *
     * @return the export object
     */
    static JsonNode export() {
        try (InputStream in = SampleProject.class.getResourceAsStream("/release/content-export/cygnus-content.json")) {
            if (in == null) {
                throw new IllegalStateException("the sample export is missing from the test resources");
            }
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads one captured page and returns its content.
     *
     * @param name the file name inside {@code backend-3.3.0}
     * @return the entries of the page
     */
    static List<JsonNode> content(String name) {
        List<JsonNode> entries = new ArrayList<>();
        read(name).get("content").forEach(entries::add);
        return entries;
    }

    static JsonNode read(String name) {
        try (InputStream in = SampleProject.class.getResourceAsStream("/release/backend-3.3.0/" + name)) {
            if (in == null) {
                throw new IllegalStateException("sample " + name + " is missing from the test resources");
            }
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String idOf(List<JsonNode> entities, String key) {
        for (JsonNode entity : entities) {
            if (key.equals(entity.get("key").asText())) {
                return entity.get("id").asText();
            }
        }
        throw new IllegalStateException("sample has no entity with key " + key);
    }
}
