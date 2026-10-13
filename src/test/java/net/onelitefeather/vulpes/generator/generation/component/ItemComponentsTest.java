package net.onelitefeather.vulpes.generator.generation.component;

import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.item.ItemComponentEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemComponentsTest {

    private static ItemComponentEntity entity(String key, String value) {
        return new ItemComponentEntity(UUID.randomUUID(), key, value);
    }

    @Test
    void readsMaterialAndAmountFromComponents() {
        ItemComponents components = ItemComponents.from(List.of(
                entity(ItemComponentKeys.MATERIAL, "\"minecraft:stone\""),
                entity(ItemComponentKeys.AMOUNT, "16")
        ));

        assertEquals(Material.STONE, components.material(), "The material should be read from the stelaris:material component");
        assertEquals(16, components.amount(), "The amount should be read from the stelaris:amount component");
    }

    @Test
    void usesDefaultsWhenNoComponentsExist() {
        ItemComponents components = ItemComponents.from(List.of());

        assertEquals(ItemComponentKeys.DEFAULT_MATERIAL, components.material(), "A missing material should default to DIRT");
        assertEquals(ItemComponentKeys.DEFAULT_AMOUNT, components.amount(), "A missing amount should default to 1");
        assertTrue(components.components().isEmpty(), "No raw components should exist without entities");
    }

    @Test
    void usesDefaultsWhenCollectionIsNull() {
        ItemComponents components = ItemComponents.from(null);

        assertEquals(ItemComponentKeys.DEFAULT_MATERIAL, components.material(), "A null collection should default the material to DIRT");
        assertEquals(ItemComponentKeys.DEFAULT_AMOUNT, components.amount(), "A null collection should default the amount to 1");
        assertTrue(components.components().isEmpty(), "A null collection should yield no raw components");
    }

    @Test
    void usesDefaultMaterialWhenMaterialKeyIsMissing() {
        ItemComponents components = ItemComponents.from(List.of(
                entity(ItemComponentKeys.AMOUNT, "5")
        ));

        assertEquals(ItemComponentKeys.DEFAULT_MATERIAL, components.material(), "A missing stelaris:material key should default to DIRT");
        assertEquals(5, components.amount(), "The amount should still be read when the material is missing");
    }

    @Test
    void usesDefaultAmountWhenAmountKeyIsMissing() {
        ItemComponents components = ItemComponents.from(List.of(
                entity(ItemComponentKeys.MATERIAL, "\"minecraft:basalt\"")
        ));

        assertEquals(Material.BASALT, components.material(), "The material should still be read when the amount is missing");
        assertEquals(ItemComponentKeys.DEFAULT_AMOUNT, components.amount(), "A missing stelaris:amount key should default to 1");
    }

    @Test
    void treatsNullMaterialValueAsMissingKey() {
        ItemComponents components = ItemComponents.from(List.of(
                entity(ItemComponentKeys.MATERIAL, null)
        ));

        assertEquals(ItemComponentKeys.DEFAULT_MATERIAL, components.material(), "A null material value should be handled like a missing key");
    }

    @Test
    void treatsNullAmountValueAsMissingKey() {
        ItemComponents components = ItemComponents.from(List.of(
                entity(ItemComponentKeys.AMOUNT, null)
        ));

        assertEquals(ItemComponentKeys.DEFAULT_AMOUNT, components.amount(), "A null amount value should be handled like a missing key");
    }

    @Test
    void skipsVanillaComponentWithNullValue() {
        ItemComponents components = ItemComponents.from(List.of(
                entity("minecraft:custom_name", null)
        ));

        assertTrue(components.components().isEmpty(), "A component with a null value should be skipped");
    }

    @Test
    void rejectsUnknownMaterialWithKeyAndValueInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ItemComponents.from(List.of(
                entity(ItemComponentKeys.MATERIAL, "\"minecraft:not_a_material\"")
        )), "An unknown material should throw an IllegalArgumentException");

        assertTrue(exception.getMessage().contains(ItemComponentKeys.MATERIAL), "The message should name the key but was: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("minecraft:not_a_material"), "The message should name the value but was: " + exception.getMessage());
    }

    @Test
    void rejectsAmountOutsideRangeWithKeyAndValueInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ItemComponents.from(List.of(
                entity(ItemComponentKeys.AMOUNT, "100")
        )), "An amount above 99 should throw an IllegalArgumentException");

        assertTrue(exception.getMessage().contains(ItemComponentKeys.AMOUNT), "The message should name the key but was: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("100"), "The message should name the value but was: " + exception.getMessage());
    }

    @Test
    void rejectsMalformedJsonWithKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ItemComponents.from(List.of(
                entity(ItemComponentKeys.AMOUNT, "{")
        )), "Malformed JSON should throw an IllegalArgumentException");

        assertTrue(exception.getMessage().contains(ItemComponentKeys.AMOUNT), "The message should name the key but was: " + exception.getMessage());
    }

    @Test
    void rejectsDuplicateComponentKeys() {
        assertThrows(IllegalArgumentException.class, () -> ItemComponents.from(List.of(
                entity("minecraft:custom_name", "\"a\""),
                entity("minecraft:custom_name", "\"b\"")
        )), "Two components with the same key should be rejected");
    }

    @Test
    void filtersOutStelarisNamespaceKeys() {
        ItemComponents components = ItemComponents.from(List.of(
                entity(ItemComponentKeys.MATERIAL, "\"minecraft:stone\""),
                entity(ItemComponentKeys.AMOUNT, "2"),
                entity("stelaris:other", "true"),
                entity("minecraft:custom_name", "\"Name\"")
        ));

        List<String> keys = components.components().stream().map(RawItemComponent::key).toList();
        assertEquals(List.of("minecraft:custom_name"), keys, "Only non-stelaris components should be kept");
    }

    @Test
    void keepsRawJsonValueOfVanillaComponent() {
        ItemComponents components = ItemComponents.from(List.of(
                entity("minecraft:custom_model_data", "{\"floats\":[1.5]}")
        ));

        assertEquals(new RawItemComponent("minecraft:custom_model_data", "{\"floats\":[1.5]}"), components.components().getFirst(),
                "A vanilla component should be kept with its unchanged JSON text");
    }

    @Test
    void sortsRemainingComponentsByKey() {
        ItemComponents components = ItemComponents.from(List.of(
                entity("minecraft:max_stack_size", "16"),
                entity("minecraft:custom_name", "\"Name\""),
                entity("minecraft:custom_model_data", "{\"floats\":[1.0]}")
        ));

        List<String> keys = components.components().stream().map(RawItemComponent::key).toList();
        assertEquals(List.of("minecraft:custom_model_data", "minecraft:custom_name", "minecraft:max_stack_size"), keys,
                "Remaining components should be sorted by key");
    }
}
