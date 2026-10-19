package net.onelitefeather.vulpes.generator.generation.java;

import net.onelitefeather.vulpes.generator.generation.JavaIdentifiers;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.vulpes.api.model.sound.SoundEventEntity;
import net.onelitefeather.vulpes.api.repository.SoundRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import net.onelitefeather.vulpes.generator.generation.sound.SoundEventKey;
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
 * Generates the {@code SoundRegistry} class which holds one {@link SoundEvent} field per sound event of a project.
 * <p>
 * Every field is created with {@code SoundEvent.of(key, null)} from the namespaced key of the event, the fields are
 * sorted by their variable name. The generation runs read only inside a transaction.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@Prototype
public class SoundGenerator extends AbstractCodeGenerator<SoundEventEntity> implements JavaStructure {

    private static final ClassName SOUND_EVENT = ClassName.get(SoundEvent.class);

    private final SoundRepository soundRepository;

    @Inject
    public SoundGenerator(@NotNull SoundRepository soundRepository) {
        super("SoundRegistry", BASE_PACKAGE + ".sound");
        this.soundRepository = soundRepository;
    }

    @Override
    protected List<SoundEventEntity> getModels(@NotNull UUID projectId) {
        return this.soundRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    private static String variableNameOf(@NotNull SoundEventEntity model) {
        String variableName = model.getVariableName();
        if (variableName == null || variableName.isBlank()) {
            throw new IllegalArgumentException("sound '" + model.getKey() + "' has no variable name");
        }
        return variableName;
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        cleanup();
        List<SoundEventEntity> models = getModels(projectId);

        if (models.isEmpty()) return;

        addClassModifiers(this.classBuilder);
        addJetbrainsAnnotation(this.classBuilder);
        addPrivateDefaultConstructor(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);

        Map<String, FieldSpec> soundFields = new TreeMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();
        for (SoundEventEntity model : models) {
            String identifier = fieldNames.claim(JavaIdentifiers.constantName(variableNameOf(model)), model.getKey());

            SoundEventKey key = SoundEventKey.of(model);
            soundFields.put(identifier, FieldSpec.builder(SOUND_EVENT, identifier)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.of($S, null)", SOUND_EVENT, key.asString())
                    .build());
        }

        this.classBuilder.addFields(soundFields.values());

        filesToGenerate.clear();
        filesToGenerate.add(
                JavaFile.builder(packageName, this.classBuilder.build())
                        .indent(INDENT_DEFAULT)
                        .skipJavaLangImports(true)
                        .build()
        );
        writeFiles(root.resolve(JAVA_MAIM_FOLDER));
    }

    @Override
    public @NotNull String getName() {
        return "SoundGenerator";
    }
}
