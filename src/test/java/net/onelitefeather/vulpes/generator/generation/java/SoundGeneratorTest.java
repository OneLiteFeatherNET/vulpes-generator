package net.onelitefeather.vulpes.generator.generation.java;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import net.onelitefeather.vulpes.api.model.project.ProjectEntity;
import net.onelitefeather.vulpes.api.model.sound.SoundEventEntity;
import net.onelitefeather.vulpes.api.repository.SoundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SoundGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final String REGISTRY_PATH = "net/reaper/vulpes/sound/SoundRegistry.java";

    @TempDir
    Path tempDir;

    @Test
    void generatesFieldWithCustomSoundEventFactoryAndKey() {
        generate(event("ui_click", "eldoria:ui.click"));

        assertTrue(readRegistry().contains("UI_CLICK = SoundEvent.of(\"eldoria:ui.click\", null)"),
                "Each event should be a field created with the custom SoundEvent factory and its key but was:\n" + readRegistry());
    }

    @Test
    void sortsFieldsByVariableName() {
        generate(
                event("zeta", "eldoria:zeta.sound"),
                event("alpha", "eldoria:alpha.sound"),
                event("mid", "eldoria:mid.sound")
        );

        String code = readRegistry();
        int alpha = code.indexOf("ALPHA =");
        int mid = code.indexOf("MID =");
        int zeta = code.indexOf("ZETA =");
        assertTrue(alpha >= 0 && mid >= 0 && zeta >= 0, "All three fields should be generated but was:\n" + code);
        assertTrue(alpha < mid && mid < zeta, "The fields should be sorted by variable name but was:\n" + code);
    }

    @Test
    void generatedSourceContainsNoEscapedPlaceholder() {
        generate(event("ui_click", "eldoria:ui.click"), event("game_start", "minigames:game.start"));

        String code = readRegistry();
        assertFalse(code.contains("\\$"), "The source must not contain a backslash in front of a placeholder but was:\n" + code);
    }

    @Test
    void generatedRegistryCompiles() throws IOException {
        generate(event("ui_click", "eldoria:ui.click"), event("game_start", "minigames:game.start"));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            exitCode = compiler.run(null, OutputStream.nullOutputStream(), err,
                    "-proc:none",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classesDir.toString(),
                    tempDir.resolve(REGISTRY_PATH).toString());
        }

        assertEquals(0, exitCode, "The generated SoundRegistry must compile but the compiler reported:\n" + errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsInvalidKeyWithKeyNameInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(event("broken", "eldoria:Ui Click")),
                "An invalid sound key must abort the generation");

        assertTrue(exception.getMessage().contains("eldoria:Ui Click"),
                "The message should name the invalid key but was: " + exception.getMessage());
    }

    @Test
    void generationRunsInReadOnlyTransaction() throws NoSuchMethodException {
        boolean readOnly = SoundGenerator.class.getMethod("generate", Path.class, UUID.class).isAnnotationPresent(ReadOnly.class);

        assertTrue(readOnly, "generate must be annotated with @ReadOnly so lazy sound data can be loaded");
    }

    private void generate(SoundEventEntity... events) {
        SoundRepository repository = mock(SoundRepository.class);
        when(repository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                .thenReturn(Page.of(List.of(events), Pageable.UNPAGED, (long) events.length));

        new SoundGenerator(repository).generate(tempDir, PROJECT_ID);
    }

    private String readRegistry() {
        try {
            return Files.readString(tempDir.resolve(REGISTRY_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("The SoundRegistry should have been generated at " + REGISTRY_PATH, e);
        }
    }

    private static SoundEventEntity event(String key, String keyName) {
        ProjectEntity project = new ProjectEntity();
        project.setKey("eldoria");
        return new SoundEventEntity(
                UUID.nameUUIDFromBytes(key.getBytes(UTF_8)),
                key,
                key,
                keyName,
                false,
                null,
                null,
                List.of(),
                project
        );
    }
}
