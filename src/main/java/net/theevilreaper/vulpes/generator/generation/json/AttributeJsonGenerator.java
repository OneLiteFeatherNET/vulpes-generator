package net.theevilreaper.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.api.repository.AttributeRepository;
import net.theevilreaper.vulpes.generator.generation.FileGenerator;
import net.theevilreaper.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Singleton
public class AttributeJsonGenerator extends FileGenerator {

    private final AttributeRepository attributeRepository;

    @Inject
    public AttributeJsonGenerator(@NotNull AttributeRepository attributeRepository) {
        super("attributes.json");
        this.attributeRepository = attributeRepository;
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        Path filePath = javaPath.resolve(fileName);
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<AttributeEntity> entities = this.attributeRepository.findAll();

        JsonArray elements = new JsonArray();

        for (AttributeEntity entity : entities) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("variableName", entity.getVariableName());
            jsonObject.addProperty("key", entity.getVariableName());
            jsonObject.addProperty("default", entity.getDefaultValue());
            jsonObject.addProperty("maximum", entity.getMaximumValue());
            elements.add(jsonObject);
        }

        save(filePath, GsonHolder.GSON, elements);
    }

    @Override
    public @NotNull String getName() {
        return AttributeJsonGenerator.class.getSimpleName();
    }

}
