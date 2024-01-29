package net.theevilreaper.vulpes.generator.generation.dart.material

import net.minestom.server.item.Material
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.vulpes.generator.util.EMPTY_STRING

internal object BlockMaterialGenerator {

    fun generateBlockMaterialEnum(className: String, materials: Set<Material>): ClassSpec? {
        if (materials.isEmpty()) return null
        val enumProperties = materials.map {
            EnumPropertySpec.builder(it.name().replace("minecraft:", EMPTY_STRING)).build()
        }.toSet()
        val enumFile = ClassSpec.builder(className)
            .enumProperties(*enumProperties.toTypedArray())
            .constructor(
                ConstructorSpec.builder(className)
                    .build()
            )
            .build()
        return enumFile
    }
}