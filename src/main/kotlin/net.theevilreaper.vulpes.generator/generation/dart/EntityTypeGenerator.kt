package net.theevilreaper.vulpes.generator.generation.dart

import net.minestom.server.entity.EntityType
import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import net.theevilreaper.vulpes.generator.util.EMPTY_STRING
import net.theevilreaper.vulpes.generator.util.StringHelper
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class EntityTypeGenerator : BaseGenerator<EntityType>(
    className = "EntityType",
    packageName = "entity_type",
    generatorType = GeneratorType.DART
) {

    private val variableName = "type"

    override fun generate(javaPath: Path) {
        val models = EntityType.values()
        val enumEntries = mutableListOf<EnumPropertySpec>()
        for (type in models) {
            val name = type.name().replace("minecraft:", EMPTY_STRING)
            enumEntries.add(
                EnumPropertySpec.builder(name.lowercase())
                    .parameter("%C", StringHelper.mapDisplayName(name))
                    .parameter("%C", name)
                    .build()
            )
        }
        val enumClass = ClassSpec.enumClass(className)
            .properties(
                PropertySpec.builder("displayName", String::class).build(),
                PropertySpec.builder(variableName, String::class).build()
            )
            .enumProperties(*enumEntries.toTypedArray())
            .constructor(
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameters(
                        ParameterSpec.builder("displayName").build(),
                        ParameterSpec.builder(variableName).build()
                    )
                    .build()
            )
            .build()

        val file = DartFile.builder(packageName)
            .doc("Generated class to represent the available entities from the game Minecraft")
            .type(enumClass)
            .build()
        file.write(javaPath)
    }

    override fun getName() = "EntityTypeGenerator"
    override fun getModels(): List<EntityType> = EntityType.values().toList()
}