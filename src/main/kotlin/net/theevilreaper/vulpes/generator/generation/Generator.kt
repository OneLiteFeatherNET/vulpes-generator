package net.theevilreaper.vulpes.generator.generation

import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import java.nio.file.Path

/**
 * The interface defines some basic structure for generators
 * @author Joltras
 * @version 1.0.0
 * @author 1.0.0
 */
interface Generator {

    /**
     * Each generator must implement his own logic for this method
     */
    fun generate(javaPath: Path)

    /**
     * Returns the name from the generator.
     * @return the given generator name
     */
    fun getName(): String

    /**
     * Contains logic to clean up the generator data structure.
     */
    fun cleanUp()

    /**
     * Returns the given [GeneratorType] from the generator implementation.
     * @return the given type
     */
    fun getType(): GeneratorType
}
