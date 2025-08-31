package net.theevilreaper.vulpes.generator.generation.json;

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.api.repository.AttributeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@MicronautTest
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
    void testAttributeJsonGenerator() throws Exception {
        generator.generate(tempDir);

        Path file = tempDir.resolve("attributes.json");

        assertTrue(Files.exists(file));

        String json = Files.readString(file);
        assertTrue(json.contains("health"));
        assertTrue(json.contains("10.0"));
        assertTrue(json.contains("100.0"));
    }
}
