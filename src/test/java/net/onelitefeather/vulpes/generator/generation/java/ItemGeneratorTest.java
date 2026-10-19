package net.onelitefeather.vulpes.generator.generation.java;

import net.onelitefeather.vulpes.generator.generation.JavaSourceAssertions;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import net.onelitefeather.vulpes.api.model.ItemEntity;
import net.onelitefeather.vulpes.api.model.item.ItemComponentEntity;
import net.onelitefeather.vulpes.api.model.item.ItemEnchantmentEntity;
import net.onelitefeather.vulpes.api.model.item.ItemLoreEntity;
import net.onelitefeather.vulpes.api.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemGeneratorTest {

    private static final UUID PROJECT_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final String REGISTRY_PATH = "src/main/java/net/reaper/vulpes/item/ItemRegistry.java";

    @TempDir
    Path tempDir;

    private ItemRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(ItemRepository.class);
    }

    @Test
    void generatesMaterialFromComponents() {
        generate(item("sword", component("stelaris:material", "\"minecraft:diamond_sword\"")));

        assertTrue(readRegistry().contains("ItemStack.builder(Material.DIAMOND_SWORD)"),
                "The material should be read from the stelaris:material component");
    }

    @Test
    void generatesAmountFromComponents() {
        generate(item("stack",
                component("stelaris:material", "\"minecraft:stone\""),
                component("stelaris:amount", "5")));

        assertTrue(readRegistry().contains(".amount(5)"),
                "A non-default amount should be emitted as an amount call");
    }

    @Test
    void omitsAmountCallWhenAmountIsDefault() {
        generate(item("single", component("stelaris:material", "\"minecraft:stone\"")));

        assertFalse(readRegistry().contains(".amount("),
                "An item with the default amount of 1 should not emit an amount call");
    }

    @Test
    void sortsLoreByOrderIndex() {
        generate(withLore(item("lore_item", component("stelaris:material", "\"minecraft:stone\"")),
                lore("Zeile 2", 2),
                lore("Zeile 1", 1)));

        String code = readRegistry();
        assertTrue(code.contains("DataComponents.LORE, List.of(Component.text(\"Zeile 1\"), Component.text(\"Zeile 2\"))"),
                "The lore lines should be emitted in orderIndex order but was:\n" + code);
    }

    @Test
    void emitsEnchantmentWithRegistryKeyAndLevel() {
        generate(withEnchantments(item("enchanted"), enchantment("minecraft:sharpness", 5)));

        assertTrue(readRegistry().contains("new EnchantmentList(Map.of(RegistryKey.<Enchantment>of(\"minecraft:sharpness\"), 5))"),
                "The enchantment should be emitted as a registry key with its level");
    }

    @Test
    void emitsGenericComponentWithEscapedJson() {
        generate(item("named",
                component("stelaris:material", "\"minecraft:stone\""),
                component("minecraft:custom_name", "{\"text\":\"Schwert\"}")));

        String code = readRegistry();
        assertTrue(code.contains("\"minecraft:custom_name\", \"{\\\"text\\\":\\\"Schwert\\\"}\""),
                "A generic component should be emitted as an escaped key and JSON string but was:\n" + code);
    }

    @Test
    void omitsWithComponentsHelperWithoutGenericComponents() {
        generate(item("plain", component("stelaris:material", "\"minecraft:stone\"")));

        String code = readRegistry();
        assertFalse(code.contains("withComponents("),
                "Without generic components neither the wrapper call nor the helper should be emitted but was:\n" + code);
    }

    @Test
    void generatedSourceContainsNoEscapedOrUnresolvedPlaceholders() {
        generate(withEnchantments(withLore(item("full",
                        component("stelaris:material", "\"minecraft:diamond_sword\""),
                        component("stelaris:amount", "5"),
                        component("minecraft:custom_name", "{\"text\":\"Schwert\"}")),
                lore("Zeile 1", 1)),
                enchantment("minecraft:sharpness", 5)));

        String code = readRegistry();
        assertFalse(code.contains("\\$"), "The source must not contain a backslash in front of a placeholder but was:\n" + code);
        assertFalse(code.contains("$"), "The source must not contain any unresolved placeholder but was:\n" + code);
    }

    @Test
    void sortsItemFieldsByVariableName() {
        generate(
                item("zeta", component("stelaris:material", "\"minecraft:stone\"")),
                item("alpha", component("stelaris:material", "\"minecraft:stone\"")),
                item("mid", component("stelaris:material", "\"minecraft:stone\""))
        );

        String code = readRegistry();
        int alpha = code.indexOf("ALPHA =");
        int mid = code.indexOf("MID =");
        int zeta = code.indexOf("ZETA =");
        assertTrue(alpha >= 0 && mid >= 0 && zeta >= 0, "All three fields should be generated but was:\n" + code);
        assertTrue(alpha < mid && mid < zeta, "The fields should be sorted by variable name but was:\n" + code);
    }

    @Test
    void rejectsUnknownComponentKeyWithKeyInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> generate(item("broken",
                component("stelaris:material", "\"minecraft:stone\""),
                component("minecraft:not_a_component", "\"x\""))),
                "An unknown data component must abort the generation");

        assertTrue(exception.getMessage().contains("minecraft:not_a_component"),
                "The message should name the unknown component but was: " + exception.getMessage());
    }

    @Test
    void rejectsUnknownEnchantmentWithNameInMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> generate(withEnchantments(item("bad_enchant"), enchantment("minecraft:not_an_enchantment", 1))),
                "An unknown enchantment must abort the generation");

        assertTrue(exception.getMessage().contains("minecraft:not_an_enchantment"),
                "The message should name the unknown enchantment but was: " + exception.getMessage());
    }

    @Test
    void generatedRegistryCompiles() throws IOException {
        generate(withEnchantments(withLore(item("compiled",
                        component("stelaris:material", "\"minecraft:diamond_sword\""),
                        component("stelaris:amount", "5"),
                        component("minecraft:custom_name", "{\"text\":\"Schwert\"}")),
                lore("Zeile 1", 1)),
                enchantment("minecraft:sharpness", 5)));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            exitCode = compiler.run(null, OutputStream.nullOutputStream(), err,
                    "-proc:none",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classesDir.toString(),
                    tempDir.resolve(REGISTRY_PATH).toString());
        }

        assertEquals(0, exitCode, "The generated ItemRegistry must compile but the compiler reported:\n" + errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void generationRunsInReadOnlyTransaction() throws NoSuchMethodException {
        boolean readOnly = ItemGenerator.class.getMethod("generate", Path.class, UUID.class).isAnnotationPresent(ReadOnly.class);

        assertTrue(readOnly, "generate must be annotated with @ReadOnly so lazy item components can be loaded");
    }

    @Test
    void keyWithDashAndDotCompilesAsJavaField() throws IOException {
        generate(item("my-item.v2", component("stelaris:material", "\"minecraft:stone\"")));

        JavaSourceAssertions.assertCompiles(tempDir, REGISTRY_PATH);
    }

    @Test
    void rejectsTwoKeysWithSameJavaIdentifierNamingBothKeys() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> generate(
                item("my-item", component("stelaris:material", "\"minecraft:stone\"")),
                item("my.item", component("stelaris:material", "\"minecraft:stone\""))),
                "Two keys that map to the same identifier must abort the generation");

        assertTrue(exception.getMessage().contains("my-item") && exception.getMessage().contains("my.item"),
                "The message must name both keys but was: " + exception.getMessage());
    }

    private void generate(ItemEntity... items) {
        when(repository.findByProjectId(eq(PROJECT_ID), any(Pageable.class)))
                .thenReturn(Page.of(List.of(items), Pageable.UNPAGED, (long) items.length));

        new ItemGenerator(repository).generate(tempDir, PROJECT_ID);
    }

    private String readRegistry() {
        try {
            return Files.readString(tempDir.resolve(REGISTRY_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("The ItemRegistry should have been generated at " + REGISTRY_PATH, e);
        }
    }

    private static ItemEntity item(String key, ItemComponentEntity... components) {
        ItemEntity entity = new ItemEntity();
        entity.setKey(key);
        entity.setLore(List.of());
        entity.setEnchantments(List.of());
        entity.setComponents(List.of(components));
        return entity;
    }

    private static ItemEntity withLore(ItemEntity entity, ItemLoreEntity... lore) {
        entity.setLore(List.of(lore));
        return entity;
    }

    private static ItemEntity withEnchantments(ItemEntity entity, ItemEnchantmentEntity... enchantments) {
        entity.setEnchantments(List.of(enchantments));
        return entity;
    }

    private static ItemComponentEntity component(String key, String jsonValue) {
        return new ItemComponentEntity(UUID.fromString("00000000-0000-0000-0000-000000000001"), key, jsonValue);
    }

    private static ItemLoreEntity lore(String text, int orderIndex) {
        return new ItemLoreEntity(UUID.fromString("00000000-0000-0000-0000-000000000002"), text, orderIndex);
    }

    private static ItemEnchantmentEntity enchantment(String name, int level) {
        return new ItemEnchantmentEntity(UUID.fromString("00000000-0000-0000-0000-000000000003"), name, (short) level, false);
    }
}
