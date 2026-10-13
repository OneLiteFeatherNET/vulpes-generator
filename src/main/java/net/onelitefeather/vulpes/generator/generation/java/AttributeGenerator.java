package net.onelitefeather.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import net.kyori.adventure.key.Key;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.api.repository.AttributeRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaIdentifiers;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

/**
 * Generates the {@code CustomAttributes} interface of vulpes-base, which replaces its placeholder.
 * <p>
 * The interface declares the nested record {@code Definition} and one constant of it per attribute. The constants are
 * sorted by their identifier. The interface is written even without attributes, so the placeholder is always replaced.
 * <p>
 * JavaPoet 1.13 has no record support, so the nested type is emitted as an empty class and turned into the record
 * declaration by {@link #withRecordDefinition(String)}. The generated source is compiled by the tests.
 */
@Prototype
public class AttributeGenerator extends AbstractCodeGenerator<AttributeEntity> implements JavaStructure {

    private static final ClassName KEY = ClassName.get(Key.class);
    private static final String DEFINITION_NAME = "Definition";
    private static final Pattern EMPTY_DEFINITION = Pattern.compile("class " + DEFINITION_NAME + " \\{\\s*}");
    private static final String RECORD_DEFINITION = "record " + DEFINITION_NAME
            + "(net.kyori.adventure.key.Key key, String name, double defaultValue, double maximumValue) {\n  }";

    private final AttributeRepository attributeRepository;
    private final ClassName definitionClass;

    @Inject
    protected AttributeGenerator(@NotNull AttributeRepository attributeRepository) {
        super("CustomAttributes", BASE_PACKAGE + ".attribute", TypeSpec.Kind.INTERFACE);
        this.attributeRepository = attributeRepository;
        this.definitionClass = ClassName.get(BASE_PACKAGE + ".attribute", "CustomAttributes", DEFINITION_NAME);
    }

    @Override
    protected List<AttributeEntity> getModels(@NotNull UUID projectId) {
        return this.attributeRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        cleanup();
        List<AttributeEntity> attributeModels = this.getModels(projectId);

        this.classBuilder.addModifiers(Modifier.PUBLIC);
        addJetbrainsAnnotation(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);
        this.classBuilder.addType(createDefinitionType());

        Map<String, FieldSpec> fields = new TreeMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();
        for (AttributeEntity attributeModel : attributeModels) {
            validate(attributeModel);
            String identifier = fieldNames.claim(JavaIdentifiers.constantName(attributeModel.getKey()), attributeModel.getKey());
            fields.put(identifier, createField(identifier, attributeModel));
        }
        this.classBuilder.addFields(fields.values());

        String source = JavaFile.builder(packageName, this.classBuilder.build())
                .indent(INDENT_DEFAULT)
                .build()
                .toString();
        writeSource(root, withRecordDefinition(source));
    }

    private TypeSpec createDefinitionType() {
        return TypeSpec.classBuilder(DEFINITION_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("The definition of a custom attribute: its key, its name in the UI and its value range.\n")
                .build();
    }

    private FieldSpec createField(@NotNull String identifier, @NotNull AttributeEntity model) {
        return FieldSpec.builder(this.definitionClass, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T($T.key($S, $S), $S, $L, $L)",
                        this.definitionClass,
                        KEY,
                        namespaceOf(model),
                        model.getKey(),
                        model.getUiName(),
                        Double.toString(model.getDefaultValue()),
                        Double.toString(model.getMaximumValue()))
                .build();
    }

    /**
     * Replaces the empty nested class of the source with the record declaration of {@code Definition}.
     *
     * @throws IllegalStateException if JavaPoet did not emit the empty nested class
     */
    private static String withRecordDefinition(@NotNull String source) {
        Matcher matcher = EMPTY_DEFINITION.matcher(source);
        if (!matcher.find()) {
            throw new IllegalStateException("the nested class " + DEFINITION_NAME + " was not found in the generated source");
        }
        return matcher.replaceFirst(Matcher.quoteReplacement(RECORD_DEFINITION));
    }

    private void writeSource(@NotNull Path root, @NotNull String source) {
        Path directory = root.resolve(JAVA_MAIM_FOLDER).resolve(packageName.replace('.', '/'));
        Path file = directory.resolve(className + ".java");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, source, UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to write attribute definitions to " + file, e);
        }
    }

    private static void validate(@NotNull AttributeEntity model) {
        String key = model.getKey();
        if (model.getUiName() == null || model.getUiName().isBlank()) {
            throw new IllegalArgumentException("attribute '" + key + "' has no ui name");
        }
        double defaultValue = model.getDefaultValue();
        double maximumValue = model.getMaximumValue();
        if (!Double.isFinite(defaultValue) || !Double.isFinite(maximumValue)) {
            throw new IllegalArgumentException("attribute '" + key + "' must have finite default and maximum values");
        }
        if (maximumValue < defaultValue) {
            throw new IllegalArgumentException("attribute '" + key + "' has the maximum " + maximumValue
                    + " below its default " + defaultValue);
        }
    }

    /**
     * Returns the project part of the namespaced key, for example {@code eldoria_rpg} for {@code eldoria_rpg:crit_chance}.
     */
    private static String namespaceOf(@NotNull AttributeEntity model) {
        String namespacedKey = model.getNamespacedKey();
        return namespacedKey.substring(0, namespacedKey.indexOf(':'));
    }

    @Override
    public @NotNull String getName() {
        return "AttributeGenerator";
    }
}
