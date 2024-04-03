package net.theevilreaper.vulpes.generator.generation.dart

import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.minestom.server.item.ItemHideFlag
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.dart.util.DEFAULT_PARAMETERS
import net.theevilreaper.vulpes.generator.generation.dart.util.DEFAULT_PROPERTIES
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import net.theevilreaper.vulpes.generator.util.StringHelper
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class ItemFlagGenerator : net.theevilreaper.vulpes.generator.generation.BaseGenerator<ItemHideFlag>(
    className = "ItemFlags",
    packageName = "item_flags",
    generatorType = GeneratorType.DART
) {

    override fun generate(javaPath: Path) {
        val enumFile = DartFile.builder(packageName)
            .type(
                ClassSpec.enumClass(className)
                    .apply {
                        getModels().forEach { entry ->
                            val propertyName = entry.name.replace("HIDE_", "").lowercase()
                            enumProperty(
                                EnumPropertySpec.builder(propertyName)
                                    .parameter("%C", StringHelper.mapDisplayName(propertyName))
                                    .parameter("%C", entry.name)
                                    .build()
                            )
                        }
                    }
                    .properties(*DEFAULT_PROPERTIES)
                    .constructor(
                        ConstructorSpec.builder(className)
                            .modifier(DartModifier.CONST)
                            .parameters(*DEFAULT_PARAMETERS)
                            .build()
                    )
                    .endWithNewLine(true)
                    .build()
            )
            .build()
        enumFile.write(javaPath)
    }

    override fun getName() = "DartItemFlagGenerator"

    /**
     * Returns a list which contains all available [ItemHideFlag].
     * @return the given list
     */
    override fun getModels(): List<ItemHideFlag> {
        return ItemHideFlag.entries.toList()
    }
}