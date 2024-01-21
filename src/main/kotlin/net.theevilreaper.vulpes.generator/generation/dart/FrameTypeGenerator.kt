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
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class FrameTypeGenerator :
    BaseGenerator<FrameType>(className = "FrameType", "frame_type", generatorType = GeneratorType.DART) {
    override fun generate(javaPath: Path) {
        val file = DartFile.builder(packageName)
            .type(
                ClassSpec.enumClass(className)
                    .apply {
                        getModels().forEach { model ->
                            val name = model.name.lowercase()
                            enumProperty(
                                EnumPropertySpec.builder(name)
                                    .parameter("%C", name.replaceFirstChar { it.uppercase() })
                                    .build()
                            )
                        }
                    }
                    .property(PropertySpec.builder("display", String::class).modifier(DartModifier.FINAL).build())
                    .constructor(
                        ConstructorSpec.builder(className)
                            .modifier(DartModifier.CONST)
                            .parameter(ParameterSpec.builder("display").build())
                            .build()
                    )
                    .endWithNewLine(true)
                    .build()
            )
            .build()
        file.write(javaPath)
    }

    override fun getName() = "FrameTypeGenerator"

    override fun getModels(): List<FrameType> = FrameType.entries.toList()
}
