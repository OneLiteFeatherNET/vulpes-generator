package net.theevilreaper.vulpes.generator.generation.dart

import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.vulpes.api.model.EnchantmentWrapper
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import net.theevilreaper.vulpes.generator.util.CLASS_PROPERTIES
import net.theevilreaper.vulpes.generator.util.CONSTRUCTOR_PARAMETERS
import org.springframework.stereotype.Service
import java.nio.file.Path

/**
 * The [EnchantmentGenerator] contains the logic to map the data from an [EnchantmentWrapper] into a dart enum
 * which contains all available enchantments.
 * @property enchantmentData the data which should be mapped
 * @constructor Sets the basic values for the generation
 * @version 1.0
 * @author theEvilReaper
 */
@Service
class EnchantmentGenerator(
    private val enchantmentData: List<EnchantmentWrapper> = emptyList(),
) : BaseGenerator<EnchantmentWrapper>(
    className = "Enchantment",
    packageName = "enchantment",
    generatorType = GeneratorType.DART
) {

    override fun generate(javaPath: Path) {
        if (enchantmentData.isEmpty()) return
        val enumClass = ClassSpec.enumClass(className)
            .apply {
                enchantmentData.forEach { mapEnchantmentToEnumProperty(it) }
            }
            .properties(*CLASS_PROPERTIES)
            .constructor {
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameters(*CONSTRUCTOR_PARAMETERS)
                    .build()
            }
            .build()
        val enumFile = DartFile.builder(className.replaceFirstChar { it.lowercase() })
            .doc("The file is generated. Don't change anything here")
            .type(enumClass)
            .build()
        enumFile.write(javaPath)
    }

    /**
     * Maps the given [EnchantmentWrapper] into a [EnumPropertySpec].
     * @param enchantment the given enchantment
     * @return the generated property
     */
    private fun mapEnchantmentToEnumProperty(enchantment: EnchantmentWrapper): EnumPropertySpec {
        return EnumPropertySpec.builder(enchantment.toVariableName())
            .parameter("%C", enchantment.mojangName)
            .parameter("%C", enchantment.category)
            .parameter("%L", enchantment.minLevel)
            .parameter("%L", enchantment.maxLevel)
            .build()
    }

    /**
     * Returns the name from [EnchantmentWrapper] which is used as identifier.
     * @return the given name
     */
    override fun getName() = "EnchantmentGenerator"

    /**
     * Not in use.
     */
    @Throws(UnsupportedOperationException::class)
    override fun getModels(): List<EnchantmentWrapper> {
        throw UnsupportedOperationException("The getter is not supported for this generator")
    }
}
