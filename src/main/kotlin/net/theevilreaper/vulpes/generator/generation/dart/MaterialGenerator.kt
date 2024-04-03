package net.theevilreaper.vulpes.generator.generation.dart

import net.minestom.server.item.Material
import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.dart.material.MaterialSubGenerator
import net.theevilreaper.vulpes.generator.generation.dart.material.MaterialSubType
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
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

    private val materialClassName = "Material"
    private val classDocumentation = "The file is generated. Don't change anything here"

    init {
        check(className.trim().isNotEmpty()) { "The class name can't be empty" }
    }

    /**
     * Generates the enum which contains all values for dart.
     * @param javaPath the path to store the content
     */
    override fun generate(javaPath: Path) {
        val models = getModels()
        val enumFiles = mutableListOf<DartFile>()

        MaterialSubType.entries.forEach {
            val className = translateEnumClassName(it)
            val fileName = "${it.type}_materials"
            val enumClass = generateItemEnum(models, className) { mat -> mapTypeToBoolean(it, mat) }
            if (enumClass == null) return@forEach
            val file = DartFile.builder(fileName)
                .type(enumClass)
                .doc(classDocumentation)
                .build()
            enumFiles.add(file)
        }

        if (enumFiles.isEmpty()) return
        enumFiles.forEach { it.write(javaPath) }
    }

    private fun mapTypeToBoolean(subType: MaterialSubType, material: Material): Boolean {
        return when (subType) {
            MaterialSubType.BLOCK -> material.isBlock
            MaterialSubType.ARMOR -> material.isArmor
            MaterialSubType.FOOD -> material.isFood
        }
    }

    private fun translateEnumClassName(materialSubType: MaterialSubType): String {
        return "${materialSubType.type.replaceFirstChar { it.uppercase() }}$materialClassName"
    }

    private inline fun generateItemEnum(
        materials: List<Material>,
        className: String,
        crossinline filter: (Material) -> Boolean,
    ): ClassSpec? {
        if (materials.isEmpty()) return null
        val filteredModels = filterMaterials(materials, filter)
        val enumClass = MaterialSubGenerator.generateBlockMaterialEnum(className, filteredModels)
        return enumClass.build()
    }

    private inline fun filterMaterials(
        materials: List<Material>,
        crossinline filter: (Material) -> Boolean,
    ): List<Material> {
        return materials.filter { filter(it) }
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