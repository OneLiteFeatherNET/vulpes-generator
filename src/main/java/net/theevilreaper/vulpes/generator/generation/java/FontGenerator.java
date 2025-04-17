package net.theevilreaper.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.api.model.FontModel;
import net.theevilreaper.vulpes.api.repository.FontRepository;
import net.theevilreaper.vulpes.font.FontSymbol;
import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import net.theevilreaper.vulpes.generator.generation.JavaStructure;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class FontGenerator extends AbstractCodeGenerator<FontModel> implements JavaStructure {

    private final FontRepository fontRepository;

    @Inject
    public FontGenerator(FontRepository fontRepository) {
        super("FontRegistry", BASE_PACKAGE + ".font");
        this.fontRepository = fontRepository;
    }

    @Override
    protected @NotNull List<FontModel> getModels() {
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

    private @NotNull Map<String, FieldSpec> generateFonts(@NotNull List<FontModel> models) {
        var fields = new HashMap<String, FieldSpec>();
        ClassName fontClass = ClassName.get(FontSymbol.class);
        for (FontModel model : models) {
            if (model.getName() == null || model.getName().isEmpty() || model.getChars().isEmpty()) continue;

            var fontCode = CodeBlock.builder();

            fontCode.add("\\$T.builder()", FontSymbol.class);
            fontCode.add(".symbols(\\$L)", model.getChars().toArray());

            if (model.getAscent() != 0) {
                fontCode.add(".ascent(\\$L)", model.getAscent());
            }

            if (model.getHeight() != 0) {
                fontCode.add(".height(\\$L)", model.getHeight());
            }

            if (model.getShift() != null && !model.getShift().isEmpty()) {
                fontCode.add(".shift(\\$L)", model.getShift());
            }

            FieldSpec fieldValue = FieldSpec.builder(fontClass, model.getName()).build();
            fields.put(model.getName(), fieldValue);
        }

        return fields;
    }

    @Override
    public @NotNull String getName() {
        return "FontGenerator";
    }
}
