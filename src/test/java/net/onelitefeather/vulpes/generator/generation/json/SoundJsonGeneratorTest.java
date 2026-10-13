package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import net.onelitefeather.vulpes.api.model.project.ProjectEntity;
import net.onelitefeather.vulpes.api.model.sound.SoundEventEntity;
import net.onelitefeather.vulpes.api.model.sound.SoundFileSource;
import net.onelitefeather.vulpes.api.repository.SoundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SoundJsonGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path tempDir;

    @Test
    void writesOnlyNonDefaultFieldsOfEntry() {
        generate(event("ui_click", "eldoria:ui.click", null,
                source("eldoria/click", 0.5f, 1f, 3, false, 16, false, "file")));

        JsonObject entry = readEvent("eldoria", "ui.click").getAsJsonArray("sounds").get(0).getAsJsonObject();

        assertEquals(Set.of("name", "volume", "weight"), entry.keySet(),
                "Only the name and the non-default fields must be written but was " + entry);
    }

    @Test
    void writesEntryWithOnlyDefaultValuesAsPlainName() {
        generate(event("ui_click", "eldoria:ui.click", null, defaultSource("eldoria/click")));

        JsonArray sounds = readEvent("eldoria", "ui.click").getAsJsonArray("sounds");

        assertTrue(sounds.get(0).isJsonPrimitive(), "An entry with only default values must be written as a plain string");
        assertEquals("eldoria/click", sounds.get(0).getAsString(), "The plain string must be the sound name");
    }

    @Test
    void normalizesLegacyTypeFileToFile() {
        generate(event("ui_click", "eldoria:ui.click", null,
                source("eldoria/click", 1f, 1f, 1, false, 16, false, "type_file")));

        JsonArray sounds = readEvent("eldoria", "ui.click").getAsJsonArray("sounds");

        assertTrue(sounds.get(0).isJsonPrimitive(), "The legacy type_file must be treated as the default file type and omitted");
    }

    @Test
    void writesEventTypeForEventSource() {
        generate(event("ui_click", "eldoria:ui.click", null,
                source("minecraft:ui.button.click", 1f, 1f, 1, false, 16, false, "event")));

        JsonObject entry = readEvent("eldoria", "ui.click").getAsJsonArray("sounds").get(0).getAsJsonObject();

        assertEquals("event", entry.get("type").getAsString(), "A source of type event must be written with type event");
    }

    @Test
    void rejectsInvalidVolumeWithSoundKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(event("ui_click", "eldoria:ui.click", null,
                        source("eldoria/click", 0f, 1f, 1, false, 16, false, "file"))),
                "A volume of 0 must abort the generation");

        assertTrue(exception.getMessage().contains("eldoria:ui.click"),
                "The message should name the sound key but was: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("volume"),
                "The message should name the invalid field but was: " + exception.getMessage());
    }

    @Test
    void groupsEventsByNamespaceIntoSeparateFiles() {
        generate(
                event("ui_click", "eldoria:ui.click", null, defaultSource("eldoria/click")),
                event("game_start", "minigames:game.start", null, defaultSource("minigames/start"))
        );

        assertTrue(readNamespace("eldoria").has("ui.click"), "The eldoria namespace must contain its own event");
        assertFalse(readNamespace("eldoria").has("game.start"), "Events of another namespace must not be written to eldoria");
        assertTrue(readNamespace("minigames").has("game.start"), "The minigames namespace must contain its own event");
        assertFalse(readNamespace("minigames").has("ui.click"), "Events of another namespace must not be written to minigames");
    }

    @Test
    void sortsEventsByPath() {
        generate(
                event("zeta", "eldoria:zeta.sound", null, defaultSource("eldoria/zeta")),
                event("alpha", "eldoria:alpha.sound", null, defaultSource("eldoria/alpha"))
        );

        String json = readRaw("eldoria");

        assertTrue(json.indexOf("alpha.sound") < json.indexOf("zeta.sound"),
                "Events must be written sorted by their path but was:\n" + json);
    }

    @Test
    void writesSubtitleOfEvent() {
        generate(event("ui_click", "eldoria:ui.click", "Klick", defaultSource("eldoria/click")));

        assertEquals("Klick", readEvent("eldoria", "ui.click").get("subtitle").getAsString(),
                "The subtitle of the event must be written as subtitle");
    }

    @Test
    void omitsSubtitleWhenAbsent() {
        generate(event("ui_click", "eldoria:ui.click", null, defaultSource("eldoria/click")));

        assertFalse(readEvent("eldoria", "ui.click").has("subtitle"), "An event without subtitle must not write the subtitle field");
    }

    @Test
    void usesProjectKeyAsNamespaceWhenKeyNameHasNone() {
        generate(event("ui_click", "ui.click", null, defaultSource("eldoria/click")));

        assertTrue(readNamespace("eldoria").has("ui.click"),
                "A key name without namespace must be written under the project key namespace");
    }

    @Test
    void writesNothingWithoutEvents() {
        generate();

        assertFalse(Files.exists(tempDir.resolve("src/main/resources/assets")), "Without events no assets folder must be created");
    }

    @Test
    void rejectsDuplicateEventKeys() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> generate(
                event("ui_click", "eldoria:ui.click", null, defaultSource("eldoria/click")),
                event("ui_click_copy", "eldoria:ui.click", null, defaultSource("eldoria/other"))),
                "Two events with the same key must abort the generation");

        assertTrue(exception.getMessage().contains("eldoria:ui.click"),
                "The message should name the duplicated key but was: " + exception.getMessage());
    }

    @Test
    void generationRunsInReadOnlyTransaction() throws NoSuchMethodException {
        boolean readOnly = SoundJsonGenerator.class.getMethod("generate", Path.class, UUID.class).isAnnotationPresent(ReadOnly.class);

        assertTrue(readOnly, "generate must be annotated with @ReadOnly so lazy sound data can be loaded");
    }

    private void generate(SoundEventEntity... events) {
        SoundRepository repository = mock(SoundRepository.class);
        when(repository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                .thenReturn(Page.of(List.of(events), Pageable.UNPAGED, (long) events.length));

        new SoundJsonGenerator(repository).generate(tempDir, PROJECT_ID);
    }

    private String readRaw(String namespace) {
        try {
            return Files.readString(tempDir.resolve("src/main/resources/assets/" + namespace + "/sounds.json"), UTF_8);
        } catch (IOException e) {
            throw new AssertionError("The sounds.json of namespace '" + namespace + "' should have been generated", e);
        }
    }

    private JsonObject readNamespace(String namespace) {
        return JsonParser.parseString(readRaw(namespace)).getAsJsonObject();
    }

    private JsonObject readEvent(String namespace, String path) {
        JsonObject namespaceObject = readNamespace(namespace);
        assertTrue(namespaceObject.has(path), "The event '" + path + "' should be written but the file contained:\n" + namespaceObject);
        return namespaceObject.getAsJsonObject(path);
    }

    private static SoundEventEntity event(String key, String keyName, String subTitle, SoundFileSource... sources) {
        ProjectEntity project = new ProjectEntity();
        project.setKey("eldoria");
        return new SoundEventEntity(
                UUID.nameUUIDFromBytes(key.getBytes(UTF_8)),
                key,
                key,
                keyName,
                false,
                subTitle,
                null,
                List.of(sources),
                project
        );
    }

    private static SoundFileSource defaultSource(String name) {
        return source(name, 1f, 1f, 1, false, 16, false, "file");
    }

    private static SoundFileSource source(String name, float volume, float pitch, int weight,
                                          boolean stream, int attenuationDistance, boolean preload, String type) {
        return new SoundFileSource(UUID.nameUUIDFromBytes(name.getBytes(UTF_8)), name, volume, pitch, weight,
                stream, attenuationDistance, preload, type);
    }
}
