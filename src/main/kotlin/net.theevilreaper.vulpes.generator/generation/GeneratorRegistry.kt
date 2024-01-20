package net.theevilreaper.vulpes.generator.generation

import net.theevilreaper.vulpes.generator.generation.dart.*
import net.theevilreaper.vulpes.generator.generation.java.*
import org.springframework.stereotype.Component
import kotlin.jvm.Throws

/**
 * The registry contains all available implementation of the BaseGenerator
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@Component
class GeneratorRegistry(
    blockGenerator: BlockGenerator,
    notificationGenerator: NotificationGenerator,
    itemGenerator: ItemGenerator,
    fontGenerator: FontGenerator,
    materialGenerator: MaterialGenerator,
    enchantmentGenerator: EnchantmentGenerator,
    bossBarGenerator: BossBarGenerator,
    frameTypeGenerator: FrameTypeGenerator,
    itemFlagGenerator: ItemFlagGenerator,
    soundTypeGenerator: SoundTypeGenerator,
    attributeGenerator: AttributeGenerator
) {

    private val generatorMap: MutableMap<String, Generator> = mutableMapOf()

    /**
     * Add some generator implementations to the underlying map.
     */
    init {
        register(blockGenerator)
        register(notificationGenerator)
        register(itemGenerator)
        register(fontGenerator)
        register(materialGenerator)
        register(enchantmentGenerator)
        register(bossBarGenerator)
        register(frameTypeGenerator)
        register(itemFlagGenerator)
        register(soundTypeGenerator)
        register(attributeGenerator)
    }

    /**
     * Add a new generator to the underlying list.
     * @param generator the generator to add
     */
    @Throws(IllegalArgumentException::class)
    private fun register(generator: Generator) {
        require(generator.getName().trim().isNotEmpty()) { "The name from the generator can't be empty" }
        this.generatorMap[generator.getName()] = generator
    }

    /**
     * Remove a generator from the registry.
     * @param name the name from the generator
     * @return true if the generator can be deleted otherwise false
     */
    fun remove(name: String): Boolean {
        return this.generatorMap.remove(name) != null
    }

    /**
     * Get a generator instance by his corresponding name.
     * @param name the name from the generator
     * @return the fetched instance or null when the key does not match
     */
    fun getGenerator(name: String): Generator? {
        if (name.isEmpty()) return null
        return this.generatorMap[name]
    }

    /**
     * Returns the map which contains all registered {@link BaseGenerator} instances.
     * @return the map with the generators
     */
    fun getGenerators(): MutableMap<String, Generator> {
        return this.generatorMap
    }

    /**
     * Triggers for each available [Generator] instance to given data
     */
    fun cleanup() {
        this.generatorMap.values.forEach { it.cleanUp() }
    }
}