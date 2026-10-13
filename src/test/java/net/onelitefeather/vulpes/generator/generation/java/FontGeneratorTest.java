package net.onelitefeather.vulpes.generator.generation.java;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import net.onelitefeather.vulpes.api.model.FontEntity;
import net.onelitefeather.vulpes.api.model.font.FontStringEntity;
import net.onelitefeather.vulpes.api.model.project.ProjectEntity;
import net.onelitefeather.vulpes.api.repository.FontRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FontGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final String REGISTRY_PATH = "net/reaper/vulpes/font/FontRegistry.java";
    private static final String HUD_FILE = "eldoria:font/hud_icons.png";

    @TempDir
    Path tempDir;

    @Test
    void emitsKeyOfFontFileFromNamespaceAndMapper() {
        generate(font("hud", "font", HUD_FILE, 7, 8, rows("ab")));

        assertTrue(readRegistry().contains("HUD_FONT = Key.key(\"eldoria\", \"font\")"),
                "the font file key must be namespace:mapper but was:\n" + readRegistry());
    }

    @Test
    void emitsGlyphStringWithRowsJoinedByNewline() {
        generate(font("hud", "font", HUD_FILE, 7, 8, rows("ab", "cd")));

        assertTrue(readRegistry().contains("\"ab\\n\"") && readRegistry().contains("\"cd\""),
                "the glyph constant must hold the rows joined by a newline but was:\n" + readRegistry());
    }

    @Test
    void sortsFontFieldsByVariableName() {
        generate(
                font("zeta", "font", "eldoria:font/zeta.png", 7, 8, rows("z")),
                font("alpha", "font", "eldoria:font/alpha.png", 7, 8, rows("a"))
        );

        String code = readRegistry();
        assertTrue(code.indexOf("ALPHA =") >= 0 && code.indexOf("ALPHA =") < code.indexOf("ZETA ="),
                "the fields should be sorted by variable name but was:\n" + code);
    }

    @Test
    void rejectsFontWithoutCharsWithKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(font("hud", "font", HUD_FILE, 7, 8, List.of())),
                "a font without chars must abort the generation");

        assertTrue(exception.getMessage().contains("hud"),
                "the message must name the font key but was: " + exception.getMessage());
    }

    @Test
    void generatedSourceContainsNoEscapedPlaceholders() {
        generate(font("hud", "font", HUD_FILE, 7, 8, rows("ab")));

        String code = readRegistry();
        assertFalse(code.contains("\\$"), "The source must not contain a backslash in front of a placeholder but was:\n" + code);
        assertFalse(code.contains("$"), "The source must not contain any unresolved placeholder but was:\n" + code);
    }

    @Test
    void generatedRegistryCompiles() throws IOException {
        generate(
                font("hud", "font", HUD_FILE, 7, 8, rows("", "\"\\")),
                font("icons", "symbols", "eldoria:font/icons.png", 7, 8, rows("äö"))
        );

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

        assertEquals(0, exitCode, "The generated FontRegistry must compile but the compiler reported:\n" + errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void generationRunsInReadOnlyTransaction() throws NoSuchMethodException {
        boolean readOnly = FontGenerator.class.getMethod("generate", Path.class, UUID.class).isAnnotationPresent(ReadOnly.class);

        assertTrue(readOnly, "generate must be annotated with @ReadOnly so lazy font chars can be loaded");
    }

    private void generate(FontEntity... fonts) {
        FontRepository repository = mock(FontRepository.class);
        when(repository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                .thenReturn(Page.of(List.of(fonts), Pageable.UNPAGED, (long) fonts.length));

        new FontGenerator(repository).generate(tempDir, PROJECT_ID);
    }

    private String readRegistry() {
        try {
            return Files.readString(tempDir.resolve(REGISTRY_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("The FontRegistry should have been generated at " + REGISTRY_PATH, e);
        }
    }

    private static List<FontStringEntity> rows(String... lines) {
        List<FontStringEntity> chars = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            chars.add(new FontStringEntity(uuid(i + 1), lines[i], i + 1));
        }
        return chars;
    }

    private static FontEntity font(String key, String mapper, String file, int ascent, int height, List<FontStringEntity> chars) {
        ProjectEntity project = new ProjectEntity();
        project.setKey("eldoria");

        FontEntity font = new FontEntity();
        font.setProject(project);
        font.setKey(key);
        font.setMapper(mapper);
        font.setProvider("bitmap");
        font.setTexturePath(file);
        font.setAscent(ascent);
        font.setHeight(height);
        font.setChars(chars);
        return font;
    }

    private static UUID uuid(int index) {
        return UUID.fromString(String.format("80000000-0000-0000-0000-%012d", index));
    }
}
