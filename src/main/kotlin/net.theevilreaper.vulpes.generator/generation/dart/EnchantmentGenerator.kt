package net.theevilreaper.vulpes.generator.generation.dart

import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec
import net.theevilreaper.vulpes.api.model.EnchantmentWrapper
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.GeneratorType
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class EnchantmentGenerator : BaseGenerator<EnchantmentWrapper>(
    className = "Enchantment",
    packageName = "enchantment",
    generatorType = GeneratorType.DART
) {

    private var data: List<EnchantmentWrapper>? = emptyList()

    /**
     * Set the new content for the enchantment generation.
     * @param values the list which contains the [EnchantmentWrapper] for the generation
     */
    fun setData(values: List<EnchantmentWrapper>) {
        data = values
    }

    override fun generate(javaPath: Path) {
        if (data.orEmpty().isEmpty()) return
        val enumClass = ClassSpec.enumClass(className)
            .apply {
                data!!.forEach {
                    enumProperty(
                        EnumPropertySpec.builder(
                            it.toVariableName()
                        )
                            .parameter("%C", it.mojangName)
                            .parameter("%C", it.category)
                            .parameter("%L", it.minLevel)
                            .parameter("%L", it.maxLevel)
                            .build()
                    )
                }
            }
            .property(PropertySpec.builder("name", String::class).build())
            .property(PropertySpec.builder("category", String::class).build())
            .property(PropertySpec.builder("minLevel", Integer::class).build())
            .property(PropertySpec.builder("maxLevel", Integer::class).build())
            .constructor {
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameter(ParameterSpec.builder("name").modifier(DartModifier.FINAL).build())
                    .parameter(ParameterSpec.builder("category").modifier(DartModifier.FINAL).build())
                    .parameter(ParameterSpec.builder("minLevel").modifier(DartModifier.FINAL).build())
                    .parameter(ParameterSpec.builder("maxLevel").modifier(DartModifier.FINAL).build())
                    .build()
            }
            .build()
        val enumFile = DartFile.builder(className.replaceFirstChar { it.lowercase() })
            .doc("The file is generated. Don't change anything here")
            .type(enumClass)
            .build()
        enumFile.write(javaPath)
        data = null
    }

    /**
     * Returns the name from [EnchantmentWrapper] which is used as identifier.
     * @return the given name
     */
    override fun getName() = "EnchantmentGenerator"

    /**
     * Not in use.
     */
    override fun getModels(): List<EnchantmentWrapper> {
        TODO("Not yet implemented")
    }
}
