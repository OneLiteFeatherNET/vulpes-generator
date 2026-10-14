package net.onelitefeather.vulpes.generator.generation.java;

import com.google.gson.JsonElement;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.advancement.AdvancementEntity;
import net.onelitefeather.vulpes.api.repository.AdvancementRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaIdentifiers;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import net.onelitefeather.vulpes.generator.generation.component.AdvancementData;
import net.onelitefeather.vulpes.generator.generation.component.AdvancementTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

/**
 * Generates the code-based custom advancement registry of vulpes-base as three top-level files in
 * {@code net.onelitefeather.vulpes.advancement}:
 * <ul>
 *     <li>{@code CustomAdvancementKeys}: one {@code Key} constant per advancement,</li>
 *     <li>{@code CustomAdvancements}: one {@code CustomAdvancement} constant per advancement, resolved through the registry,</li>
 *     <li>{@code CustomAdvancementEntries}: the list of all advancements the registry is built from.</li>
 * </ul>
 * Only the definitions are generated, the tabs are built by {@code AdvancementTabs} in vulpes-base. The constants are
 * sorted by their identifier. The files are written even without advancements.
 * <p>
 * Titles and descriptions are emitted as JSON text and deserialized at runtime. All values are validated by
 * {@link AdvancementTree} at generation time, so invalid data aborts the generation instead of ending up in the code.
 * The generation runs read only inside a transaction, because the parent relations are loaded lazily.
 * <p>
 * JavaPoet 1.13 cannot emit {@code sealed ... permits}, so the declaration of {@code CustomAdvancements} is rewritten
 * by {@link #asSealedInterface(String)}. The generated sources are compiled by the tests.
 */
@Prototype
public class AdvancementGenerator extends AbstractCodeGenerator<AdvancementEntity> implements JavaStructure {

    private static final String ADVANCEMENT_PACKAGE = BASE_PACKAGE + ".advancement";
    private static final String KEYS_NAME = "CustomAdvancementKeys";
    private static final String ADVANCEMENTS_NAME = "CustomAdvancements";
    private static final String ENTRIES_NAME = "CustomAdvancementEntries";

    private static final ClassName KEY = ClassName.get(Key.class);
    private static final ClassName KEYS = ClassName.get(ADVANCEMENT_PACKAGE, KEYS_NAME);
    private static final ClassName CUSTOM_ADVANCEMENT = ClassName.get(ADVANCEMENT_PACKAGE, "CustomAdvancement");
    private static final ClassName CUSTOM_ADVANCEMENT_IMPL = ClassName.get(ADVANCEMENT_PACKAGE, "CustomAdvancementImpl");
    private static final ClassName FRAME_TYPE = ClassName.get(FrameType.class);
    private static final ClassName MATERIAL = ClassName.get(Material.class);
    private static final ClassName ITEM_STACK = ClassName.get(ItemStack.class);
    private static final ClassName COMPONENT = ClassName.get(Component.class);
    private static final ClassName GSON_COMPONENT_SERIALIZER = ClassName.get(GsonComponentSerializer.class);

    private final AdvancementRepository advancementRepository;

    @Inject
    public AdvancementGenerator(@NotNull AdvancementRepository advancementRepository) {
        super(ADVANCEMENTS_NAME, ADVANCEMENT_PACKAGE, TypeSpec.Kind.INTERFACE);
        this.advancementRepository = advancementRepository;
    }

    @Override
    protected List<AdvancementEntity> getModels(@NotNull UUID projectId) {
        return this.advancementRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        cleanup();
        List<AdvancementData> data = AdvancementTree.from(getModels(projectId));

        Map<String, AdvancementData> advancements = new TreeMap<>();
        Map<String, String> identifierByNamespacedKey = new HashMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();
        for (AdvancementData advancement : data) {
            String identifier = fieldNames.claim(JavaIdentifiers.constantName(advancement.variableName()), advancement.key());
            advancements.put(identifier, advancement);
            identifierByNamespacedKey.put(advancement.namespacedKey(), identifier);
        }

        Path directory = root.resolve(JAVA_MAIM_FOLDER).resolve(packageName.replace('.', '/'));
        write(directory, KEYS_NAME, render(createKeys(advancements)));
        write(directory, ADVANCEMENTS_NAME, asSealedInterface(render(createAdvancements(advancements))));
        write(directory, ENTRIES_NAME, render(createEntries(advancements, identifierByNamespacedKey)));
    }

    @Override
    public @NotNull String getName() {
        return "AdvancementGenerator";
    }

    private TypeSpec createKeys(@NotNull Map<String, AdvancementData> advancements) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(KEYS_NAME).addModifiers(Modifier.PUBLIC);
        addJetbrainsAnnotation(builder);
        advancements.forEach((identifier, advancement) -> builder.addField(
                FieldSpec.builder(KEY, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.key($S, $S)", KEY, namespaceOf(advancement), advancement.key())
                        .build()));
        return builder.build();
    }

    private TypeSpec createAdvancements(@NotNull Map<String, AdvancementData> advancements) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(ADVANCEMENTS_NAME);
        advancements.keySet().forEach(identifier -> builder.addField(
                FieldSpec.builder(CUSTOM_ADVANCEMENT, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.get($T.$L)", CUSTOM_ADVANCEMENT_IMPL, KEYS, identifier)
                        .build()));
        return builder.build();
    }

    private TypeSpec createEntries(@NotNull Map<String, AdvancementData> advancements,
                                   @NotNull Map<String, String> identifierByNamespacedKey) {
        List<CodeBlock> entries = new ArrayList<>();
        advancements.forEach((identifier, advancement) -> entries.add(createEntry(identifier, advancement, identifierByNamespacedKey)));

        ParameterizedTypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), CUSTOM_ADVANCEMENT);
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

    private static CodeBlock createEntry(String identifier, AdvancementData advancement,
                                         Map<String, String> identifierByNamespacedKey) {
        CodeBlock parent = advancement.isRoot()
                ? CodeBlock.of("null")
                : CodeBlock.of("$T.$L", KEYS, identifierByNamespacedKey.get(advancement.parentNamespacedKey()));
        return CodeBlock.of("new $T($T.$L, $L, $L, $L, $T.of($T.$L), $T.$L, $L, $L, $L, $L, $L)",
                CUSTOM_ADVANCEMENT_IMPL,
                KEYS, identifier,
                parent,
                textComponent(advancement.title()),
                textComponent(advancement.description()),
                ITEM_STACK, MATERIAL, advancement.material().key().value().toUpperCase(Locale.ROOT),
                FRAME_TYPE, advancement.frameType().name(),
                floatLiteral(advancement.x()), floatLiteral(advancement.y()),
                advancement.showToast(), advancement.hidden(),
                backgroundLiteral(advancement.background()));
    }

    /**
     * Emits the title or description as {@code GsonComponentSerializer.gson().deserialize(json)}, or {@code Component.empty()}
     * if the value is missing.
     */
    private static CodeBlock textComponent(@Nullable JsonElement text) {
        if (text == null) {
            return CodeBlock.of("$T.empty()", COMPONENT);
        }
        return CodeBlock.of("$T.gson().deserialize($S)", GSON_COMPONENT_SERIALIZER, text.toString());
    }

    private static CodeBlock backgroundLiteral(@Nullable String background) {
        return background == null ? CodeBlock.of("null") : CodeBlock.of("$S", background);
    }

    private static String floatLiteral(float value) {
        return Float.toString(value) + "f";
    }

    private static String render(@NotNull TypeSpec type) {
        return JavaFile.builder(ADVANCEMENT_PACKAGE, type)
                .indent(INDENT_DEFAULT)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    /**
     * Turns the declaration of the {@code CustomAdvancements} interface into {@code sealed ... permits CustomAdvancement}.
     *
     * @throws IllegalStateException if JavaPoet did not emit the expected declaration
     */
    private static String asSealedInterface(@NotNull String source) {
        String declaration = "interface " + ADVANCEMENTS_NAME + " {";
        if (!source.contains(declaration)) {
            throw new IllegalStateException("the interface " + ADVANCEMENTS_NAME + " was not found in the generated source");
        }
        return source.replace(declaration, "sealed interface " + ADVANCEMENTS_NAME + " permits CustomAdvancement {");
    }

    private static void write(@NotNull Path directory, @NotNull String name, @NotNull String source) {
        Path file = directory.resolve(name + ".java");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, source, UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to write advancement definitions to " + file, e);
        }
    }

    /**
     * Returns the project part of the namespaced key, for example {@code eldoria_rpg} for {@code eldoria_rpg:first_kill}.
     */
    private static String namespaceOf(@NotNull AdvancementData advancement) {
        String namespacedKey = advancement.namespacedKey();
        return namespacedKey.substring(0, namespacedKey.indexOf(':'));
    }
}
