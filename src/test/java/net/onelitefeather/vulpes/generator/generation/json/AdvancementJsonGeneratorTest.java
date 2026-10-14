package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import net.onelitefeather.vulpes.api.model.advancement.AdvancementEntity;
import net.onelitefeather.vulpes.api.model.advancement.AdvancementFrameType;
import net.onelitefeather.vulpes.api.model.project.ProjectEntity;
import net.onelitefeather.vulpes.api.repository.AdvancementRepository;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdvancementJsonGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @TempDir
    Path tempDir;

    private ProjectEntity project;

    @BeforeEach
    void setUp() {
        project = new ProjectEntity();
        project.setKey("vulpes");
    }

    @Test
    void omitsParentKeyForRoot() {
        JsonArray array = generate(advancement("root", null));

        assertFalse(array.get(0).getAsJsonObject().has("parent"), "a root must not have a parent key");
    }

    @Test
    void writesParentAsNamespacedKeyOfChild() {
        AdvancementEntity root = advancement("root", null);
        JsonArray array = generate(root, advancement("child", root));

        JsonObject child = find(array, "vulpes:child");
        assertEquals("vulpes:root", child.get("parent").getAsString(), "the parent must be written as the namespaced key of the root");
    }

    @Test
    void ordersParentBeforeChildEvenWhenInputIsReversed() {
        AdvancementEntity root = advancement("root", null);
        AdvancementEntity child = advancement("child", root);

        JsonArray array = generate(child, root);

        assertEquals("vulpes:root", array.get(0).getAsJsonObject().get("key").getAsString(),
                "the root must come before its child in the output");
        assertEquals("vulpes:child", array.get(1).getAsJsonObject().get("key").getAsString(),
                "the child must come after its root in the output");
    }

    @Test
    void writesLegacyStoneMaterialAsNamespacedIcon() {
        AdvancementEntity root = advancement("root", null);
        root.setMaterial("STONE");

        JsonObject object = generateSingle(root);

        assertEquals("minecraft:stone", object.get("icon").getAsString(), "the legacy material STONE must be written as minecraft:stone");
    }

    @Test
    void writesPaperIconWhenMaterialIsMissing() {
        AdvancementEntity root = advancement("root", null);
        root.setMaterial(null);

        JsonObject object = generateSingle(root);

        assertEquals("minecraft:paper", object.get("icon").getAsString(), "a missing material must fall back to minecraft:paper");
    }

    @Test
    void writesTitleAsParsedJsonObject() {
        AdvancementEntity root = advancement("root", null);
        root.setTitle("{\"text\":\"Titel\"}");

        JsonObject object = generateSingle(root);

        assertTrue(object.get("title").isJsonObject(), "the title must be the parsed JSON object and not its raw text");
        assertEquals("Titel", object.getAsJsonObject("title").get("text").getAsString(), "the parsed title must keep its fields");
    }

    @Test
    void writesFrameTypeInLowerCase() {
        AdvancementEntity root = advancement("root", null);
        root.setFrameType(AdvancementFrameType.GOAL);

        JsonObject object = generateSingle(root);

        assertEquals("goal", object.get("frameType").getAsString(), "the frame type must be written in lower case");
    }

    @Test
    void invalidTitleAbortsWithKeyInMessage() {
        AdvancementEntity broken = advancement("broken_title", null);
        broken.setTitle("{not json");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(broken),
                "an invalid title must abort the generation");

        assertTrue(exception.getMessage().contains("broken_title"),
                "the message should name the advancement key but was: " + exception.getMessage());
    }

    @Test
    void generatingTwiceIntoSameDirectoryOverwritesFileWithoutFailing() throws IOException {
        AdvancementRepository repository = mock(AdvancementRepository.class);
        AdvancementEntity root = advancement("root", null);
        when(repository.findByProjectId(any(UUID.class), any(Pageable.class)))
                .thenReturn(Page.of(List.of(root), Pageable.UNPAGED, 1L));
        AdvancementJsonGenerator generator = new AdvancementJsonGenerator(repository);

        generator.generate(tempDir, PROJECT_ID);
        generator.generate(tempDir, PROJECT_ID);

        Path file = tempDir.resolve("src/main/resources/advancements.json");
        JsonArray written = GsonHolder.GSON.fromJson(Files.readString(file, UTF_8), JsonArray.class);
        assertEquals(1, written.size(), "A second run must replace advancements.json instead of appending to it");
    }

    private JsonObject generateSingle(AdvancementEntity advancement) {
        JsonArray array = generate(advancement);
        assertEquals(1, array.size(), "exactly one advancement must be written");
        return array.get(0).getAsJsonObject();
    }

    private JsonArray generate(AdvancementEntity... advancements) {
        AdvancementRepository repository = mock(AdvancementRepository.class);
        when(repository.findByProjectId(any(UUID.class), any(Pageable.class)))
                .thenReturn(Page.of(List.of(advancements), Pageable.UNPAGED, (long) advancements.length));

        new AdvancementJsonGenerator(repository).generate(tempDir, PROJECT_ID);

        Path file = tempDir.resolve("src/main/resources/advancements.json");
        assertTrue(Files.exists(file), "advancements.json must be created in the target directory");
        try (var reader = Files.newBufferedReader(file, UTF_8)) {
            return GsonHolder.GSON.fromJson(reader, JsonArray.class);
        } catch (Exception e) {
            throw new AssertionError("advancements.json must be valid JSON", e);
        }
    }

    private static JsonObject find(JsonArray array, String key) {
        for (var element : array) {
            JsonObject object = element.getAsJsonObject();
            if (key.equals(object.get("key").getAsString())) {
                return object;
            }
        }
        throw new AssertionError("no advancement with key " + key + " was written");
    }

    /**
     * Creates an advancement with a fixed id derived from its key, so every test run sees the same data.
     */
    private AdvancementEntity advancement(String key, AdvancementEntity parent) {
        AdvancementEntity entity = new AdvancementEntity(
                UUID.nameUUIDFromBytes(key.getBytes(UTF_8)),
                "UI " + key,
                key,
                null,
                "minecraft:stone",
                AdvancementFrameType.TASK,
                "{\"text\":\"" + key + "\"}",
                null,
                parent,
                project
        );
        entity.setX(1.5f);
        entity.setY(-0.5f);
        entity.setShowToast(true);
        entity.setAnnounceToChat(false);
        entity.setHidden(false);
        return entity;
    }
}
