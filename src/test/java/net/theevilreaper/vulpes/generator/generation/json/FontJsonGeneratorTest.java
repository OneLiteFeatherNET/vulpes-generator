package net.theevilreaper.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.api.model.FontEntity;
import net.onelitefeather.vulpes.api.repository.FontRepository;
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

@MicronautTest
public class FontJsonGeneratorTest {

    @TempDir
    Path tempDir;

    @Inject
    FontJsonGenerator fontJsonGenerator;

    @Inject
    FontRepository fontRepository;

    @MockBean(FontRepository.class)
    FontRepository mockRepo() {
        return Mockito.mock(FontRepository.class);
    }

    @BeforeEach
    void setup() {
        FontEntity fontEntity = Mockito.mock(FontEntity.class);
        when(fontEntity.getVariableName()).thenReturn("h");
        when(fontEntity.getHeight()).thenReturn(15);
        when(fontEntity.getAscent()).thenReturn(20);

        when(fontRepository.findAll()).thenReturn(List.of(fontEntity));
    }

    @Test
    void testFontJsonGenerator() {
        fontJsonGenerator.generate(tempDir);

        Path file = tempDir.resolve("fonts.json");

        assertTrue(Files.exists(file));
        JsonArray dataArray = null;
        try (var reader = Files.newBufferedReader(file, UTF_8)){
            dataArray = GsonHolder.GSON.fromJson(reader, JsonArray.class);
        } catch (Exception e) {
            fail("Unable to read file");
        }

        assertNotNull(dataArray, "The loaded array is null which is not right");
        assertFalse(dataArray.isEmpty());
        assertEquals(1, dataArray.size());

        JsonObject object = dataArray.get(0).getAsJsonObject();
        assertNotNull(object);

        assertEquals("h", object.get("variableName").getAsString());
        assertEquals(15, object.get("height").getAsInt());
        assertEquals(20, object.get("ascent").getAsInt());
    }

}
