package net.theevilreaper.vulpes.generator.generation.java

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import net.kyori.adventure.text.Component
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.theevilreaper.vulpes.api.model.BlockModel
import net.theevilreaper.vulpes.api.repository.BlocKRepository
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.util.BASE_PACKAGE
import net.theevilreaper.vulpes.generator.util.JavaGenerationHelper
import net.theevilreaper.vulpes.generator.util.INDENT_DEFAULT
import net.theevilreaper.vulpes.generator.util.ITEM_CONST
import net.theevilreaper.vulpes.generator.util.toVariableString
import org.springframework.stereotype.Service
import java.nio.file.Path

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
@Service
class BlockGenerator(
    val blocKRepository: BlocKRepository
) : JavaGenerationHelper, BaseGenerator<BlockModel>(
    className = "CustomBlockRegistry",
    packageName = "$BASE_PACKAGE.block",
) {
    override fun generate(javaPath: Path) {
        val models = getModels()

        if (models.isEmpty()) return

        val blockField: MutableList<FieldSpec> = arrayListOf()
        val itemField: MutableList<FieldSpec> = arrayListOf()

        models.filter { it.name.orEmpty().trim().isNotEmpty() }.forEach { model ->
            val namespace = model.material
            val itemBuilder = FieldSpec.builder(
                ItemStack::class.java, "$ITEM_CONST${model.name!!.toVariableString()}"
            )
                //TODO: Improve that because the spread operator makes a full copy of an "constant array" lul
                .addModifiers(*defaultModifiers)
                .initializer(
                    "ItemStack.builder(\$T.fromNamespaceId(\$S)).displayName(\$T.text(\$S)).meta(metaBuilder -> metaBuilder.customModelData(\$L)).build()",
                    Material::class.java,
                    namespace,
                    Component::class.java,
                    model.name,
                    model.customModelId
                )
            itemField.add(itemBuilder.build())

            blockField.add(
                FieldSpec.builder(Block::class.java, model.name!!.toVariableString())
                    .addModifiers(*defaultModifiers)
                    .initializer("\$T.fromNamespaceId(\$S)", Block::class.java, namespace).build()
            )
        }
        this.classSpec.addJavadoc(defaultDocumentation)
        addClassModifiers(this.classSpec)
        addJetbrainsAnnotation(this.classSpec)
        addPrivateDefaultConstructor(this.classSpec)
        val javaFile = JavaFile.builder(packageName, this.classSpec.build())
            .indent(INDENT_DEFAULT)
            .skipJavaLangImports(true)
            .build()
        writeFiles(listOf(javaFile), javaPath)
    }

    override fun getName(): String = "BlockGenerator"

    /**
     * Returns a [List] which contains all blocks which are currently persists in the database.
     * @return the list with the models or an empty list when no model is in the database
     */
    override fun getModels(): List<BlockModel> {
        return blocKRepository.findAll()
    }
}