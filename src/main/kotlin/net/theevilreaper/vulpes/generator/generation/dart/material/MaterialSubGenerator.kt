package net.theevilreaper.vulpes.generator.generation.dart.material

import net.minestom.server.item.Material
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.clazz.ClassBuilder
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec
import net.theevilreaper.vulpes.generator.util.EMPTY_STRING
import net.theevilreaper.vulpes.generator.util.StringHelper

internal object MaterialSubGenerator {

    private const val MATERIAL_KEY = "material"
    private const val MAX_STACK_SIZE = "maxStackSize"
    private val enumModifier = DartModifier.FINAL

    fun generateBlockMaterialEnum(className: String, materials: List<Material>): ClassBuilder {
        val enumProperties = materials.map {
            val name = escapeMinecraftPart(it.name())
            EnumPropertySpec.builder(name)
                .parameter("%C", StringHelper.mapDisplayName(name))
                .parameter("%C", name)
                .parameter("%L", it.maxStackSize())
                .build()
        }.toSet()
        val enumFile = ClassSpec.enumClass(className)
            .enumProperties(*enumProperties.toTypedArray())
            .properties(
                PropertySpec.builder("displayName", String::class).modifier(enumModifier).build(),
                PropertySpec.builder(MATERIAL_KEY, String::class).modifier(enumModifier).build(),
                PropertySpec.builder(MAX_STACK_SIZE, Int::class).modifier(enumModifier).build()
            )
            .constructor(
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameters(
                        ParameterSpec.builder("displayName").build(),
                        ParameterSpec.builder(MATERIAL_KEY).build(),
                        ParameterSpec.builder(MAX_STACK_SIZE).build()
                    )
                    .build()
            )
        return enumFile
    }

    /**
     * Escapes the minecraft part from the given name.
     * @param name the name to escape
     * @return the escaped name
     */
    private fun escapeMinecraftPart(name: String): String {
        return name.replace("minecraft:", EMPTY_STRING)
    }
}