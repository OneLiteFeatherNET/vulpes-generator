package net.theevilreaper.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.NotificationEntity;
import net.onelitefeather.vulpes.api.repository.NotificationRepository;
import net.theevilreaper.vulpes.generator.gson.GsonHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@MicronautTest(startApplication = false)
class NotificationJsonGeneratorTest {

    @TempDir
    Path tempDir;

    @Inject
    NotificationJsonGenerator generator;

    @Inject
    NotificationRepository repo;

    @MockBean(NotificationRepository.class)
    NotificationRepository mockRepo() {
        return Mockito.mock(NotificationRepository.class);
    }

    @BeforeEach
    void setUp() {
        NotificationEntity entity = Mockito.mock(NotificationEntity.class);
        when(entity.getVariableName()).thenReturn("test_notification");
        when(entity.getFrameType()).thenReturn(FrameType.CHALLENGE.name().toLowerCase(Locale.getDefault()));
        when(entity.getMaterial()).thenReturn(Material.BASALT.key().asString());
        when(entity.getTitle()).thenReturn("My wonderful title");

        when(repo.findAll()).thenReturn(List.of(entity));
    }

    @Test
    void testNotificationJsonGenerator() {
        generator.generate(tempDir);

        Path file = tempDir.resolve("notifications.json");

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

        assertEquals("test_notification", object.get("variableName").getAsString());
        assertEquals("challenge", object.get("frameType").getAsString());
        assertEquals("minecraft:basalt", object.get("material").getAsString());
        assertEquals("My wonderful title", object.get("title").getAsString());
    }
}
