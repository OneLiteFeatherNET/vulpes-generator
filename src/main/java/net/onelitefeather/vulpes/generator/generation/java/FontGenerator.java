package net.onelitefeather.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import net.kyori.adventure.key.Key;
import net.onelitefeather.vulpes.api.model.FontEntity;
import net.onelitefeather.vulpes.api.repository.FontRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import net.onelitefeather.vulpes.generator.generation.font.BitmapFontProviderCodec;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

/**
 * Generates the {@code FontRegistry} class with two constants per font:
 * <ul>
 *     <li>{@code <VARIABLE>_FONT} is the {@link Key} of the font file, {@code namespace:mapper}.</li>
 *     <li>{@code <VARIABLE>} is the glyph string of the font. The rows of the font are joined with a newline.</li>
 * </ul>
 * The constants are sorted by variable name. The glyphs are emitted as escaped string literals, so private use
 * codepoints and quotes are safe in the generated source.
 * <p>
 * The fonts are validated with {@link BitmapFontProviderCodec#fromFont(FontEntity)}, so a font that is invalid for the
 * JSON output aborts the generation here too. The generation runs read only inside a transaction, because the chars
 * of a font are loaded lazily.
 */
@Prototype
public class FontGenerator extends AbstractCodeGenerator<FontEntity> implements JavaStructure {

    private static final ClassName KEY = ClassName.get(Key.class);

    private final FontRepository fontRepository;

    @Inject
    public FontGenerator(FontRepository fontRepository) {
        super("FontRegistry", BASE_PACKAGE + ".font");
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

        if (models.isEmpty()) return;

        addClassModifiers(this.classBuilder);
        addJetbrainsAnnotation(this.classBuilder);
        addPrivateDefaultConstructor(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);

        Map<String, FieldSpec[]> fontFields = new TreeMap<>();
        for (FontEntity model : models) {
            String variableName = model.getVariableName();
            if (variableName == null || variableName.isEmpty()) continue;
            if (fontFields.containsKey(variableName)) {
                throw new IllegalArgumentException("more than one font has the variable name '" + variableName + "'");
            }
            fontFields.put(variableName, createFields(model, variableName));
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
     * Creates the key field and the glyph field of a font.
     */
    private static FieldSpec[] createFields(@NotNull FontEntity model, @NotNull String variableName) {
        BitmapFontProviderCodec.BitmapFontProvider provider = BitmapFontProviderCodec.fromFont(model);
        String glyphs = String.join("\n", provider.chars());
        String namespace = BitmapFontProviderCodec.namespaceOf(model);

        FieldSpec key = FieldSpec.builder(KEY, variableName + "_FONT", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.key($S, $S)", KEY, namespace, model.getMapper())
                .build();
        FieldSpec glyphField = FieldSpec.builder(String.class, variableName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", glyphs)
                .build();
        return new FieldSpec[]{key, glyphField};
    }

    @Override
    public @NotNull String getName() {
        return "FontGenerator";
    }
}
