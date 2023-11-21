package net.theevilreaper.vulpes.generator.generation.dart

import net.kyori.adventure.sound.Sound
import net.theevilreaper.dartpoet.DartFile
import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.clazz.ClassSpec
import net.theevilreaper.dartpoet.enum.EnumPropertySpec
import net.theevilreaper.dartpoet.function.constructor.ConstructorSpec
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.GeneratorType
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class SoundTypeGenerator : BaseGenerator<Any>(
    className = "SoundSource",
    packageName = "sound",
    generatorType = GeneratorType.DART
) {
    override fun generate(javaPath: Path) {
        val enumClass = ClassSpec.enumClass(className)
            .apply {
                val entries = Sound.Source.entries
                entries.forEach {
                    enumProperty(
                        EnumPropertySpec
                            .builder(it.name.uppercase())
                            .parameter("%C", it.name)
                            .build()
                    )
                }
            }
            .property(PropertySpec.builder("name", String::class).modifier(DartModifier.FINAL).build())
            .constructor(
                ConstructorSpec.builder(className)
                    .modifier(DartModifier.CONST)
                    .parameter(ParameterSpec.builder("name", String::class).build())
                    .build()
            )
            .build()
        val file = DartFile.builder(packageName)
            .doc("Generated class for the sound sources. Don't edit this file manually")
            .type(enumClass)
            .build()
        file.write(javaPath)
    }

    override fun getName(): String = this.javaClass.simpleName

    override fun getModels(): List<Any> {
        TODO("Not yet implemented")
    }

}
