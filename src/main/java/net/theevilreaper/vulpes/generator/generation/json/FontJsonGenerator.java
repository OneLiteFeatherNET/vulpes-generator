package net.theevilreaper.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.FontEntity;
import net.onelitefeather.vulpes.api.repository.FontRepository;
import net.theevilreaper.vulpes.generator.generation.FileGenerator;
import net.theevilreaper.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Singleton
public class FontJsonGenerator extends FileGenerator {

    private final FontRepository fontRepository;

    @Inject
    public FontJsonGenerator(@NotNull FontRepository fontRepository) {
        super("fonts.json");
        this.fontRepository = fontRepository;
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        Path filePath = javaPath.resolve(fileName);
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<FontEntity> entityList = this.fontRepository.findAll();

        if (entityList.isEmpty()) return;

        JsonArray elements = new JsonArray();

        for (FontEntity entity : entityList) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("variableName", entity.getVariableName());
            jsonObject.addProperty("provider", entity.getProvider());
            jsonObject.addProperty("mapper", entity.getMapper());
            jsonObject.addProperty("texturePath", entity.getTexturePath());
            jsonObject.addProperty("height", entity.getHeight());
            jsonObject.addProperty("ascent", entity.getAscent());

            if (entity.getChars() != null) {
                JsonArray chars = new JsonArray();
                entity.getChars().forEach(string -> chars.add(string.getLine()));
                jsonObject.add("chars", chars);
            }

            elements.add(jsonObject);
        }

        save(filePath, GsonHolder.GSON, elements);
    }

    @Override
    public @NotNull String getName() {
        return FontJsonGenerator.class.getSimpleName();
    }
}
