package net.onelitefeather.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.enchant.Enchantment;
import net.onelitefeather.vulpes.api.model.ItemEntity;
import net.onelitefeather.vulpes.api.repository.ItemRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import net.onelitefeather.vulpes.generator.util.MaterialParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;

@Singleton
public final class ItemGenerator extends AbstractCodeGenerator<ItemEntity> implements JavaStructure {

    private final ItemRepository itemRepository;

    @Inject
    public ItemGenerator(@NotNull ItemRepository itemRepository) {
        super("ItemRegistry", BASE_PACKAGE + ".item");
        this.itemRepository = itemRepository;
    }

    @Override
    protected List<ItemEntity> getModels() {
        return this.itemRepository.findAll();
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        List<ItemEntity> models = getModels();

        if (models.isEmpty()) return;

        // this.classSpec.addJavadoc(defaultDocumentation)
        addClassModifiers(this.classBuilder);
        addJetbrainsAnnotation(this.classBuilder);
        addPrivateDefaultConstructor(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);
        Map<String, FieldSpec> itemFields = new HashMap<>();

        ClassName itemStackClass = ClassName.get(ItemStack.class);
        ClassName materialClass = ClassName.get(Material.class);

        models.stream().filter(itemModel -> {
                    String variableName = itemModel.getVariableName();
                    return variableName != null && !variableName.isEmpty();
                })
                .forEach(model -> {
                    if (itemFields.containsKey(model.getVariableName())) return;
                    Material material = MaterialParser.fromKey(model.getMaterial());

                    var initBlock = CodeBlock.builder();

                    initBlock.add(
                            "\\$T.builder(\\$T.\\$L)",
                            itemStackClass,
                            materialClass,
                            material
                    );
                    if (model.getAmount() != 0) {
                        initBlock.add(".amount(\\$L)", model.getAmount());
                    }

                          /*  if (model.hasMetadata()) {
                                val metadata = CodeBlock.builder();

                                metadata.add(".meta($META_DATA_VARIABLE -> ");
                                metadata.indent();
                                metadata.add("$META_DATA_VARIABLE.");

                                if (model.customModelId != null) {
                                    metadata.add("customModelData(\$L)", model.customModelId);
                                }

                                if (model.hasDisplayName()) {
                                    metadata.add(".displayName(\$T.text(\$S))", Component::class.java, getDisplayName(model));
                                }

                                if (model.hasFlags()) {
                                    metadata.add(buildItemFlags(model.flags!!));
                                }

                                if (model.hasLoreLines()) {
                                    metadata.add(buildLore(model.lore!!));
                                }

                                if (model.hasEnchantments()) {
                                    metadata.add(buildEnchantments(model.enchantments!!));
                                }

                                metadata.unindent();
                                metadata.add(")");
                                init.add(metadata.build());
                            }*/
                    initBlock.add(".build()");
                    itemFields.put(model.getVariableName(), FieldSpec.builder(itemStackClass, model.getVariableName())
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer(initBlock.build())
                            .build());
                });

        filesToGenerate.clear();
        this.classBuilder.addFields(itemFields.values());
        filesToGenerate.add(
                JavaFile.builder(packageName, this.classBuilder.build())
                        .indent(INDENT_DEFAULT)
                        .build()
        );
        writeFiles(javaPath);
    }

    @Override
    public @NotNull String getName() {
        return "ItemGenerator";
    }

    /**
     * Generates the item fields for the given models.
     *
     * @param itemModel the models to generate the fields for
     * @return the generated fields
     */
    private @NotNull String getDisplayName(@NotNull ItemEntity itemModel) {
        var displayName = itemModel.getDisplayName() == null ? "" : itemModel.getDisplayName();
        return displayName.isEmpty() ? "No display name provided for " + itemModel.getUiName() : displayName;
    }

    /**
     * Builds the lore for the given item model.
     *
     * @param lore the lore to build
     * @return the generated lore
     */
    private @Nullable CodeBlock buildLore(@NotNull List<String> lore) {
        if (lore.isEmpty()) return null;
        var loreBlock = CodeBlock.builder();
        ClassName componentClass = ClassName.get(Component.class);
        int lastEntry = lore.size() - 1;

        loreBlock.add(".lore(\\$T.of(");
        for (int i = 0; i < lore.size(); i++) {
            loreBlock.add("\\$T.text(\\$S)", componentClass, lore.get(i));
            if (i != lastEntry) {
                loreBlock.add(", ");
            }
        }
        loreBlock.add("))");
        return loreBlock.build();
    }

    private @NotNull CodeBlock buildEnchantments(@NotNull Map<String, Short> enchantments) {
        var enchantmentBlock = CodeBlock.builder();
        for (Map.Entry<String, Short> entry : enchantments.entrySet()) {
            enchantmentBlock.add(".enchantment(");
            enchantmentBlock.add("\\$T.fromNamespaceId(\\$S)", ClassName.get(Enchantment.class), entry.getKey());
            enchantmentBlock.add(", (short) \\$L)", entry.getValue());
        }
        return enchantmentBlock.build();
    }
}
