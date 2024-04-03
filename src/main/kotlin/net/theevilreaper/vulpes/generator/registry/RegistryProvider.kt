package net.theevilreaper.vulpes.generator.registry

import net.theevilreaper.vulpes.generator.generation.dart.*
import net.theevilreaper.vulpes.generator.generation.java.AttributeGenerator
import net.theevilreaper.vulpes.generator.generation.java.BlockGenerator
import net.theevilreaper.vulpes.generator.generation.java.FontGenerator
import net.theevilreaper.vulpes.generator.generation.java.NotificationGenerator
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import org.jetbrains.annotations.NotNull
import org.springframework.stereotype.Service

@Service
class RegistryProvider internal constructor(
    private val attributeGenerator: AttributeGenerator,
    private val blockGenerator: BlockGenerator,
    private val fontGenerator: FontGenerator,
    private val itemGenerator: BlockGenerator,
    private val notificationGenerator: NotificationGenerator
) {

    private val dartGeneratorRegistry = GeneratorRegistry(GeneratorType.DART)

    private val javaGeneratorRegistry = GeneratorRegistry(GeneratorType.JAVA)

    init {
        addJavaGenerators()
        addDartGenerators()
    }

    private fun addJavaGenerators() {
        javaGeneratorRegistry.add(attributeGenerator)
        javaGeneratorRegistry.add(blockGenerator)
        javaGeneratorRegistry.add(fontGenerator)
        javaGeneratorRegistry.add(itemGenerator)
        javaGeneratorRegistry.add(notificationGenerator)
    }

    private fun addDartGenerators() {
        dartGeneratorRegistry.add(BossBarGenerator())
        dartGeneratorRegistry.add(EffectGenerator())
        dartGeneratorRegistry.add(EnchantmentGenerator())
        dartGeneratorRegistry.add(EntityTypeGenerator())
        dartGeneratorRegistry.add(FrameTypeGenerator())
        dartGeneratorRegistry.add(ItemFlagGenerator())
        dartGeneratorRegistry.add(MaterialGenerator())
        dartGeneratorRegistry.add(SoundTypeGenerator())
    }

    /**
     * Returns a reference from the [GeneratorRegistry] based on a given generator type
     * @param type the type to check
     * @return the instance to the registry
     */
    @NotNull
    fun getRegistry(type: GeneratorType): GeneratorRegistry {
        return when (type) {
            GeneratorType.DART -> dartGeneratorRegistry
            GeneratorType.JAVA -> javaGeneratorRegistry
        }
    }
}
