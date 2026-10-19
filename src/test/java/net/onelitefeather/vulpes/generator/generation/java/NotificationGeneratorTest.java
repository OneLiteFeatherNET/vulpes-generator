package net.onelitefeather.vulpes.generator.generation.java;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.NotificationEntity;
import net.onelitefeather.vulpes.api.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@MicronautTest(startApplication = false)
class NotificationGeneratorTest {

    @TempDir
    Path tempDir;

    @Inject
    NotificationGenerator generator;

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
        when(entity.getFrameType()).thenReturn(FrameType.CHALLENGE.name().toLowerCase(Locale.ROOT));
        when(entity.getMaterial()).thenReturn(Material.BASALT.key().asString());
        when(entity.getTitle()).thenReturn("My wonderful title");
        when(entity.getComment()).thenReturn("Some comment");

        when(repo.findByProjectId(any(UUID.class), any(Pageable.class)))
                .thenReturn(Page.of(List.of(entity), Pageable.UNPAGED, 1L));
    }

    @Test
    void generatedSourceContainsNoEscapedPlaceholderBackslash() throws IOException {
        generator.generate(tempDir, UUID.randomUUID());

        String source = readGeneratedJavaSource();

        assertFalse(source.contains("\\"),
                "Generated Java source must not contain a backslash (escaped JavaPoet placeholder), but was:\n" + source);
    }

    private String readGeneratedJavaSource() throws IOException {
        try (Stream<Path> files = Files.walk(tempDir)) {
            Path javaFile = files
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No Java file was generated in " + tempDir));
            assertTrue(Files.exists(javaFile), "Generated file must exist: " + javaFile);
            return Files.readString(javaFile, UTF_8);
        }
    }
}
