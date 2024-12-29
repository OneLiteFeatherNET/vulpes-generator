package net.theevilreaper.vulpes.generator.registry

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.theevilreaper.vulpes.generator.generation.java.AttributeGenerator
import net.theevilreaper.vulpes.generator.generation.java.FontGenerator
import net.theevilreaper.vulpes.generator.generation.java.ItemGenerator
import net.theevilreaper.vulpes.generator.generation.java.NotificationGenerator

@Singleton
class RegistryProvider @Inject constructor(
    private val attributeGenerator: AttributeGenerator,
    private val fontGenerator: FontGenerator,
    private val itemGenerator: ItemGenerator,
    private val notificationGenerator: NotificationGenerator
) {

    private val javaGeneratorRegistry = GeneratorRegistry()

    init {
        addJavaGenerators()
    }

    private fun addJavaGenerators() {
        javaGeneratorRegistry.add(attributeGenerator)
        javaGeneratorRegistry.add(fontGenerator)
        javaGeneratorRegistry.add(itemGenerator)
        javaGeneratorRegistry.add(notificationGenerator)
    }

    fun getRegistry(): GeneratorRegistry {
        return javaGeneratorRegistry
    }
}
