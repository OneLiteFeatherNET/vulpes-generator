package net.theevilreaper.vulpes.generator.generation.dart

import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.minestom.server.item.ItemHideFlag
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class ItemFlagGenerator : BaseGenerator<ItemHideFlag>(
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
                            val propertyName = entry.name.replace("HIDE", "").lowercase()
                            enumProperty(
                                EnumPropertySpec.builder(propertyName)
                                    .parameter("%C", propertyName.replaceFirstChar { it.uppercase() })
                                    .parameter("%C", entry.name)
                                    .build()
                            )
                        }
                    }
                    .property(PropertySpec.builder("name", String::class).modifier { DartModifier.FINAL }.build())
                    .property(PropertySpec.builder("minestomValue", String::class).modifier { DartModifier.FINAL }
                        .build())
                    .constructor(
                        ConstructorSpec.builder(className)
                            .modifier(DartModifier.FINAL)
                            .parameter(ParameterSpec.builder("name").build())
                            .parameter(ParameterSpec.builder("minestomValue").build())
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