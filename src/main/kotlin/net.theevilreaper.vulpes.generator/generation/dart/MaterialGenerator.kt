package net.theevilreaper.vulpes.generator.generation.dart

import net.minestom.server.item.Material
import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec
import net.theevilreaper.vulpes.api.model.MaterialWrapper
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import net.theevilreaper.vulpes.generator.util.EMPTY_STRING
import org.springframework.stereotype.Service
import java.nio.file.Path

/**
 *
 * @author theEvilReaper
 */
@Service
class MaterialGenerator : BaseGenerator<Material>(
    className = "Materials",
    packageName = "materials",
    generatorType = GeneratorType.DART
) {

    private var generationData: MutableList<MaterialWrapper> = mutableListOf()
    private val keyName = "namespace"
    private val categoryKey = "category"
    private val stackSizeKey = "stackSize"

    init {
        check(className.trim().isNotEmpty()) { "The class name can't be empty" }
    }

    /**
     * Set the data for the enum generation.
     * @param map the map which contains the data for the generation
     */
    fun updateGenerationData(map: List<MaterialWrapper>) {
        this.generationData += map;
    }

    /**
     * Clears the given map which contains the data for the generation.
     */
    fun clearData() = generationData.clear()

    /**
     * Generates the enum which contains all values for dart.
     * @param javaPath the path to store the content
     */
    override fun generate(javaPath: Path) {
        val models = getModels();
        val enumEntries = mutableListOf<EnumPropertySpec>()
        for (material in models) {
           // material.isBlock
            val name = material.name().replace("minecraft:", EMPTY_STRING)
            enumEntries.add(
                EnumPropertySpec.builder(name.uppercase())
                    .parameter("%C", name)
                    .parameter("%C", mapToCategory(material))
                    .parameter("%L", material.maxStackSize())
                    .build()
            )
        }
        val enumClass = ClassSpec.enumClass(className)
            .properties(
                PropertySpec.builder(keyName, String::class).build(),
                PropertySpec.builder(categoryKey, String::class).build(),
                PropertySpec.builder(stackSizeKey, Int::class).build()
            )
            .enumProperties(*enumEntries.toTypedArray())
            .constructor(
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameters(
                        ParameterSpec.builder(keyName).build(),
                        ParameterSpec.builder(categoryKey).build(),
                        ParameterSpec.builder(stackSizeKey).build()
                    )
                    .build()
            )
            .build()

        val file = DartFile.builder(packageName)
            .doc("The file is generated. Don't change anything here")
            .type(enumClass)
            .build()

        file.write(javaPath)
        clearData()
    }

    private fun mapToCategory(material: Material): String {
        if (material.isFood) return "food"
        if (material.isArmor) return "armor"
        return "block"
    }

    /**
     * Returns the name from [MaterialGenerator] which is used as identifier.
     * @return the given name
     */
    override fun getName() = "MaterialGenerator"

    /**
     * Not in use.
     */
    override fun getModels(): List<Material> = Material.values().toList()
}