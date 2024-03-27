package net.theevilreaper.vulpes.generator.generation.dart

import net.minestom.server.advancements.FrameType
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
class FrameTypeGenerator : BaseGenerator<FrameType>(
    className = "FrameType",
    packageName = "frame_type",
    generatorType = GeneratorType.DART,
) {
    private val displayEntry: String = "display"
    override fun generate(javaPath: Path) {
        val enumFile = ClassSpec.enumClass(className)
            .also {
                getModels().forEach { model ->
                    val name = model.name.lowercase()
                    it.enumProperty(
                        EnumPropertySpec.builder(name)
                            .parameter("%C", StringHelper.mapDisplayName(name))
                            .build()
                    )
                }
            }
            .property(PropertySpec.builder(displayEntry, String::class).modifier(DartModifier.FINAL).build())
            .constructor(
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameter(ParameterSpec.builder(displayEntry).build())
                    .build()
            )
            .endWithNewLine(true)
            .build()
        val file = DartFile.builder(packageName)
            .type(enumFile)
            .build()
        file.write(javaPath)
    }

    override fun getName() = "FrameTypeGenerator"

    override fun getModels(): List<FrameType> = FrameType.entries.toList()
}
