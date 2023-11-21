package net.theevilreaper.vulpes.generator.generation.dart

import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.GeneratorType
import net.theevilreaper.vulpes.model.MaterialWrapper
import org.springframework.stereotype.Service
import java.nio.file.Path

/**
 *
 * @author theEvilReaper
 */
@Service
class MaterialGenerator : BaseGenerator<MaterialWrapper>(
    className = "Materials",
    packageName = "materials",
    generatorType = GeneratorType.DART
) {

    private var generationData: MutableList<MaterialWrapper> = mutableListOf()

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
        val enumClass = ClassSpec.enumClass(className)
            .apply {
                generationData.forEach {
                    val name = it.mojangName.split(":")[1].replace("_", "")
                    enumProperty(
                        EnumPropertySpec
                            .builder(name)
                            .parameter("%C", it)
                            .build()
                    )
                }
            }
            .property(PropertySpec.builder("name", String::class).modifier(DartModifier.FINAL).build())
            .constructor(
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameter(ParameterSpec.builder("name").build())
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

    /**
     * Returns the name from [MaterialGenerator] which is used as identifier.
     * @return the given name
     */
    override fun getName() = "MaterialGenerator"

    /**
     * Not in use.
     */
    override fun getModels(): List<MaterialWrapper> {
        return generationData
    }
}