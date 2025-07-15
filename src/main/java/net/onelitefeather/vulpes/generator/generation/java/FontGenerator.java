package net.onelitefeather.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.FontEntity;
import net.onelitefeather.vulpes.api.repository.FontRepository;
import net.theevilreaper.vulpes.font.FontSymbol;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class FontGenerator extends AbstractCodeGenerator<FontEntity> implements JavaStructure {

    private final FontRepository fontRepository;

    @Inject
    public FontGenerator(FontRepository fontRepository) {
        super("FontRegistry", BASE_PACKAGE + ".font");
        this.fontRepository = fontRepository;
    }

    @Override
    protected List<FontEntity> getModels() {
        return this.fontRepository.findAll();
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        var models = getModels();

        if (models.isEmpty()) return;

        var fieldSpecs = generateFonts(models).values();
        addClassModifiers(this.classBuilder);
        addJetbrainsAnnotation(this.classBuilder);
        addPrivateDefaultConstructor(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);
        this.classBuilder.addFields(fieldSpecs);
    }

    private @NotNull Map<String, FieldSpec> generateFonts(@NotNull List<FontEntity> models) {
        var fields = new HashMap<String, FieldSpec>();
        ClassName fontClass = ClassName.get(FontSymbol.class);
        for (FontEntity model : models) {
            if (model.getVariableName() == null || model.getVariableName().isEmpty() || model.getChars().isEmpty()) continue;

            var fontCode = CodeBlock.builder();

            fontCode.add("\\$T.builder()", FontSymbol.class);
            fontCode.add(".symbols(\\$L)", model.getChars().toArray());

            if (model.getAscent() != 0) {
                fontCode.add(".ascent(\\$L)", model.getAscent());
            }

            if (model.getHeight() != 0) {
                fontCode.add(".height(\\$L)", model.getHeight());
            }

            FieldSpec fieldValue = FieldSpec.builder(fontClass, model.getVariableName()).build();
            fields.put(model.getVariableName(), fieldValue);
        }

        return fields;
    }

    @Override
    public @NotNull String getName() {
        return "FontGenerator";
    }
}
