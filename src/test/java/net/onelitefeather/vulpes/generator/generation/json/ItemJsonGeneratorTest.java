package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.ItemEntity;
import net.onelitefeather.vulpes.api.repository.ItemRepository;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@MicronautTest(startApplication = false)
class ItemJsonGeneratorTest {
    @TempDir
    Path tempDir;

    @Inject
    ItemJsonGenerator generator;

    @Inject
    ItemRepository repo;

    @MockBean(ItemRepository.class)
    ItemRepository mockRepo() {
        return Mockito.mock(ItemRepository.class);
    }

    @BeforeEach
    void setUp() {
        ItemEntity entity = Mockito.mock(ItemEntity.class);
        when(entity.getVariableName()).thenReturn("test_item");
        when(entity.getDisplayName()).thenReturn("Test Item");
        when(entity.getMaterial()).thenReturn(Material.BASALT.key().asString());
        when(entity.getGroupName()).thenReturn("test_group");
        when(entity.getCustomModelData()).thenReturn(10);
        when(entity.getAmount()).thenReturn(1);

        when(repo.findAll()).thenReturn(List.of(entity));
    }

    @Test
    void testItemJsonGenerator() {
        generator.generate(tempDir);

        Path file = tempDir.resolve("items.json");

        assertTrue(Files.exists(file));

        JsonArray dataArray = null;

        try (var reader = Files.newBufferedReader(file, UTF_8)) {
            dataArray = GsonHolder.GSON.fromJson(reader, JsonArray.class);
        } catch (Exception _) {
            fail("Unable to load notification file");
        }

        assertNotNull(dataArray, "The loaded array is null which is not right");
        assertFalse(dataArray.isEmpty());
        assertEquals(1, dataArray.size());

        JsonObject object = dataArray.get(0).getAsJsonObject();
        assertNotNull(object);

        assertEquals("test_item", object.get("variableName").getAsString());
        assertEquals("Test Item", object.get("displayName").getAsString());
        assertEquals("minecraft:basalt", object.get("material").getAsString());
        assertEquals("test_group", object.get("groupName").getAsString());
        assertEquals(10, object.get("customModelData").getAsInt());
        assertEquals(1, object.get("amount").getAsInt());
    }
}
