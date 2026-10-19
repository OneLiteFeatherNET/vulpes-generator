package net.onelitefeather.vulpes.generator.generation.java;

import net.onelitefeather.vulpes.generator.generation.JavaIdentifiers;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.RegistryKey;
import net.onelitefeather.vulpes.api.model.ItemEntity;
import net.onelitefeather.vulpes.api.model.item.ItemEnchantmentEntity;
import net.onelitefeather.vulpes.api.model.item.ItemLoreEntity;
import net.onelitefeather.vulpes.api.repository.ItemRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import net.onelitefeather.vulpes.generator.generation.component.ItemComponentKeys;
import net.onelitefeather.vulpes.generator.generation.component.ItemComponents;
import net.onelitefeather.vulpes.generator.generation.component.RawItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

/**
 * Generates the {@code ItemRegistry} class which holds one {@link ItemStack} field per item of a project.
 * <p>
 * Material, amount, lore and enchantments are emitted as builder calls. All other data components are emitted
 * as raw JSON pairs and applied at runtime by the generated {@code withComponents} helper. Every component is
 * decoded with the Minestom codecs at generation time, so invalid data aborts the generation instead of
 * ending up in the generated code.
 * <p>
 * The generation runs read only inside a transaction, because the components of an item are loaded lazily.
 */
/**
 * Generates the code-based custom item registry of vulpes-base as three top-level files in
 * {@code net.onelitefeather.vulpes.item}:
 * <ul>
 *     <li>{@code CustomItemKeys}: one {@code Key} constant per item,</li>
 *     <li>{@code CustomItems}: one {@code CustomItem} constant per item, resolved through the registry,</li>
 *     <li>{@code CustomItemEntries}: the list of all items the registry is built from.</li>
 * </ul>
 * The key of an item is the key of its project as namespace and the key of the item as path. The constants are
 * sorted by their identifier. The files are written even without items.
 * <p>
 * Material, amount, lore and enchantments are emitted as builder calls. All other data components are emitted
 * as raw JSON pairs and applied at runtime by the hand written {@code ItemComponentDecoder} of vulpes-base. Every
 * component is decoded with the Minestom codecs at generation time, so invalid data aborts the generation instead
 * of ending up in the generated code.
 * <p>
 * JavaPoet 1.13 cannot emit {@code sealed ... permits}, so the declaration of {@code CustomItems} is rewritten
 * by {@link #asSealedInterface(String)}. The generation runs read only inside a transaction, because the components
 * of an item are loaded lazily.
 */
@Prototype
public class ItemGenerator extends AbstractCodeGenerator<ItemEntity> implements JavaStructure {

    private static final String ITEM_PACKAGE = BASE_PACKAGE + ".item";
    private static final String KEYS_NAME = "CustomItemKeys";
    private static final String ITEMS_NAME = "CustomItems";
    private static final String ENTRIES_NAME = "CustomItemEntries";
    private static final int MAX_MAP_OF_ENTRIES = 10;

    private static final ClassName KEY = ClassName.get(Key.class);
    private static final ClassName KEYS = ClassName.get(ITEM_PACKAGE, KEYS_NAME);
    private static final ClassName CUSTOM_ITEM = ClassName.get(ITEM_PACKAGE, "CustomItem");
    private static final ClassName CUSTOM_ITEM_IMPL = ClassName.get(ITEM_PACKAGE, "CustomItemImpl");
    private static final ClassName ITEM_COMPONENT_DECODER = ClassName.get(ITEM_PACKAGE, "ItemComponentDecoder");
    private static final ClassName ITEM_STACK = ClassName.get(ItemStack.class);
    private static final ClassName MATERIAL = ClassName.get(Material.class);
    private static final ClassName DATA_COMPONENTS = ClassName.get(DataComponents.class);
    private static final ClassName ENCHANTMENT = ClassName.get(Enchantment.class);
    private static final ClassName ENCHANTMENT_LIST = ClassName.get(EnchantmentList.class);
    private static final ClassName REGISTRY_KEY = ClassName.get(RegistryKey.class);
    private static final ClassName COMPONENT = ClassName.get(Component.class);
    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName MAP = ClassName.get(Map.class);

    private final ItemRepository itemRepository;

    @Inject
    public ItemGenerator(@NotNull ItemRepository itemRepository) {
        super(ITEMS_NAME, ITEM_PACKAGE, TypeSpec.Kind.INTERFACE);
        this.itemRepository = itemRepository;
    }

    @Override
    protected List<ItemEntity> getModels(@NotNull UUID projectId) {
        return this.itemRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        cleanup();
        List<ItemEntity> models = getModels(projectId);

        DynamicRegistry<Enchantment> enchantments = Registries.vanilla().enchantment();
        Map<String, ItemEntity> items = new TreeMap<>();
        Map<String, CodeBlock> initializers = new TreeMap<>();
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();

        for (ItemEntity model : models) {
            String identifier = fieldNames.claim(JavaIdentifiers.constantName(variableNameOf(model)), model.getKey());
            ItemComponents components = readComponents(model);
            items.put(identifier, model);
            initializers.put(identifier, createInitializer(model, components, enchantments));
        }

        Path directory = root.resolve(JAVA_MAIM_FOLDER).resolve(packageName.replace('.', '/'));
        write(directory, KEYS_NAME, render(createKeys(items)));
        write(directory, ITEMS_NAME, asSealedInterface(render(createItems(items))));
        write(directory, ENTRIES_NAME, render(createEntries(initializers)));
    }

    private TypeSpec createKeys(@NotNull Map<String, ItemEntity> items) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(KEYS_NAME).addModifiers(Modifier.PUBLIC);
        addJetbrainsAnnotation(builder);
        items.forEach((identifier, model) -> builder.addField(
                FieldSpec.builder(KEY, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.key($S, $S)", KEY, namespaceOf(model), model.getKey())
                        .build()));
        return builder.build();
    }

    private TypeSpec createItems(@NotNull Map<String, ItemEntity> items) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(ITEMS_NAME);
        items.keySet().forEach(identifier -> builder.addField(
                FieldSpec.builder(CUSTOM_ITEM, identifier, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.get($T.$L)", CUSTOM_ITEM_IMPL, KEYS, identifier)
                        .build()));
        return builder.build();
    }

    private TypeSpec createEntries(@NotNull Map<String, CodeBlock> initializers) {
        List<CodeBlock> entries = new ArrayList<>();
        initializers.forEach((identifier, initializer) -> entries.add(
                CodeBlock.of("new $T($T.$L, $L)", CUSTOM_ITEM_IMPL, KEYS, identifier, initializer)));

        ParameterizedTypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), CUSTOM_ITEM);
        MethodSpec.Builder method = MethodSpec.methodBuilder("entries")
                .addModifiers(Modifier.STATIC)
                .returns(listType);
        if (entries.isEmpty()) {
            method.addStatement("return $T.of()", List.class);
        } else {
            method.addStatement("return $T.of($L)", List.class, CodeBlock.join(entries, ",\n"));
        }

        return TypeSpec.classBuilder(ENTRIES_NAME)
                .addModifiers(Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                .addMethod(method.build())
                .build();
    }

    @Override
    public @NotNull String getName() {
        return "ItemGenerator";
    }

    /**
     * Reads the components of an item and validates every generic component with the Minestom codecs.
     *
     * @param model the item to read
     * @return the components of the item
     * @throws IllegalArgumentException if a component is unknown, malformed or has an invalid value
     */
    private @NotNull ItemComponents readComponents(@NotNull ItemEntity model) {
        String itemKey = model.getKey();
        ItemComponents components;
        try {
            components = ItemComponents.from(model.getComponents());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("item '" + itemKey + "': " + e.getMessage(), e);
        }
        components.components().forEach(component -> validateComponent(itemKey, component));
        return components;
    }

    /**
     * Decodes a generic component with the same Minestom API that the generated code uses at runtime.
     *
     * @param itemKey   the key of the item, used in error messages
     * @param component the component to validate
     * @throws IllegalArgumentException if the key is unknown or the JSON value cannot be decoded
     */
    private static void validateComponent(@NotNull String itemKey, @NotNull RawItemComponent component) {
        String key = component.key();
        DataComponent<?> dataComponent;
        try {
            dataComponent = DataComponent.fromKey(key);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("item '" + itemKey + "' uses malformed data component key '" + key + "'", e);
        }
        if (dataComponent == null) {
            throw new IllegalArgumentException("item '" + itemKey + "' uses unknown data component '" + key + "'");
        }

        JsonElement json;
        try {
            json = JsonParser.parseString(component.jsonValue());
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("item '" + itemKey + "' has invalid JSON for data component '" + key + "': " + component.jsonValue(), e);
        }

        Result<?> result = dataComponent.decode(Transcoder.JSON, json);
        if (!(result instanceof Result.Ok<?>)) {
            String reason = result instanceof Result.Error<?> error ? error.message() : "no value decoded";
            throw new IllegalArgumentException("item '" + itemKey + "' has an invalid value for data component '" + key + "': "
                    + component.jsonValue() + " (" + reason + ")");
        }
    }

    /**
     * Creates the initializer of an item field. The builder chain is indented one level, so the emitted code is
     * readable. Items with generic components wrap the chain into the {@code ItemComponentDecoder.apply} call.
     */
    private @NotNull CodeBlock createInitializer(@NotNull ItemEntity model, @NotNull ItemComponents components,
                                                 @NotNull DynamicRegistry<Enchantment> enchantments) {
        boolean generic = !components.components().isEmpty();
        CodeBlock.Builder init = CodeBlock.builder();

        if (generic) {
            init.add("$T.apply(\n$>", ITEM_COMPONENT_DECODER);
        }
        init.add("$T.builder($T.$L)$>", ITEM_STACK, MATERIAL, materialConstant(components));

        if (components.amount() != ItemComponentKeys.DEFAULT_AMOUNT) {
            init.add("\n.amount($L)", components.amount());
        }

        List<CodeBlock> lore = loreLines(model);
        if (!lore.isEmpty()) {
            init.add("\n.set($T.LORE, $T.of($L))", DATA_COMPONENTS, LIST, CodeBlock.join(lore, ", "));
        }

        Map<String, Integer> enchantmentLevels = enchantmentLevels(model, enchantments);
        if (!enchantmentLevels.isEmpty()) {
            init.add("\n.set($T.ENCHANTMENTS, new $T($L))", DATA_COMPONENTS, ENCHANTMENT_LIST, enchantmentMap(enchantmentLevels));
        }
        init.add("$<");

        if (generic) {
            init.add(",\n");
            boolean first = true;
            for (RawItemComponent component : components.components()) {
                if (!first) init.add(",\n");
                init.add("$S, $S", component.key(), component.jsonValue());
                first = false;
            }
            init.add(")\n.build()$<");
        } else {
            init.add("\n.build()");
        }
        return init.build();
    }

    private static @NotNull String variableNameOf(@NotNull ItemEntity model) {
        String variableName = model.getVariableName();
        if (variableName == null || variableName.isBlank()) {
            throw new IllegalArgumentException("item '" + model.getKey() + "' has no variable name");
        }
        return variableName;
    }

    private static @NotNull String materialConstant(@NotNull ItemComponents components) {
        return components.material().key().value().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the lore lines of an item sorted by their order index, empty lines are skipped.
     */
    private static @NotNull List<CodeBlock> loreLines(@NotNull ItemEntity model) {
        if (model.getLore() == null) return List.of();
        return model.getLore().stream()
                .filter(line -> line != null && line.getText() != null)
                .sorted()
                .map(ItemGenerator::loreLine)
                .toList();
    }

    private static @NotNull CodeBlock loreLine(@NotNull ItemLoreEntity line) {
        return CodeBlock.of("$T.text($S)", COMPONENT, line.getText());
    }

    /**
     * Resolves the enchantments of an item to their registry names and levels, sorted by name.
     *
     * @throws IllegalArgumentException if an enchantment is unknown to the vanilla registry
     */
    private static @NotNull Map<String, Integer> enchantmentLevels(@NotNull ItemEntity model,
                                                                   @NotNull DynamicRegistry<Enchantment> enchantments) {
        Map<String, Integer> levels = new TreeMap<>();
        if (model.getEnchantments() == null) return levels;

        for (ItemEnchantmentEntity enchantment : model.getEnchantments()) {
            RegistryKey<Enchantment> key = resolveEnchantment(model.getKey(), enchantments, enchantment.getName());
            levels.put(key.name(), (int) enchantment.getLevel());
        }
        return levels;
    }

    private static @NotNull RegistryKey<Enchantment> resolveEnchantment(@NotNull String itemKey,
                                                                        @NotNull DynamicRegistry<Enchantment> enchantments,
                                                                        String name) {
        RegistryKey<Enchantment> key;
        try {
            key = enchantments.getKey(Key.key(name));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("item '" + itemKey + "' uses malformed enchantment '" + name + "'", e);
        }
        if (key == null) {
            throw new IllegalArgumentException("item '" + itemKey + "' uses unknown enchantment '" + name + "'");
        }
        return key;
    }

    /**
     * Emits the map of enchantment keys to levels. Up to {@link #MAX_MAP_OF_ENTRIES} entries use {@code Map.of},
     * larger maps use {@code Map.ofEntries}.
     */
    private static @NotNull CodeBlock enchantmentMap(@NotNull Map<String, Integer> levels) {
        CodeBlock.Builder map = CodeBlock.builder();
        if (levels.size() <= MAX_MAP_OF_ENTRIES) {
            map.add("$T.of(", MAP);
            map.add(CodeBlock.join(levels.entrySet().stream()
                    .map(e -> CodeBlock.of("$T.<$T>of($S), $L", REGISTRY_KEY, ENCHANTMENT, e.getKey(), e.getValue()))
                    .toList(), ", "));
        } else {
            map.add("$T.ofEntries(", MAP);
            map.add(CodeBlock.join(levels.entrySet().stream()
                    .map(e -> CodeBlock.of("$T.entry($T.<$T>of($S), $L)", MAP, REGISTRY_KEY, ENCHANTMENT, e.getKey(), e.getValue()))
                    .toList(), ", "));
        }
        map.add(")");
        return map.build();
    }

    private static String render(@NotNull TypeSpec type) {
        return JavaFile.builder(ITEM_PACKAGE, type)
                .indent(INDENT_DEFAULT)
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    /**
     * Turns the declaration of the {@code CustomItems} interface into {@code sealed ... permits CustomItem}.
     *
     * @throws IllegalStateException if JavaPoet did not emit the expected declaration
     */
    private static String asSealedInterface(@NotNull String source) {
        String declaration = "interface " + ITEMS_NAME + " {";
        if (!source.contains(declaration)) {
            throw new IllegalStateException("the interface " + ITEMS_NAME + " was not found in the generated source");
        }
        return source.replace(declaration, "sealed interface " + ITEMS_NAME + " permits CustomItem {");
    }

    private static void write(@NotNull Path directory, @NotNull String name, @NotNull String source) {
        Path file = directory.resolve(name + ".java");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, source, UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to write item definitions to " + file, e);
        }
    }

    /**
     * Returns the project part of the namespaced key, for example {@code eldoria_rpg} for {@code eldoria_rpg:sword}.
     */
    private static String namespaceOf(@NotNull ItemEntity model) {
        String namespacedKey = model.getNamespacedKey();
        return namespacedKey.substring(0, namespacedKey.indexOf(':'));
    }
}
