package net.theevilreaper.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.minestom.server.attribute.Attribute;
import net.theevilreaper.vulpes.api.model.AttributeModel;
import net.theevilreaper.vulpes.api.repository.AttributeRepository;
import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import net.theevilreaper.vulpes.generator.generation.JavaStructure;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.theevilreaper.vulpes.generator.util.Constants.INDENT_DEFAULT;

@Singleton
public class AttributeGenerator extends AbstractCodeGenerator<AttributeModel> implements JavaStructure {

    private final AttributeRepository attributeRepository;
    private final ClassName attributeClass;

    @Inject
    protected AttributeGenerator(@NotNull AttributeRepository attributeRepository) {
        super("DungeonAttributes", BASE_PACKAGE + ".attribute");
        this.attributeRepository = attributeRepository;
        this.attributeClass = ClassName.get(Attribute.class);
    }

    @Override
    protected List<AttributeModel> getModels() {
        return this.attributeRepository.findAll();
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        List<AttributeModel> attributeModels = this.getModels();

        if (attributeModels.isEmpty()) {
            logger.info("No attributes found. Skipping the generation");
            return;
        }

        Map<String, FieldSpec> fields = new HashMap<>();

        for (AttributeModel attributeModel : attributeModels) {
            if (attributeModel.modelName() == null || attributeModel.modelName().isEmpty()) continue;

            FieldSpec fieldSpec = FieldSpec.builder(attributeClass, attributeModel.name())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new \\$T(\\$S, \\$Lf, \\$Lf", attributeClass, attributeModel.name(), attributeModel.defaultValue(), attributeModel.maximumValue())
                    .build();
            fields.put(attributeModel.name(), fieldSpec);
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
        writeFiles(List.of(javaFile), javaPath);
    }

    @Override
    public @NotNull String getName() {
        return "AttributeGenerator";
    }
}
