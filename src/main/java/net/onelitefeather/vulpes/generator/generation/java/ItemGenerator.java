package net.onelitefeather.vulpes.generator.generation.java;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
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
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;

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
@Prototype
public class ItemGenerator extends AbstractCodeGenerator<ItemEntity> implements JavaStructure {

    private static final String WITH_COMPONENTS = "withComponents";
    private static final String APPLY_COMPONENT = "applyComponent";
    private static final int MAX_MAP_OF_ENTRIES = 10;

    private static final ClassName ITEM_STACK = ClassName.get(ItemStack.class);
    private static final ClassName ITEM_STACK_BUILDER = ClassName.get(ItemStack.Builder.class);
    private static final ClassName MATERIAL = ClassName.get(Material.class);
    private static final ClassName DATA_COMPONENTS = ClassName.get(DataComponents.class);
    private static final ClassName DATA_COMPONENT = ClassName.get(DataComponent.class);
    private static final ClassName ENCHANTMENT = ClassName.get(Enchantment.class);
    private static final ClassName ENCHANTMENT_LIST = ClassName.get(EnchantmentList.class);
    private static final ClassName REGISTRY_KEY = ClassName.get(RegistryKey.class);
    private static final ClassName COMPONENT = ClassName.get(Component.class);
    private static final ClassName TRANSCODER = ClassName.get(Transcoder.class);
    private static final ClassName RESULT = ClassName.get(Result.class);
    private static final ClassName JSON_ELEMENT = ClassName.get(JsonElement.class);
    private static final ClassName JSON_PARSER = ClassName.get(JsonParser.class);
    private static final ClassName JSON_PARSE_EXCEPTION = ClassName.get(JsonParseException.class);
    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName MAP = ClassName.get(Map.class);

    private final ItemRepository itemRepository;

    @Inject
    public ItemGenerator(@NotNull ItemRepository itemRepository) {
        super("ItemRegistry", BASE_PACKAGE + ".item");
        this.itemRepository = itemRepository;
    }

    @Override
    protected List<ItemEntity> getModels(@NotNull UUID projectId) {
        return this.itemRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path javaPath, @NotNull UUID projectId) {
        cleanup();
        List<ItemEntity> models = getModels(projectId);

        if (models.isEmpty()) return;

        addClassModifiers(this.classBuilder);
        addJetbrainsAnnotation(this.classBuilder);
        addPrivateDefaultConstructor(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);

        DynamicRegistry<Enchantment> enchantments = Registries.vanilla().enchantment();
        Map<String, FieldSpec> itemFields = new TreeMap<>();
        boolean needsComponentHelper = false;

        for (ItemEntity model : models) {
            String variableName = model.getVariableName();
            if (variableName == null || variableName.isEmpty() || itemFields.containsKey(variableName)) continue;

            ItemComponents components = readComponents(model);
            needsComponentHelper |= !components.components().isEmpty();
            itemFields.put(variableName, createField(model, components, enchantments));
        }

        this.classBuilder.addFields(itemFields.values());
        if (needsComponentHelper) {
            this.classBuilder.addMethod(createWithComponentsMethod());
            this.classBuilder.addMethod(createApplyComponentMethod());
        }

        filesToGenerate.clear();
        filesToGenerate.add(
                JavaFile.builder(packageName, this.classBuilder.build())
                        .indent(INDENT_DEFAULT)
                        .skipJavaLangImports(true)
                        .build()
        );
        writeFiles(javaPath);
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

    private @NotNull FieldSpec createField(@NotNull ItemEntity model, @NotNull ItemComponents components,
                                           @NotNull DynamicRegistry<Enchantment> enchantments) {
        return FieldSpec.builder(ITEM_STACK, model.getVariableName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(createInitializer(model, components, enchantments))
                .build();
    }

    /**
     * Creates the initializer of an item field. The builder chain is indented one level, so the emitted code is
     * readable. Items with generic components wrap the chain into the {@code withComponents} call.
     */
    private @NotNull CodeBlock createInitializer(@NotNull ItemEntity model, @NotNull ItemComponents components,
                                                 @NotNull DynamicRegistry<Enchantment> enchantments) {
        boolean generic = !components.components().isEmpty();
        CodeBlock.Builder init = CodeBlock.builder();

        if (generic) {
            init.add("$L(\n$>", WITH_COMPONENTS);
        }
        init.add("$T.builder($T.$L)$>", ITEM_STACK, MATERIAL, constantName(components));

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

    private static @NotNull String constantName(@NotNull ItemComponents components) {
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

    /**
     * Creates {@code private static ItemStack.Builder withComponents(ItemStack.Builder, String...)}.
     */
    private static @NotNull MethodSpec createWithComponentsMethod() {
        return MethodSpec.methodBuilder(WITH_COMPONENTS)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ITEM_STACK_BUILDER)
                .addParameter(ITEM_STACK_BUILDER, "builder")
                .addParameter(ParameterSpec.builder(String[].class, "keyAndJson").build())
                .varargs(true)
                .addStatement("if (keyAndJson.length % 2 != 0) throw new $T($S + keyAndJson.length + $S)",
                        IllegalArgumentException.class,
                        "Expected key and JSON value pairs but got ",
                        " arguments")
                .beginControlFlow("for (int i = 0; i < keyAndJson.length; i += 2)")
                .addStatement("String key = keyAndJson[i]")
                .addStatement("$T<?> component = $T.fromKey(key)", DATA_COMPONENT, DATA_COMPONENT)
                .beginControlFlow("if (component == null)")
                .addStatement("throw new $T($S + key)", IllegalStateException.class, "Unknown data component: ")
                .endControlFlow()
                .addStatement("$T json", JSON_ELEMENT)
                .beginControlFlow("try")
                .addStatement("json = $T.parseString(keyAndJson[i + 1])", JSON_PARSER)
                .nextControlFlow("catch ($T e)", JSON_PARSE_EXCEPTION)
                .addStatement("throw new $T($S + key + $S + e.getMessage(), e)", IllegalStateException.class,
                        "Invalid JSON for data component ", ": ")
                .endControlFlow()
                .addStatement("$L(builder, component, key, json)", APPLY_COMPONENT)
                .endControlFlow()
                .addStatement("return builder")
                .build();
    }

    /**
     * Creates {@code private static <T> void applyComponent(...)} which keeps the generic type of the component.
     */
    private static @NotNull MethodSpec createApplyComponentMethod() {
        TypeVariableName t = TypeVariableName.get("T");
        return MethodSpec.methodBuilder(APPLY_COMPONENT)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addTypeVariable(t)
                .returns(TypeName.VOID)
                .addParameter(ITEM_STACK_BUILDER, "builder")
                .addParameter(ParameterizedTypeName.get(DATA_COMPONENT, t), "component")
                .addParameter(String.class, "key")
                .addParameter(JSON_ELEMENT, "json")
                .addStatement("$T<$T> result = component.decode($T.JSON, json)", RESULT, t, TRANSCODER)
                .beginControlFlow("if (result instanceof $T.Ok<$T> ok)", RESULT, t)
                .addStatement("builder.set(component, ok.value())")
                .addStatement("return")
                .endControlFlow()
                .addStatement("String reason = result instanceof $T.Error<$T> error ? error.message() : $S",
                        RESULT, t, "no value decoded")
                .addStatement("throw new $T($S + key + $S + reason)", IllegalStateException.class,
                        "Unable to decode data component ", ": ")
                .build();
    }
}
