package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.advancement.AdvancementEntity;
import net.onelitefeather.vulpes.api.repository.AdvancementRepository;
import net.onelitefeather.vulpes.generator.generation.FileGenerator;
import net.onelitefeather.vulpes.generator.generation.component.AdvancementData;
import net.onelitefeather.vulpes.generator.generation.component.AdvancementTree;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import static net.onelitefeather.vulpes.generator.util.Constants.RESOURCES_FOLDER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Writes the advancements of a project as {@code advancements.json}, a JSON array in tree order.
 * <p>
 * Every entry carries the namespaced key of the advancement and of its parent, so the tree can be rebuilt from the file.
 * Titles and descriptions are written as parsed JSON, the icon as namespaced material key. All values are validated by
 * {@link AdvancementTree} before anything is written.
 */
@Singleton
public class AdvancementJsonGenerator extends FileGenerator {

    private final AdvancementRepository advancementRepository;

    @Inject
    public AdvancementJsonGenerator(@NotNull AdvancementRepository advancementRepository) {
        super("advancements.json");
        this.advancementRepository = advancementRepository;
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        Path resourcesFolder = root.resolve(RESOURCES_FOLDER);
        Path filePath = resourcesFolder.resolve(fileName);
        try {
            Files.createDirectories(resourcesFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<AdvancementEntity> entities = this.advancementRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();

        if (entities.isEmpty()) return;

        JsonArray elements = new JsonArray();
        AdvancementTree.from(entities).stream()
                .map(AdvancementJsonGenerator::toJson)
                .forEach(elements::add);

        save(filePath, GsonHolder.GSON, elements);
    }

    private static JsonObject toJson(AdvancementData advancement) {
        JsonObject object = new JsonObject();
        object.addProperty("key", advancement.namespacedKey());
        object.addProperty("variableName", advancement.variableName());
        if (advancement.uiName() != null) {
            object.addProperty("uiName", advancement.uiName());
        }
        if (advancement.parentNamespacedKey() != null) {
            object.addProperty("parent", advancement.parentNamespacedKey());
        }
        object.addProperty("frameType", advancement.frameType().name().toLowerCase(Locale.ROOT));
        object.addProperty("icon", advancement.material().key().asString());
        if (advancement.title() != null) {
            object.add("title", advancement.title());
        }
        if (advancement.description() != null) {
            object.add("description", advancement.description());
        }
        if (advancement.background() != null) {
            object.addProperty("background", advancement.background());
        }
        object.addProperty("x", advancement.x());
        object.addProperty("y", advancement.y());
        object.addProperty("showToast", advancement.showToast());
        object.addProperty("announceToChat", advancement.announceToChat());
        object.addProperty("hidden", advancement.hidden());
        return object;
    }

    @Override
    public @NotNull String getName() {
        return AdvancementJsonGenerator.class.getSimpleName();
    }
}
