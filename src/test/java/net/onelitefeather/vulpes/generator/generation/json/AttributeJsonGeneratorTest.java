package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.api.model.project.ProjectEntity;
import net.onelitefeather.vulpes.api.repository.AttributeRepository;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttributeJsonGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID ENTITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final String PROJECT_KEY = "eldoria_rpg";
    private static final String FILE = "src/main/resources/attributes.json";

    @TempDir
    Path tempDir;

    @Test
    void writesNamespacedKeyAndUiNameOfAttribute() {
        generate(attribute("crit_chance", "Critical Hit Chance", 0.05, 1.0));

        JsonObject object = readAttributes().get(0).getAsJsonObject();
        assertEquals("eldoria_rpg:crit_chance", object.get("key").getAsString(),
                "the key must be the namespaced key, not the variable name");
        assertEquals("Critical Hit Chance", object.get("uiName").getAsString(),
                "the ui name must be written");
    }

    @Test
    void writesDefaultAndMaximumValues() {
        generate(attribute("crit_chance", "Critical Hit Chance", 0.05, 1.0));

        JsonObject object = readAttributes().get(0).getAsJsonObject();
        assertEquals(0.05, object.get("default").getAsDouble(), "the default value must be written");
        assertEquals(1.0, object.get("maximum").getAsDouble(), "the maximum value must be written");
    }

    @Test
    void sortsEntriesByNamespacedKey() {
        generate(
                attribute("zeta", "Zeta", 0.0, 1.0),
                attribute("alpha", "Alpha", 0.0, 1.0)
        );

        JsonArray attributes = readAttributes();
        assertEquals("eldoria_rpg:alpha", attributes.get(0).getAsJsonObject().get("key").getAsString(),
                "the entries must be sorted by key");
        assertEquals("eldoria_rpg:zeta", attributes.get(1).getAsJsonObject().get("key").getAsString(),
                "the entries must be sorted by key");
    }

    @Test
    void rejectsAttributeWithoutUiNameWithKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(attribute("crit_chance", null, 0.05, 1.0)),
                "an attribute without ui name must abort the generation");

        assertTrue(exception.getMessage().contains("crit_chance"),
                "the message must name the attribute key but was: " + exception.getMessage());
    }

    @Test
    void generatingTwiceProducesIdenticalFile() throws IOException {
        AttributeJsonGenerator generator = new AttributeJsonGenerator(repositoryWith(
                attribute("crit_chance", "Critical Hit Chance", 0.05, 1.0),
                attribute("mana_regen", "Mana Regeneration", 0.0, 10.0)));

        generator.generate(tempDir, PROJECT_ID);
        String first = Files.readString(tempDir.resolve(FILE), UTF_8);
        generator.generate(tempDir, PROJECT_ID);

        assertEquals(first, Files.readString(tempDir.resolve(FILE), UTF_8),
                "a second run into the same target must write the same file");
    }

    private void generate(AttributeEntity... attributes) {
        new AttributeJsonGenerator(repositoryWith(attributes)).generate(tempDir, PROJECT_ID);
    }

    private static AttributeRepository repositoryWith(AttributeEntity... attributes) {
        AttributeRepository repository = mock(AttributeRepository.class);
        when(repository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                .thenReturn(Page.of(List.of(attributes), Pageable.UNPAGED, (long) attributes.length));
        return repository;
    }

    private JsonArray readAttributes() {
        try (var reader = Files.newBufferedReader(tempDir.resolve(FILE), UTF_8)) {
            return GsonHolder.GSON.fromJson(reader, JsonArray.class);
        } catch (IOException e) {
            throw new AssertionError("attributes.json should have been written to " + FILE, e);
        }
    }

    private static AttributeEntity attribute(String key, String uiName, double defaultValue, double maximumValue) {
        ProjectEntity project = new ProjectEntity();
        project.setKey(PROJECT_KEY);
        return new AttributeEntity(ENTITY_ID, uiName, key, defaultValue, maximumValue, project);
    }
}
