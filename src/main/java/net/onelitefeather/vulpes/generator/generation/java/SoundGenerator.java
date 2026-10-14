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
import net.onelitefeather.vulpes.api.model.sound.SoundEventEntity;
import net.onelitefeather.vulpes.api.repository.SoundRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaIdentifiers;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import net.onelitefeather.vulpes.generator.generation.sound.SoundEventKey;
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
 * Generates the code-based custom sound registry of vulpes-base as three top-level files in
 * {@code net.onelitefeather.vulpes.sound}:
 * <ul>
 *     <li>{@code CustomSoundKeys}: one {@code Key} constant per sound event,</li>
 *     <li>{@code CustomSounds}: one {@code CustomSound} constant per sound event, resolved through the registry,</li>
 *     <li>{@code CustomSoundEntries}: the list of all sounds the registry is built from.</li>
 * </ul>
 * The constants are named after the variable name of the event and sorted by their identifier. The files are written
 * even without sounds. The model of a sound event has no range, so every entry is created with a {@code null} range.
 * <p>
 * JavaPoet 1.13 cannot emit {@code sealed ... permits}, so the declaration of {@code CustomSounds} is rewritten
 * by {@link #asSealedInterface(String)}. The generation runs read only inside a transaction.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 0.3.2
 */
@Prototype
public class SoundGenerator extends AbstractCodeGenerator<SoundEventEntity> implements JavaStructure {

    private static final String SOUND_PACKAGE = BASE_PACKAGE + ".sound";
    private static final String KEYS_NAME = "CustomSoundKeys";
    private static final String SOUNDS_NAME = "CustomSounds";
    private static final String ENTRIES_NAME = "CustomSoundEntries";

    private static final ClassName KEY = ClassName.get(Key.class);
    private static final ClassName KEYS = ClassName.get(SOUND_PACKAGE, KEYS_NAME);
    private static final ClassName CUSTOM_SOUND = ClassName.get(SOUND_PACKAGE, "CustomSound");
    private static final ClassName CUSTOM_SOUND_IMPL = ClassName.get(SOUND_PACKAGE, "CustomSoundImpl");

    private final SoundRepository soundRepository;

    @Inject
    public SoundGenerator(@NotNull SoundRepository soundRepository) {
        super(SOUNDS_NAME, SOUND_PACKAGE, TypeSpec.Kind.INTERFACE);
        this.soundRepository = soundRepository;
    }

    @Override
    protected List<SoundEventEntity> getModels(@NotNull UUID projectId) {
        return this.soundRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        cleanup();
        List<SoundEventEntity> models = getModels(projectId);

        Map<String, SoundEventKey> sounds = new TreeMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();
        for (SoundEventEntity model : models) {
            String identifier = fieldNames.claim(JavaIdentifiers.constantName(variableNameOf(model)), model.getKey());
            sounds.put(identifier, SoundEventKey.of(model));
        }

        Path directory = root.resolve(JAVA_MAIM_FOLDER).resolve(packageName.replace('.', '/'));
        write(directory, KEYS_NAME, render(createKeys(sounds)));
        write(directory, SOUNDS_NAME, asSealedInterface(render(createSounds(sounds))));
        write(directory, ENTRIES_NAME, render(createEntries(sounds)));
    }

    private TypeSpec createKeys(@NotNull Map<String, SoundEventKey> sounds) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(KEYS_NAME).addModifiers(Modifier.PUBLIC);
        addJetbrainsAnnotation(builder);
        sounds.forEach((identifier, key) -> builder.addField(
                FieldSpec.builder(KEY, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.key($S, $S)", KEY, key.namespace(), key.path())
                        .build()));
        return builder.build();
    }

    private TypeSpec createSounds(@NotNull Map<String, SoundEventKey> sounds) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(SOUNDS_NAME);
        sounds.keySet().forEach(identifier -> builder.addField(
                FieldSpec.builder(CUSTOM_SOUND, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.get($T.$L)", CUSTOM_SOUND_IMPL, KEYS, identifier)
                        .build()));
        return builder.build();
    }

    private TypeSpec createEntries(@NotNull Map<String, SoundEventKey> sounds) {
        List<CodeBlock> entries = new ArrayList<>();
        sounds.keySet().forEach(identifier -> entries.add(
                CodeBlock.of("new $T($T.$L, null)", CUSTOM_SOUND_IMPL, KEYS, identifier)));

        ParameterizedTypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), CUSTOM_SOUND);
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
        return JavaFile.builder(SOUND_PACKAGE, type)
                .indent(INDENT_DEFAULT)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    /**
     * Turns the declaration of the {@code CustomSounds} interface into {@code sealed ... permits CustomSound}.
     *
     * @throws IllegalStateException if JavaPoet did not emit the expected declaration
     */
    private static String asSealedInterface(@NotNull String source) {
        String declaration = "interface " + SOUNDS_NAME + " {";
        if (!source.contains(declaration)) {
            throw new IllegalStateException("the interface " + SOUNDS_NAME + " was not found in the generated source");
        }
        return source.replace(declaration, "sealed interface " + SOUNDS_NAME + " permits CustomSound {");
    }

    private static void write(@NotNull Path directory, @NotNull String name, @NotNull String source) {
        Path file = directory.resolve(name + ".java");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, source, UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to write sound definitions to " + file, e);
        }
    }

    private static String variableNameOf(@NotNull SoundEventEntity model) {
        String variableName = model.getVariableName();
        if (variableName == null || variableName.isBlank()) {
            throw new IllegalArgumentException("sound '" + model.getKey() + "' has no variable name");
        }
        return variableName;
    }

    @Override
    public @NotNull String getName() {
        return "SoundGenerator";
    }
}
