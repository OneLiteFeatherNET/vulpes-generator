package net.onelitefeather.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import net.kyori.adventure.key.Key;
import net.onelitefeather.vulpes.api.model.FontEntity;
import net.onelitefeather.vulpes.api.repository.FontRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaIdentifiers;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import net.onelitefeather.vulpes.generator.generation.font.BitmapFontProviderCodec;
import net.onelitefeather.vulpes.generator.generation.font.BitmapFontProviderCodec.BitmapFontProvider;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

/**
 * Generates the {@code FontSymbols} interface of vulpes-base, which replaces its placeholder. Per font it emits two
 * constants, sorted by their identifier:
 * <ul>
 *     <li>{@code <NAME>} is a {@code FontSymbol} with the texture, the type {@code bitmap}, the metrics and the rows.</li>
 *     <li>{@code <NAME>_FONT} is the {@link Key} of the font file, {@code namespace:mapper}.</li>
 * </ul>
 * The glyphs are written as string literals. Every character outside printable ASCII is written as a
 * unicode escape (a backslash, the letter u and four hex digits), so private use glyphs stay visible in the source. A
 * supplementary codepoint becomes its
 * surrogate pair. The interface is written even without fonts, so the placeholder is always replaced.
 * <p>
 * The fonts are validated with {@link BitmapFontProviderCodec#fromFont(FontEntity)}, so a font that is invalid for the
 * JSON output aborts the generation here too. The generation runs read only inside a transaction, because the chars
 * of a font are loaded lazily.
 */
@Prototype
public class FontGenerator extends AbstractCodeGenerator<FontEntity> implements JavaStructure {

    private static final ClassName KEY = ClassName.get(Key.class);
    private static final ClassName FONT_SYMBOL = ClassName.get(BASE_PACKAGE + ".font", "FontSymbol");
    private static final ClassName LIST = ClassName.get(List.class);
    private static final String KEY_SUFFIX = "_FONT";

    private final FontRepository fontRepository;

    @Inject
    public FontGenerator(FontRepository fontRepository) {
        super("FontSymbols", BASE_PACKAGE + ".font", TypeSpec.Kind.INTERFACE);
        this.fontRepository = fontRepository;
    }

    @Override
    protected List<FontEntity> getModels(@NotNull UUID projectId) {
        return this.fontRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        cleanup();
        List<FontEntity> models = getModels(projectId);

        this.classBuilder.addModifiers(Modifier.PUBLIC);
        addJetbrainsAnnotation(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);

        Map<String, FieldSpec[]> fontFields = new TreeMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();
        for (FontEntity model : models) {
            String identifier = JavaIdentifiers.constantName(model.getKey());
            fieldNames.claim(identifier, model.getKey());
            fieldNames.claim(identifier + KEY_SUFFIX, model.getKey());
            fontFields.put(identifier, createFields(model, identifier));
        }

        fontFields.values().forEach(fields -> this.classBuilder.addFields(List.of(fields)));

        filesToGenerate.clear();
        filesToGenerate.add(
                JavaFile.builder(packageName, this.classBuilder.build())
                        .indent(INDENT_DEFAULT)
                        .skipJavaLangImports(true)
                        .build()
        );
        writeFiles(root.resolve(JAVA_MAIM_FOLDER));
    }

    /**
     * Creates the symbol field and the key field of a font.
     */
    private static FieldSpec[] createFields(@NotNull FontEntity model, @NotNull String identifier) {
        BitmapFontProvider provider = BitmapFontProviderCodec.fromFont(model);
        String namespace = BitmapFontProviderCodec.namespaceOf(model);

        String rows = provider.chars().stream()
                .map(FontGenerator::escapeLiteral)
                .collect(Collectors.joining(", "));
        FieldSpec symbol = FieldSpec.builder(FONT_SYMBOL, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T($S, $S, $L, $L, $T.of($L))",
                        FONT_SYMBOL,
                        provider.file(),
                        BitmapFontProviderCodec.TYPE,
                        provider.ascent(),
                        provider.height(),
                        LIST,
                        rows)
                .build();
        FieldSpec key = FieldSpec.builder(KEY, identifier + KEY_SUFFIX, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.key($S, $S)", KEY, namespace, model.getMapper())
                .build();
        return new FieldSpec[]{symbol, key};
    }

    /**
     * Returns the Java string literal of a glyph row. Quotes and backslashes are escaped, the common control characters
     * use their escape sequence and every other character outside printable ASCII becomes a unicode escape.
     * JavaPoet does not escape these characters, so this method is required.
     *
     * @param row the row of glyphs
     * @return the row as a quoted Java string literal that contains only printable ASCII
     */
    static String escapeLiteral(@NotNull String row) {
        StringBuilder literal = new StringBuilder(row.length() + 2).append('"');
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            switch (c) {
                case '"' -> literal.append("\\\"");
                case '\\' -> literal.append("\\\\");
                case '\b' -> literal.append("\\b");
                case '\t' -> literal.append("\\t");
                case '\n' -> literal.append("\\n");
                case '\f' -> literal.append("\\f");
                case '\r' -> literal.append("\\r");
                default -> {
                    if (c >= 0x20 && c <= 0x7E) {
                        literal.append(c);
                    } else {
                        literal.append(String.format(Locale.ROOT, "\\u%04X", (int) c));
                    }
                }
            }
        }
        return literal.append('"').toString();
    }

    @Override
    public @NotNull String getName() {
        return "FontGenerator";
    }
}
