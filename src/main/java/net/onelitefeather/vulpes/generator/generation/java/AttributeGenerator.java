package net.onelitefeather.vulpes.generator.generation.java;

import net.onelitefeather.vulpes.generator.generation.JavaIdentifiers;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import net.minestom.server.entity.attribute.Attribute;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.api.repository.AttributeRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

@Prototype
public class AttributeGenerator extends AbstractCodeGenerator<AttributeEntity> implements JavaStructure {

    private final AttributeRepository attributeRepository;
    private final ClassName attributeClass;

    @Inject
    protected AttributeGenerator(@NotNull AttributeRepository attributeRepository) {
        super("DungeonAttributes", BASE_PACKAGE + ".attribute");
        this.attributeRepository = attributeRepository;
        this.attributeClass = ClassName.get(Attribute.class);
    }

    @Override
    protected List<AttributeEntity> getModels(@NotNull UUID projectId) {
        return this.attributeRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        List<AttributeEntity> attributeModels = this.getModels(projectId);

        if (attributeModels.isEmpty()) {
            logger.info("No attributes found. Skipping the generation");
            return;
        }

        Map<String, FieldSpec> fields = new TreeMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();

        for (AttributeEntity attributeModel : attributeModels) {
            String variableName = variableNameOf(attributeModel);
            String identifier = fieldNames.claim(JavaIdentifiers.constantName(variableName), attributeModel.getKey());

            FieldSpec fieldSpec = FieldSpec.builder(attributeClass, identifier)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T($S, $Lf, $Lf)",
                            attributeClass,
                            variableName,
                            attributeModel.getDefaultValue(),
                            attributeModel.getMaximumValue()
                    )
                    .build();
            fields.put(identifier, fieldSpec);
        }

        this.classBuilder.addFields(fields.values());
        addClassModifiers(this.classBuilder);
        addJetbrainsAnnotation(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);
        addPrivateDefaultConstructor(this.classBuilder);

        JavaFile javaFile = JavaFile.builder(this.packageName, this.classBuilder.build())
                .indent(INDENT_DEFAULT)
                .addStaticImport(attributeClass, "*")
                .build();
        writeFiles(List.of(javaFile), root.resolve(JAVA_MAIM_FOLDER));
    }

    private static String variableNameOf(@NotNull AttributeEntity model) {
        String variableName = model.getVariableName();
        if (variableName == null || variableName.isBlank()) {
            throw new IllegalArgumentException("attribute '" + model.getKey() + "' has no variable name");
        }
        return variableName;
    }

    @Override
    public @NotNull String getName() {
        return "AttributeGenerator";
    }
}
