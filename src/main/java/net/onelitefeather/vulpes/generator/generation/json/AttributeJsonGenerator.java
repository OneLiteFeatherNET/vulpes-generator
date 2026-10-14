package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.api.repository.AttributeRepository;
import net.onelitefeather.vulpes.generator.generation.FileGenerator;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import static net.onelitefeather.vulpes.generator.util.Constants.RESOURCES_FOLDER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Generates the {@code attributes.json} resource. Every attribute becomes one entry with its namespaced key, its ui name,
 * its default and its maximum value. The entries are sorted by the namespaced key.
 * <p>
 * The file is overwritten on every run, so the generator can run more than once into the same root.
 */
@Singleton
public class AttributeJsonGenerator extends FileGenerator {

    private final AttributeRepository attributeRepository;

    @Inject
    public AttributeJsonGenerator(@NotNull AttributeRepository attributeRepository) {
        super("attributes.json");
        this.attributeRepository = attributeRepository;
    }

    @Override
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        Path resourcesFolder = root.resolve(RESOURCES_FOLDER);
        Path filePath = resourcesFolder.resolve(fileName);
        try {
            Files.createDirectories(resourcesFolder);
        } catch (IOException e) {
            throw new IllegalStateException("unable to create " + resourcesFolder, e);
        }

        List<AttributeEntity> entities = this.attributeRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent().stream()
                .sorted(Comparator.comparing(AttributeEntity::getNamespacedKey))
                .toList();

        JsonArray elements = new JsonArray();
        for (AttributeEntity entity : entities) {
            if (entity.getUiName() == null || entity.getUiName().isBlank()) {
                throw new IllegalArgumentException("attribute '" + entity.getKey() + "' has no ui name");
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("variableName", entity.getVariableName());
            jsonObject.addProperty("key", entity.getNamespacedKey());
            jsonObject.addProperty("uiName", entity.getUiName());
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
