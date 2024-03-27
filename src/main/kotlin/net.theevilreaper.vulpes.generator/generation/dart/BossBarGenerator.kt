package net.theevilreaper.vulpes.generator.generation.dart

import net.kyori.adventure.bossbar.BossBar
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

/**
 * This generator type contains the logic to generate the [BossBar] enum structure to enums for dart.
 * A generation process triggers all enum generation, and it is not possible to skip one type
 * @author theEvilReaper
 * @since 1.0.0
 */
@Service
class BossBarGenerator : BaseGenerator<BossBar>(
    className = "BossBarColor",
    packageName = "boss_bar_color",
    generatorType = GeneratorType.DART
) {
    private val overlayClass = "BossOverlay"
    private val overlayFile = "boss_overlay"
    private val flagClass = "BossFlag"
    private val flagFile = "boss_flag"
    private val displayValue = "display"
    private val enumValue = "enumValue"
    private val stringIdentifier = "%C"

    private val propertyStack: Array<PropertySpec> = arrayOf(
        PropertySpec.builder(displayValue, String::class).modifier { DartModifier.FINAL }.build(),
        PropertySpec.builder(enumValue, String::class).modifier { DartModifier.FINAL }.build()
    )

    private val parameterStack: Array<ParameterSpec> = arrayOf(
        ParameterSpec.builder(displayValue).build(),
    )

    /**
     * Triggers the generation for all relevant data for the [BossBar].
     * @param javaPath the path to write the code into
     */
    override fun generate(javaPath: Path) {
        generateBossColors().write(javaPath)
        generateStyle().write(javaPath)
        generateFlags().write(javaPath)
    }

    /**
     * Generates the [DartFile] which contains all values from the [BossBar.Color] class.
     */
    private fun generateBossColors(): DartFile {
        return DartFile.builder(packageName)
            .type(
                ClassSpec.enumClass(className)
                    .apply {
                        BossBar.Color.entries.forEach { color ->
                            enumProperty(
                                EnumPropertySpec.builder(color.name.lowercase())
                                    .parameter(stringIdentifier, color.name.lowercase().replaceFirstChar { it.uppercase() })
                                    .build()
                            )
                        }
                    }
                    .properties(*propertyStack)
                    .constructor(
                        ConstructorSpec.builder(className)
                            .modifier(DartModifier.CONST)
                            .parameter(ParameterSpec.builder(displayValue).build())
                            .build()
                    )
            )
            .build()
    }

    /**
     * Generates the [DartFile] which contains all values from the [BossBar.Overlay] class.
     */
    private fun generateStyle(): DartFile {
        return DartFile.builder(overlayFile)
            .type(
                ClassSpec.enumClass(overlayClass)
                    .apply {
                        BossBar.Overlay.entries.forEach { overlay ->
                            enumProperty(
                                EnumPropertySpec.builder(overlay.name.lowercase())
                                    .parameter(stringIdentifier, StringHelper.mapDisplayName(overlay.name))
                                    .parameter(stringIdentifier, overlay.name.uppercase())
                                    .build()
                            )
                        }
                    }
                    .properties(*propertyStack)
                    .constructor(
                        ConstructorSpec.builder(overlayClass)
                            .modifier(DartModifier.CONST)
                            .parameters(*parameterStack)
                            .build()
                    )
            )
            .build()
    }

    /**
     * Generates the [DartFile] which contains all values from the [BossBar.Flag] class.
     */
    private fun generateFlags(): DartFile {
        return DartFile.builder(flagFile)
            .type(
                ClassSpec.enumClass(flagClass)
                    .apply {
                        BossBar.Flag.entries.forEach { overlay ->
                            enumProperty(
                                EnumPropertySpec.builder(overlay.name.lowercase())
                                    .parameter(stringIdentifier, StringHelper.mapDisplayName(overlay.name))
                                    .build()
                            )
                        }
                    }
                    .properties(*propertyStack)
                    .constructor(
                        ConstructorSpec.builder(flagClass)
                            .modifier(DartModifier.CONST)
                            .parameters(*parameterStack)
                            .build()
                    )
            )
            .build()
    }

    /**
     * Returns the name from the generator as [String].
     * @return the given name
     */
    override fun getName(): String = "BossBarGenerator"

    /**
     * Not in use for this generator
     */
    override fun getModels(): List<BossBar> = throw Exception("The generator can't use this getter")
}