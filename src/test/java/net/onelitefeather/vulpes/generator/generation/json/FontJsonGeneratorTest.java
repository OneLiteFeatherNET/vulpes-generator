package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import net.onelitefeather.vulpes.api.model.FontEntity;
import net.onelitefeather.vulpes.api.model.font.FontStringEntity;
import net.onelitefeather.vulpes.api.model.project.ProjectEntity;
import net.onelitefeather.vulpes.api.repository.FontRepository;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

class FontJsonGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String NAMESPACE = "eldoria";
    private static final String HUD_FILE = "eldoria:font/hud_icons.png";

    @TempDir
    Path tempDir;

    @Test
    void writesBitmapProviderWithFileAscentHeightAndChars() {
        generate(font("hud", "font", HUD_FILE, 7, 9, rows("ab", "cd")));

        JsonObject provider = readProviders("font").get(0).getAsJsonObject();
        assertEquals("bitmap", provider.get("type").getAsString(), "the provider type must be bitmap");
        assertEquals(HUD_FILE, provider.get("file").getAsString(), "the file must be the texture path of the font");
        assertEquals(7, provider.get("ascent").getAsInt(), "the ascent must be written as stored");
        assertEquals(9, provider.get("height").getAsInt(), "the height must be written as stored");
        assertEquals(List.of("ab", "cd"), toStrings(provider.getAsJsonArray("chars")), "the chars must be written as rows");
    }

    @Test
    void writesFontsWithSameMapperIntoOneFileSortedByKey() {
        generate(
                font("zeta", "font", "eldoria:font/zeta.png", 7, 8, rows("z")),
                font("alpha", "font", "eldoria:font/alpha.png", 7, 8, rows("a")),
                font("icons", "symbols", "eldoria:font/icons.png", 7, 8, rows("i"))
        );

        JsonArray providers = readProviders("font");
        assertEquals(2, providers.size(), "both fonts with the mapper font must be providers of font.json");
        assertEquals("eldoria:font/alpha.png", providers.get(0).getAsJsonObject().get("file").getAsString(),
                "providers must be sorted by key so alpha comes before zeta");
        assertEquals("eldoria:font/zeta.png", providers.get(1).getAsJsonObject().get("file").getAsString(),
                "providers must be sorted by key so alpha comes before zeta");
        assertEquals(1, readProviders("symbols").size(), "a font with another mapper must be written to its own file");
    }

    @Test
    void ordersCharsByOrderIndex() {
        generate(font("hud", "font", HUD_FILE, 7, 8, List.of(
                new FontStringEntity(uuid(2), "cd", 2),
                new FontStringEntity(uuid(1), "ab", 1)
        )));

        JsonObject provider = readProviders("font").get(0).getAsJsonObject();
        assertEquals(List.of("ab", "cd"), toStrings(provider.getAsJsonArray("chars")),
                "the chars must be written in orderIndex order");
    }

    @Test
    void rejectsFontWithoutCharsWithKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(font("hud", "font", HUD_FILE, 7, 8, rows())),
                "a font without chars must abort the generation");

        assertTrue(exception.getMessage().contains("hud"),
                "the message must name the font key but was: " + exception.getMessage());
    }

    @Test
    void rejectsAscentGreaterThanHeightWithKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(font("hud", "font", HUD_FILE, 9, 8, rows("ab"))),
                "an ascent greater than the height must abort the generation");

        assertTrue(exception.getMessage().contains("hud"),
                "the message must name the font key but was: " + exception.getMessage());
    }

    @Test
    void rejectsFilePathWithoutNamespaceWithKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(font("hud", "font", "font/hud_icons.png", 7, 8, rows("ab"))),
                "a file without a namespace must abort the generation");

        assertTrue(exception.getMessage().contains("hud"),
                "the message must name the font key but was: " + exception.getMessage());
    }

    @Test
    void rejectsProviderOtherThanBitmapWithKeyInMessage() {
        FontEntity font = font("hud", "font", HUD_FILE, 7, 8, rows("ab"));
        font.setProvider("ttf");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(font),
                "a provider other than bitmap must abort the generation");

        assertTrue(exception.getMessage().contains("hud"),
                "the message must name the font key but was: " + exception.getMessage());
    }

    @Test
    void rejectsRowsWithDifferentLengthWithKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(font("hud", "font", HUD_FILE, 7, 8, rows("ab", "c"))),
                "rows with different lengths must abort the generation");

        assertTrue(exception.getMessage().contains("hud"),
                "the message must name the font key but was: " + exception.getMessage());
    }

    @Test
    void countsCodepointsWhenComparingRowLengths() {
        generate(font("glyphs", "font", HUD_FILE, 7, 8, rows("", "ab")));

        assertEquals(1, readProviders("font").size(),
                "a private use glyph is one codepoint, so both rows have the same length and the font is written");
    }

    @Test
    void generationRunsInReadOnlyTransaction() throws NoSuchMethodException {
        boolean readOnly = FontJsonGenerator.class.getMethod("generate", Path.class, UUID.class).isAnnotationPresent(ReadOnly.class);

        assertTrue(readOnly, "generate must be annotated with @ReadOnly so lazy font chars can be loaded");
    }

    @Test
    void generatingTwiceProducesIdenticalFile() throws IOException {
        FontJsonGenerator generator = new FontJsonGenerator(repositoryWith(
                font("hud", "font", HUD_FILE, 7, 8, rows("ab")),
                font("alpha", "font", "eldoria:font/alpha.png", 7, 8, rows("a"))));
        Path file = tempDir.resolve("src/main/resources/assets/eldoria/font/font.json");

        generator.generate(tempDir, PROJECT_ID);
        String first = Files.readString(file, UTF_8);
        generator.generate(tempDir, PROJECT_ID);

        assertEquals(first, Files.readString(file, UTF_8), "a second run into the same target must write the same file");
    }

    private void generate(FontEntity... fonts) {
        new FontJsonGenerator(repositoryWith(fonts)).generate(tempDir, PROJECT_ID);
    }

    private static FontRepository repositoryWith(FontEntity... fonts) {
        FontRepository repository = mock(FontRepository.class);
        when(repository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                .thenReturn(Page.of(List.of(fonts), Pageable.UNPAGED, (long) fonts.length));
        return repository;
    }

    private JsonArray readProviders(String mapper) {
        Path file = tempDir.resolve("src/main/resources/assets").resolve(NAMESPACE).resolve("font").resolve(mapper + ".json");
        assertTrue(Files.exists(file), "the font file for mapper " + mapper + " must exist at " + file);
        try (var reader = Files.newBufferedReader(file, UTF_8)) {
            JsonObject root = GsonHolder.GSON.fromJson(reader, JsonObject.class);
            assertTrue(root != null && root.has("providers"), "the font file must contain a providers array");
            return root.getAsJsonArray("providers");
        } catch (IOException e) {
            throw new AssertionError("the font file must be readable", e);
        }
    }

    private static List<String> toStrings(JsonArray array) {
        List<String> values = new ArrayList<>();
        array.forEach(element -> values.add(element.getAsString()));
        return values;
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
        project.setKey(NAMESPACE);

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
        return UUID.fromString(String.format("70000000-0000-0000-0000-%012d", index));
    }
}
