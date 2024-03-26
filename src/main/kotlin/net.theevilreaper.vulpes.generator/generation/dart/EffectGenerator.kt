package net.theevilreaper.vulpes.generator.generation.dart

import net.minestom.server.effects.Effects
import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import net.theevilreaper.vulpes.generator.util.StringHelper
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class EffectGenerator : BaseGenerator<Effects>(
    className = "Effect",
    packageName = "effect",
    generatorType = GeneratorType.DART
) {
    override fun generate(javaPath: Path) {
        val effects = getModels()
        val enumEntries = mutableListOf<EnumPropertySpec>()
        effects.forEach {
            enumEntries.add(
                EnumPropertySpec.builder(it.name.lowercase()).parameter("%C", StringHelper.mapDisplayName(it.name)).build()
            )
        }

        val enumClass = ClassSpec.enumClass(className)
            .enumProperties(*enumEntries.toTypedArray())
            .property(PropertySpec.builder("displayName", String::class).build())
            .constructor(
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameters(ParameterSpec.builder("displayName").build())
                    .build()
            )
            .build()
        val file = DartFile.builder(packageName)
            .doc("Generated class to represent the available effects from the game Minecraft")
            .type(enumClass)
            .build()
        file.write(javaPath)
    }

    override fun getName(): String = "EffectGenerator"

    override fun getModels(): List<Effects> = Effects.entries.toList()

}
