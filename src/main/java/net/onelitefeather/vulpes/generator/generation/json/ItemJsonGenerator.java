package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.ItemEntity;
import net.onelitefeather.vulpes.api.model.item.ItemEnchantmentEntity;
import net.onelitefeather.vulpes.api.model.item.ItemLoreEntity;
import net.onelitefeather.vulpes.api.repository.ItemRepository;
import net.onelitefeather.vulpes.generator.generation.FileGenerator;
import net.onelitefeather.vulpes.generator.generation.component.ItemComponents;
import net.onelitefeather.vulpes.generator.generation.component.RawItemComponent;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Singleton
public class ItemJsonGenerator extends FileGenerator {

    private final ItemRepository itemRepository;

    @Inject
    public ItemJsonGenerator(@NotNull ItemRepository itemRepository) {
        super("items.json");
        this.itemRepository = itemRepository;
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path javaPath, @NotNull UUID projectId) {
        Path filePath = javaPath.resolve(fileName);
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<ItemEntity> entities = this.itemRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();

        if (entities.isEmpty()) return;

        JsonArray elements = new JsonArray();

        entities.stream()
                .sorted(Comparator.comparing(ItemEntity::getVariableName, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ItemJsonGenerator::toJson)
                .forEach(elements::add);

        save(filePath, GsonHolder.GSON, elements);
    }

    private static JsonObject toJson(ItemEntity entity) {
        ItemComponents components = ItemComponents.from(entity.getComponents());

        JsonObject object = new JsonObject();
        object.addProperty("variableName", entity.getVariableName());
        object.addProperty("key", entity.getKey());
        object.addProperty("uiName", entity.getUiName());
        object.addProperty("groupName", entity.getGroupName());
        object.addProperty("material", components.material().key().asString());
        object.addProperty("amount", components.amount());

        JsonArray lore = new JsonArray();
        if (entity.getLore() != null) {
            entity.getLore().stream()
                    .sorted()
                    .map(ItemLoreEntity::getText)
                    .forEach(lore::add);
        }
        object.add("lore", lore);

        JsonObject enchantments = new JsonObject();
        if (entity.getEnchantments() != null) {
            entity.getEnchantments().stream()
                    .sorted(Comparator.comparing(ItemEnchantmentEntity::getName))
                    .forEach(enchantment -> enchantments.addProperty(enchantment.getName(), enchantment.getLevel()));
        }
        object.add("enchantments", enchantments);

        JsonObject componentObject = new JsonObject();
        for (RawItemComponent component : components.components()) {
            componentObject.add(component.key(), JsonParser.parseString(component.jsonValue()));
        }
        object.add("components", componentObject);

        return object;
    }

    @Override
    public @NotNull String getName() {
        return ItemRepository.class.getSimpleName();
    }
}
