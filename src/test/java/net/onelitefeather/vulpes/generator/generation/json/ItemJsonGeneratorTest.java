package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import net.onelitefeather.vulpes.api.model.ItemEntity;
import net.onelitefeather.vulpes.api.model.item.ItemComponentEntity;
import net.onelitefeather.vulpes.api.model.item.ItemEnchantmentEntity;
import net.onelitefeather.vulpes.api.model.item.ItemLoreEntity;
import net.onelitefeather.vulpes.api.repository.ItemRepository;
import net.onelitefeather.vulpes.generator.generation.component.ItemComponentKeys;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ItemJsonGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path tempDir;

    @Test
    void writesMaterialAndAmountFromComponents() {
        ItemEntity sword = item("MY_SWORD", "my_sword", List.of(
                component("10000000-0000-0000-0000-000000000001", ItemComponentKeys.MATERIAL, "\"minecraft:diamond_sword\""),
                component("10000000-0000-0000-0000-000000000002", ItemComponentKeys.AMOUNT, "5")
        ));

        JsonObject object = generateSingle(sword);

        assertEquals("minecraft:diamond_sword", object.get("material").getAsString(), "material must be taken from the stelaris:material component");
        assertEquals(5, object.get("amount").getAsInt(), "amount must be taken from the stelaris:amount component");
    }

    @Test
    void amountDefaultsToOneWithoutAmountComponent() {
        ItemEntity stone = item("STONE_ITEM", "stone_item", List.of(
                component("10000000-0000-0000-0000-000000000003", ItemComponentKeys.MATERIAL, "\"minecraft:stone\"")
        ));

        JsonObject object = generateSingle(stone);

        assertEquals(1, object.get("amount").getAsInt(), "amount must default to 1 when no stelaris:amount component exists");
    }

    @Test
    void loreIsOrderedByOrderIndex() {
        ItemEntity item = item("LORED", "lored", List.of());
        when(item.getLore()).thenReturn(List.of(
                new ItemLoreEntity(UUID.fromString("20000000-0000-0000-0000-000000000002"), "Zeile 2", 2),
                new ItemLoreEntity(UUID.fromString("20000000-0000-0000-0000-000000000001"), "Zeile 1", 1)
        ));

        JsonObject object = generateSingle(item);

        JsonArray lore = object.getAsJsonArray("lore");
        assertEquals(2, lore.size(), "lore must contain every line of the item");
        assertEquals("Zeile 1", lore.get(0).getAsString(), "lore line with the lower orderIndex must come first");
        assertEquals("Zeile 2", lore.get(1).getAsString(), "lore line with the higher orderIndex must come second");
    }

    @Test
    void enchantmentsAreMappedByNameToLevel() {
        ItemEntity item = item("ENCHANTED", "enchanted", List.of());
        when(item.getEnchantments()).thenReturn(List.of(
                new ItemEnchantmentEntity(UUID.fromString("30000000-0000-0000-0000-000000000001"), "minecraft:sharpness", (short) 5, false)
        ));

        JsonObject object = generateSingle(item);

        JsonObject enchantments = object.getAsJsonObject("enchantments");
        assertEquals(1, enchantments.size(), "enchantments must contain one entry per enchantment");
        assertEquals(5, enchantments.get("minecraft:sharpness").getAsInt(), "enchantment level must be stored under its name");
    }

    @Test
    void componentsAreWrittenAsParsedJsonObject() {
        ItemEntity item = item("TITLED", "titled", List.of(
                component("40000000-0000-0000-0000-000000000001", ItemComponentKeys.MATERIAL, "\"minecraft:diamond_sword\""),
                component("40000000-0000-0000-0000-000000000002", "minecraft:custom_name", "{\"text\":\"Schwert\"}")
        ));

        JsonObject object = generateSingle(item);

        JsonElement components = object.get("components");
        assertTrue(components.isJsonObject(), "components must be a JSON object and not a string");
        JsonElement customName = components.getAsJsonObject().get("minecraft:custom_name");
        assertTrue(customName.isJsonObject(), "component value must be the parsed JSON object and not its raw text");
        assertEquals("Schwert", customName.getAsJsonObject().get("text").getAsString(), "parsed component value must keep its fields");
    }

    @Test
    void stelarisComponentsAreAbsentFromComponents() {
        ItemEntity item = item("FILTERED", "filtered", List.of(
                component("50000000-0000-0000-0000-000000000001", ItemComponentKeys.MATERIAL, "\"minecraft:stone\""),
                component("50000000-0000-0000-0000-000000000002", ItemComponentKeys.AMOUNT, "2"),
                component("50000000-0000-0000-0000-000000000003", "minecraft:max_stack_size", "16")
        ));

        JsonObject object = generateSingle(item);

        JsonObject components = object.getAsJsonObject("components");
        assertFalse(components.has(ItemComponentKeys.MATERIAL), "stelaris:material must not appear in components");
        assertFalse(components.has(ItemComponentKeys.AMOUNT), "stelaris:amount must not appear in components");
        assertTrue(components.has("minecraft:max_stack_size"), "vanilla components must still appear in components");
    }

    @Test
    void nullFieldsAreOmittedFromOutput() {
        ItemEntity item = item("NO_UI", "no_ui", List.of());
        when(item.getUiName()).thenReturn(null);

        JsonObject object = generateSingle(item);

        assertFalse(object.has("uiName"), "a null uiName must be left out of the output");
    }

    @Test
    void itemsAreSortedByVariableName() {
        ItemEntity zeta = item("ZETA", "zeta", List.of());
        ItemEntity alpha = item("ALPHA", "alpha", List.of());

        JsonArray array = generate(List.of(zeta, alpha));

        assertEquals("ALPHA", array.get(0).getAsJsonObject().get("variableName").getAsString(), "items must be sorted by variableName, first entry");
        assertEquals("ZETA", array.get(1).getAsJsonObject().get("variableName").getAsString(), "items must be sorted by variableName, second entry");
    }

    private JsonObject generateSingle(ItemEntity item) {
        JsonArray array = generate(List.of(item));
        assertEquals(1, array.size(), "exactly one item must be written");
        return array.get(0).getAsJsonObject();
    }

    private JsonArray generate(List<ItemEntity> items) {
        ItemRepository repository = Mockito.mock(ItemRepository.class);
        when(repository.findByProjectId(any(UUID.class), any(Pageable.class)))
                .thenReturn(Page.of(items, Pageable.UNPAGED, (long) items.size()));

        new ItemJsonGenerator(repository).generate(tempDir, PROJECT_ID);

        Path file = tempDir.resolve("src/main/resources/items.json");
        assertTrue(Files.exists(file), "items.json must be created in the target directory");
        try (var reader = Files.newBufferedReader(file, UTF_8)) {
            return GsonHolder.GSON.fromJson(reader, JsonArray.class);
        } catch (Exception e) {
            throw new AssertionError("items.json must be valid JSON", e);
        }
    }

    private static ItemEntity item(String variableName, String key, List<ItemComponentEntity> components) {
        ItemEntity item = Mockito.mock(ItemEntity.class);
        when(item.getVariableName()).thenReturn(variableName);
        when(item.getKey()).thenReturn(key);
        when(item.getUiName()).thenReturn("UI " + variableName);
        when(item.getGroupName()).thenReturn("test_group");
        when(item.getComponents()).thenReturn(components);
        when(item.getEnchantments()).thenReturn(List.of());
        when(item.getLore()).thenReturn(List.of());
        return item;
    }

    private static ItemComponentEntity component(String id, String key, String json) {
        return new ItemComponentEntity(UUID.fromString(id), key, json);
    }
}
