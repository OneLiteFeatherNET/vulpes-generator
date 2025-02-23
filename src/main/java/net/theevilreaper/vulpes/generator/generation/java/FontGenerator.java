package net.theevilreaper.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.reaper.vulpes.font.FontSymbol;
import net.theevilreaper.vulpes.api.model.FontModel;
import net.theevilreaper.vulpes.api.repository.FontRepository;
import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import net.theevilreaper.vulpes.generator.generation.JavaStructure;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
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
    protected List<FontModel> getModels() {
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
            if (model.name() == null || model.name().isEmpty() || model.chars().isEmpty()) continue;

            var fontCode = CodeBlock.builder();

            fontCode.add("\\$T.builder()", FontSymbol.class);
            fontCode.add(".symbols(\\$L)", model.chars().toArray());

            if (model.ascent() != 0) {
                fontCode.add(".ascent(\\$L)", model.ascent());
            }

            if (model.height() != 0) {
                fontCode.add(".height(\\$L)", model.height());
            }

            if (model.shift() != null && !model.shift().isEmpty()) {
                fontCode.add(".shift(\\$L)", model.shift());
            }

            FieldSpec fieldValue = FieldSpec.builder(fontClass, model.name()).build();
            fields.put(model.name(), fieldValue);
        }

        return fields;
    }

    @Override
    public @NotNull String getName() {
        return "FontGenerator";
    }
}
