package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.ItemEntity;
import net.onelitefeather.vulpes.api.repository.ItemRepository;
import net.onelitefeather.vulpes.generator.generation.FileGenerator;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Singleton
public class ItemJsonGenerator extends FileGenerator {

    private final ItemRepository itemRepository;

    @Inject
    public ItemJsonGenerator(@NotNull ItemRepository itemRepository) {
        super("items.json");
        this.itemRepository = itemRepository;
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        Path filePath = javaPath.resolve(fileName);
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<ItemEntity> entities = this.itemRepository.findAll();

        if (entities.isEmpty()) return;

        JsonArray elements = new JsonArray();

        for (ItemEntity entity : entities) {
            JsonObject object = new JsonObject();
            object.addProperty("variableName", entity.getVariableName());
            object.addProperty("displayName", entity.getDisplayName());
            object.addProperty("material", entity.getMaterial());
            object.addProperty("groupName", entity.getGroupName());
            object.addProperty("customModelData", entity.getCustomModelData());
            object.addProperty("amount", entity.getAmount());
            //TODO: Add enchantments, lore and flags at a later point
            elements.add(object);
        }

        save(filePath, GsonHolder.GSON, elements);
    }

    @Override
    public @NotNull String getName() {
        return ItemRepository.class.getSimpleName();
    }
}
