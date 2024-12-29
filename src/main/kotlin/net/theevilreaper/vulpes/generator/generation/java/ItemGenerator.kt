package net.theevilreaper.vulpes.generator.generation.java

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.kyori.adventure.text.Component
import net.minestom.server.item.Enchantment
import net.minestom.server.item.ItemHideFlag
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.theevilreaper.vulpes.api.model.ItemModel
import net.theevilreaper.vulpes.api.repository.ItemRepository
import net.theevilreaper.vulpes.api.util.*
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.util.BASE_PACKAGE
import net.theevilreaper.vulpes.generator.util.JavaGenerationHelper
import net.theevilreaper.vulpes.generator.util.INDENT_DEFAULT
import net.theevilreaper.vulpes.generator.util.META_DATA_VARIABLE
import java.nio.file.Path

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
@Singleton
class ItemGenerator @Inject constructor(
    val itemRepository: ItemRepository
) : JavaGenerationHelper, BaseGenerator<ItemModel>(
    className = "ItemRegistry",
    packageName = "$BASE_PACKAGE.item",
) {
    override fun generate(javaPath: Path) {
        val models = getModels()

        if (models.isEmpty()) return

        this.classSpec.addJavadoc(defaultDocumentation)
        addClassModifiers(this.classSpec)
        addJetbrainsAnnotation(this.classSpec)
        addPrivateDefaultConstructor(this.classSpec)
        addSuppressAnnotation(this.classSpec)
        val itemFields: MutableMap<String, FieldSpec> = mutableMapOf()

        models.filter { it.name.orEmpty().trim().isNotEmpty() }.forEach { model ->
            //Models can be ignored when the namespace aka the material is null or include not the valid namespace
            if (itemFields.isNotEmpty() && itemFields.containsKey(model.name!!)) return@forEach

            val material = if (model.hasMaterial()) {
                Material.fromNamespaceId(model.material!!) ?: Material.STONE
            } else {
                Material.STONE
            }
            val init = CodeBlock.builder()

            init.add(
                "\$T.builder(\$T.\$L)",
                ItemStack::class.java,
                Material::class.java,
                material.namespace().path().uppercase(),
            )

            if (model.amount != null) {
                init.add(".amount(\$L)", model.getAmount())
            }

            if (model.hasMetaData()) {
                val metadata = CodeBlock.builder()

                metadata.add(".meta($META_DATA_VARIABLE -> ")
                metadata.indent()
                metadata.add("$META_DATA_VARIABLE.")

                if (model.customModelId != null) {
                    metadata.add("customModelData(\$L)", model.customModelId)
                }

                if (model.hasDisplayName()) {
                    metadata.add(".displayName(\$T.text(\$S))", Component::class.java, getDisplayName(model))
                }

                if (model.hasFlags()) {
                    metadata.add(buildItemFlags(model.flags!!))
                }

                if (model.hasLoreLines()) {
                    metadata.add(buildLore(model.lore!!))
                }

                if (model.hasEnchantments()) {
                    metadata.add(buildEnchantments(model.enchantments!!))
                }

                metadata.unindent()
                metadata.add(")")
                init.add(metadata.build())
            }
            init.add(".build()")

            itemFields[model.name!!] = FieldSpec.builder(ItemStack::class.java, model.name!!.uppercase())
                .addModifiers(*defaultModifiers)
                .initializer(init.build())
                .build()

        }
        filesToGenerate.clear()
        this.classSpec.addFields(itemFields.values)
        filesToGenerate.add(
            JavaFile.builder(packageName, this.classSpec.build()).indent(INDENT_DEFAULT).build()
        )
        writeFiles(javaPath)
    }

    override fun getName(): String = "ItemGenerator"

    override fun getModels(): List<ItemModel> = itemRepository.findAll()

    private fun getDisplayName(model: ItemModel): String {
        val displayName = model.displayName.orEmpty()
        return when(displayName.isNotEmpty()) {
            true -> displayName
            false -> "No display name provided for ${model.name}"
        }
    }

    private fun buildLore(lore: List<String>): CodeBlock {
        val block = CodeBlock.builder()
        val lastEntry = lore.size - 1
        block.add(".lore(\$T.of(")
        lore.forEachIndexed { index, loreEntry ->
            block.add("\$T.text(\$S)", Component::class.java, loreEntry)
            if (index < lastEntry) {
                block.add(",")
            }
        }
        block.add("))")
        return block.build()
    }

    private fun buildEnchantments(enchantments: Map<String, Short>): CodeBlock {
        val block = CodeBlock.builder()
        enchantments.entries.forEach { entry ->
            block.add(".enchantment(")
            block.add("\$T.fromNamespaceId(\$S)", Enchantment::class.java, entry.key)
            block.add(", (short) \$L", entry.value)
        }

        return block.build()
    }

    private fun buildItemFlags(itemFlags: List<String>): CodeBlock {
        val codeBlock = CodeBlock.builder()
        codeBlock.add(".hideFlag(")
        val lastEntry = itemFlags.size - 1
        itemFlags.forEachIndexed { index, flag ->
            codeBlock.add("\$T.\$L", ItemHideFlag::class.java, ItemHideFlag.valueOf(flag).name)
            if (index < lastEntry) {
                codeBlock.add(", ")
            }
        }
        codeBlock.add(")")
        return codeBlock.build()
    }
}
