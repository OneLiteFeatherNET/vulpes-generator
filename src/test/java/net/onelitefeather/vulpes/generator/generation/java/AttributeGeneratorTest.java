package net.onelitefeather.vulpes.generator.generation.java;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.api.repository.AttributeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttributeGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path tempDir;

    @Test
    void rejectsTwoKeysWithSameJavaIdentifierNamingBothKeys() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(attribute("my-item"), attribute("my.item")),
                "Two keys that map to the same identifier must abort the generation");

        assertTrue(exception.getMessage().contains("my-item") && exception.getMessage().contains("my.item"),
                "The message must name both keys but was: " + exception.getMessage());
    }

    private void generate(AttributeEntity... attributes) {
        AttributeRepository repository = mock(AttributeRepository.class);
        when(repository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                .thenReturn(Page.of(List.of(attributes), Pageable.UNPAGED, (long) attributes.length));

        new AttributeGenerator(repository).generate(tempDir, PROJECT_ID);
    }

    private static AttributeEntity attribute(String key) {
        AttributeEntity entity = mock(AttributeEntity.class);
        when(entity.getKey()).thenReturn(key);
        when(entity.getVariableName()).thenReturn(key);
        when(entity.getDefaultValue()).thenReturn(10.0);
        when(entity.getMaximumValue()).thenReturn(100.0);
        return entity;
    }
}
