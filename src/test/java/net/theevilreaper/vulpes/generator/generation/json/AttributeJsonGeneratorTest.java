package net.theevilreaper.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.api.repository.AttributeRepository;
import net.theevilreaper.vulpes.generator.gson.GsonHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@MicronautTest(startApplication = false)
class AttributeJsonGeneratorTest {

    @TempDir
    Path tempDir;

    @Inject
    AttributeJsonGenerator generator;

    @Inject
    AttributeRepository repo;

    @MockBean(AttributeRepository.class)
    AttributeRepository mockRepo() {
        return Mockito.mock(AttributeRepository.class);
    }

    @BeforeEach
    void setUp() {
        AttributeEntity entity = Mockito.mock(AttributeEntity.class);
        when(entity.getVariableName()).thenReturn("health");
        when(entity.getDefaultValue()).thenReturn(10.0);
        when(entity.getMaximumValue()).thenReturn(100.0);

        when(repo.findAll()).thenReturn(List.of(entity));
    }

    @Test
    void testAttributeJsonGenerator() {
        generator.generate(tempDir);

        Path file = tempDir.resolve("attributes.json");

        assertTrue(Files.exists(file));
        JsonArray dataArray = null;
        try (var reader = Files.newBufferedReader(file, UTF_8)) {
            dataArray = GsonHolder.GSON.fromJson(reader, JsonArray.class);
        } catch (Exception exception) {
            fail("Unable to load notification file");
        }

        assertNotNull(dataArray, "The loaded array is null which is not right");
        assertFalse(dataArray.isEmpty());
        assertEquals(1, dataArray.size());

        JsonObject object = dataArray.get(0).getAsJsonObject();
        assertNotNull(object);

        assertEquals("health", object.get("variableName").getAsString());
        assertEquals("10.0", object.get("default").getAsString());
        assertEquals("100.0", object.get("maximum").getAsString());
        assertEquals("health", object.get("key").getAsString());
    }
}
