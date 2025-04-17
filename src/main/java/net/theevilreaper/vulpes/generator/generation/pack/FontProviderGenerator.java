package net.theevilreaper.vulpes.generator.generation.pack;

import com.google.gson.JsonArray;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.api.model.FontModel;
import net.theevilreaper.vulpes.api.repository.FontRepository;
import net.theevilreaper.vulpes.generator.generation.AbstractGenerator;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

@Singleton
public final class FontProviderGenerator extends AbstractGenerator<FontModel> {

    private final FontRepository fontRepository;

    @Inject
    public FontProviderGenerator(@NotNull FontRepository fontRepository) {
        this.fontRepository = fontRepository;
    }

    @Override
    @NotNull
    protected List<FontModel> getModels() {
        return fontRepository.findAll();
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        List<FontModel> models = getModels();

        // Check if the list is empty, if so, skip the generation
        if (models.isEmpty()) return;

        JsonArray jsonArray = new JsonArray();
    }

    @Override
    public @NotNull String getName() {
        return "FontProviderGenerator";
    }
}
