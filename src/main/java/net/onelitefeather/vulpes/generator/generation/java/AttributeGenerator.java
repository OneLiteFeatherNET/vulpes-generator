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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

/**
 * Generates the code-based custom attribute registry of vulpes-base as three top-level files in
 * {@code net.onelitefeather.vulpes.attribute}:
 * <ul>
 *     <li>{@code CustomAttributeKeys}: one {@code Key} constant per attribute,</li>
 *     <li>{@code CustomAttributes}: one {@code CustomAttribute} constant per attribute, resolved through the registry,</li>
 *     <li>{@code CustomAttributeEntries}: the list of all attributes the registry is built from.</li>
 * </ul>
 * The constants are sorted by their identifier. The files are written even without attributes.
 * <p>
 * JavaPoet 1.13 cannot emit {@code sealed ... permits}, so the declaration of {@code CustomAttributes} is rewritten
 * by {@link #asSealedInterface(String)}. The generated sources are compiled by the tests.
 */
@Prototype
public class AttributeGenerator extends AbstractCodeGenerator<AttributeEntity> implements JavaStructure {

    private static final String ATTRIBUTE_PACKAGE = BASE_PACKAGE + ".attribute";
    private static final String KEYS_NAME = "CustomAttributeKeys";
    private static final String ATTRIBUTES_NAME = "CustomAttributes";
    private static final String ENTRIES_NAME = "CustomAttributeEntries";

    private static final ClassName KEY = ClassName.get(Key.class);
    private static final ClassName KEYS = ClassName.get(ATTRIBUTE_PACKAGE, KEYS_NAME);
    private static final ClassName CUSTOM_ATTRIBUTE = ClassName.get(ATTRIBUTE_PACKAGE, "CustomAttribute");
    private static final ClassName CUSTOM_ATTRIBUTE_IMPL = ClassName.get(ATTRIBUTE_PACKAGE, "CustomAttributeImpl");

    private final AttributeRepository attributeRepository;

    @Inject
    protected AttributeGenerator(@NotNull AttributeRepository attributeRepository) {
        super(ATTRIBUTES_NAME, ATTRIBUTE_PACKAGE, TypeSpec.Kind.INTERFACE);
        this.attributeRepository = attributeRepository;
    }

    @Override
    protected List<AttributeEntity> getModels(@NotNull UUID projectId) {
        return this.attributeRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        cleanup();
        List<AttributeEntity> attributeModels = this.getModels(projectId);

        Map<String, AttributeEntity> attributes = new TreeMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();
        for (AttributeEntity attributeModel : attributeModels) {
            validate(attributeModel);
            String identifier = fieldNames.claim(JavaIdentifiers.constantName(attributeModel.getKey()), attributeModel.getKey());
            attributes.put(identifier, attributeModel);
        }

        Path directory = root.resolve(JAVA_MAIM_FOLDER).resolve(packageName.replace('.', '/'));
        write(directory, KEYS_NAME, render(createKeys(attributes)));
        write(directory, ATTRIBUTES_NAME, asSealedInterface(render(createAttributes(attributes))));
        write(directory, ENTRIES_NAME, render(createEntries(attributes)));
    }

    private TypeSpec createKeys(@NotNull Map<String, AttributeEntity> attributes) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(KEYS_NAME).addModifiers(Modifier.PUBLIC);
        addJetbrainsAnnotation(builder);
        attributes.forEach((identifier, model) -> builder.addField(
                FieldSpec.builder(KEY, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.key($S, $S)", KEY, namespaceOf(model), model.getKey())
                        .build()));
        return builder.build();
    }

    private TypeSpec createAttributes(@NotNull Map<String, AttributeEntity> attributes) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(ATTRIBUTES_NAME);
        attributes.keySet().forEach(identifier -> builder.addField(
                FieldSpec.builder(CUSTOM_ATTRIBUTE, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.get($T.$L)", CUSTOM_ATTRIBUTE_IMPL, KEYS, identifier)
                        .build()));
        return builder.build();
    }

    private TypeSpec createEntries(@NotNull Map<String, AttributeEntity> attributes) {
        List<CodeBlock> entries = new ArrayList<>();
        attributes.forEach((identifier, model) -> entries.add(CodeBlock.of("new $T($T.$L, $S, $L, $L)",
                CUSTOM_ATTRIBUTE_IMPL,
                KEYS,
                identifier,
                model.getUiName(),
                Double.toString(model.getDefaultValue()),
                Double.toString(model.getMaximumValue()))));

        ParameterizedTypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), CUSTOM_ATTRIBUTE);
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
        return JavaFile.builder(ATTRIBUTE_PACKAGE, type)
                .indent(INDENT_DEFAULT)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    /**
     * Turns the declaration of the {@code CustomAttributes} interface into {@code sealed ... permits CustomAttribute}.
     *
     * @throws IllegalStateException if JavaPoet did not emit the expected declaration
     */
    private static String asSealedInterface(@NotNull String source) {
        String declaration = "interface " + ATTRIBUTES_NAME + " {";
        if (!source.contains(declaration)) {
            throw new IllegalStateException("the interface " + ATTRIBUTES_NAME + " was not found in the generated source");
        }
        return source.replace(declaration, "sealed interface " + ATTRIBUTES_NAME + " permits CustomAttribute {");
    }

    private static void write(@NotNull Path directory, @NotNull String name, @NotNull String source) {
        Path file = directory.resolve(name + ".java");
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
