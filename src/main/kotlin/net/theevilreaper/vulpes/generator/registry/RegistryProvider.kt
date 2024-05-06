package net.theevilreaper.vulpes.generator.registry

import net.theevilreaper.vulpes.generator.generation.java.AttributeGenerator
import net.theevilreaper.vulpes.generator.generation.java.BlockGenerator
import net.theevilreaper.vulpes.generator.generation.java.FontGenerator
import net.theevilreaper.vulpes.generator.generation.java.NotificationGenerator
import org.springframework.stereotype.Service

@Service
class RegistryProvider internal constructor(
    private val attributeGenerator: AttributeGenerator,
    private val blockGenerator: BlockGenerator,
    private val fontGenerator: FontGenerator,
    private val itemGenerator: BlockGenerator,
    private val notificationGenerator: NotificationGenerator
) {

    private val javaGeneratorRegistry = GeneratorRegistry()

    init {
        addJavaGenerators()
    }

    private fun addJavaGenerators() {
        javaGeneratorRegistry.add(attributeGenerator)
        javaGeneratorRegistry.add(blockGenerator)
        javaGeneratorRegistry.add(fontGenerator)
        javaGeneratorRegistry.add(itemGenerator)
        javaGeneratorRegistry.add(notificationGenerator)
    }


    fun getRegistry(): GeneratorRegistry {
        return javaGeneratorRegistry
    }
}
