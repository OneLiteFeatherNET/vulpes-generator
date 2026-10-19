package net.onelitefeather.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

/**
 * Generates the code-based custom font symbol registry of vulpes-base as three top-level files in
 * {@code net.onelitefeather.vulpes.font}:
 * <ul>
 *     <li>{@code CustomFontSymbolKeys}: one {@code Key} constant per font symbol,</li>
 *     <li>{@code CustomFontSymbols}: one {@code CustomFontSymbol} constant per font symbol, resolved through the registry,</li>
 *     <li>{@code CustomFontSymbolEntries}: the list of all font symbols the registry is built from.</li>
 * </ul>
 * The key of a symbol is the namespace of the font (the project part of its namespaced key) and the key of the font
 * entity as path, for example {@code eldoria:hud_icons}. It is unique because the identifier of the constant is
 * claimed through {@link JavaIdentifiers.Claims}, which rejects two keys that map to the same identifier. The symbol
 * additionally carries the {@code Key} of the font file ({@code namespace:mapper}) for {@code Component.font(...)}.
 * <p>
 * The glyphs are written as string literals. Every character outside printable ASCII is written as a
 * unicode escape (a backslash, the letter u and four hex digits), so private use glyphs stay visible in the source. A
 * supplementary codepoint becomes its surrogate pair. The files are written even without fonts.
 * <p>
 * The fonts are validated with {@link BitmapFontProviderCodec#fromFont(FontEntity)}, so a font that is invalid for the
 * JSON output aborts the generation here too. The generation runs read only inside a transaction, because the chars
 * of a font are loaded lazily.
 * <p>
 * JavaPoet 1.13 cannot emit {@code sealed ... permits}, so the declaration of {@code CustomFontSymbols} is rewritten
 * by {@link #asSealedInterface(String)}. The generated sources are compiled by the tests.
 */
@Prototype
public class FontGenerator extends AbstractCodeGenerator<FontEntity> implements JavaStructure {

    private static final String FONT_PACKAGE = BASE_PACKAGE + ".font";
    private static final String KEYS_NAME = "CustomFontSymbolKeys";
    private static final String SYMBOLS_NAME = "CustomFontSymbols";
    private static final String ENTRIES_NAME = "CustomFontSymbolEntries";

    private static final ClassName KEY = ClassName.get(Key.class);
    private static final ClassName KEYS = ClassName.get(FONT_PACKAGE, KEYS_NAME);
    private static final ClassName FONT_SYMBOL = ClassName.get(FONT_PACKAGE, "FontSymbol");
    private static final ClassName CUSTOM_FONT_SYMBOL = ClassName.get(FONT_PACKAGE, "CustomFontSymbol");
    private static final ClassName CUSTOM_FONT_SYMBOL_IMPL = ClassName.get(FONT_PACKAGE, "CustomFontSymbolImpl");
    private static final ClassName LIST = ClassName.get(List.class);

    private final FontRepository fontRepository;

    @Inject
    public FontGenerator(FontRepository fontRepository) {
        super(SYMBOLS_NAME, FONT_PACKAGE, TypeSpec.Kind.INTERFACE);
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

        Map<String, FontEntity> fonts = new TreeMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();
        for (FontEntity model : models) {
            BitmapFontProviderCodec.fromFont(model);
            String identifier = fieldNames.claim(JavaIdentifiers.constantName(model.getKey()), model.getKey());
            fonts.put(identifier, model);
        }

        Path directory = root.resolve(JAVA_MAIM_FOLDER).resolve(packageName.replace('.', '/'));
        write(directory, KEYS_NAME, render(createKeys(fonts)));
        write(directory, SYMBOLS_NAME, asSealedInterface(render(createSymbols(fonts))));
        write(directory, ENTRIES_NAME, render(createEntries(fonts)));
    }

    private TypeSpec createKeys(@NotNull Map<String, FontEntity> fonts) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(KEYS_NAME).addModifiers(Modifier.PUBLIC);
        addJetbrainsAnnotation(builder);
        fonts.forEach((identifier, model) -> builder.addField(
                FieldSpec.builder(KEY, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.key($S, $S)", KEY, BitmapFontProviderCodec.namespaceOf(model), model.getKey())
                        .build()));
        return builder.build();
    }

    private TypeSpec createSymbols(@NotNull Map<String, FontEntity> fonts) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(SYMBOLS_NAME);
        fonts.keySet().forEach(identifier -> builder.addField(
                FieldSpec.builder(CUSTOM_FONT_SYMBOL, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.get($T.$L)", CUSTOM_FONT_SYMBOL_IMPL, KEYS, identifier)
                        .build()));
        return builder.build();
    }

    private TypeSpec createEntries(@NotNull Map<String, FontEntity> fonts) {
        List<CodeBlock> entries = new ArrayList<>();
        fonts.forEach((identifier, model) -> {
            BitmapFontProvider provider = BitmapFontProviderCodec.fromFont(model);
            String rows = provider.chars().stream()
                    .map(FontGenerator::escapeLiteral)
                    .collect(Collectors.joining(", "));
            entries.add(CodeBlock.of("new $T($T.$L, $T.key($S, $S), new $T($S, $S, $L, $L, $T.of($L)))",
                    CUSTOM_FONT_SYMBOL_IMPL,
                    KEYS,
                    identifier,
                    KEY,
                    BitmapFontProviderCodec.namespaceOf(model),
                    model.getMapper(),
                    FONT_SYMBOL,
                    provider.file(),
                    BitmapFontProviderCodec.TYPE,
                    provider.ascent(),
                    provider.height(),
                    LIST,
                    rows));
        });

        ParameterizedTypeName listType = ParameterizedTypeName.get(LIST, CUSTOM_FONT_SYMBOL);
        MethodSpec.Builder method = MethodSpec.methodBuilder("entries")
                .addModifiers(Modifier.STATIC)
                .returns(listType);
        if (entries.isEmpty()) {
            method.addStatement("return $T.of()", List.class);
        } else {
            method.addStatement("return $T.of($L)", List.class, CodeBlock.join(entries, ",\n"));
        }

        return TypeSpec.classBuilder(ENTRIES_NAME)
                .addModifiers(Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                .addMethod(method.build())
                .build();
    }

    private static String render(@NotNull TypeSpec type) {
        return JavaFile.builder(FONT_PACKAGE, type)
                .indent(INDENT_DEFAULT)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    /**
     * Turns the declaration of the {@code CustomFontSymbols} interface into {@code sealed ... permits CustomFontSymbol}.
     *
     * @throws IllegalStateException if JavaPoet did not emit the expected declaration
     */
    private static String asSealedInterface(@NotNull String source) {
        String declaration = "interface " + SYMBOLS_NAME + " {";
        if (!source.contains(declaration)) {
            throw new IllegalStateException("the interface " + SYMBOLS_NAME + " was not found in the generated source");
        }
        return source.replace(declaration, "sealed interface " + SYMBOLS_NAME + " permits CustomFontSymbol {");
    }

    private static void write(@NotNull Path directory, @NotNull String name, @NotNull String source) {
        Path file = directory.resolve(name + ".java");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, source, UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to write font symbols to " + file, e);
        }
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
