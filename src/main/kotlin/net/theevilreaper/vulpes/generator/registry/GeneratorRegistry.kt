package net.theevilreaper.vulpes.generator.registry

import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.generation.Generator
import java.nio.file.Path

class GeneratorRegistry internal constructor(
) {
    private val generators: MutableMap<String, Generator> = mutableMapOf()

    @Throws(IllegalStateException::class)
    fun add(generator: BaseGenerator<*>) {
        check(generator.getName().trim().isNotEmpty()) { "The name of the generator can't be empty" }
        this.generators[generator.getName()] = generator
    }

    @Throws(IllegalStateException::class)
    fun remove(name: String): Boolean {
        check(name.trim().isNotEmpty()) { "The name of the generator to remove can't be empty" }
        return this.generators.remove(name) != null
    }

    fun triggerGeneration(generator: String, path: Path) {
        require(generator.trim().isEmpty()) { "The name of the generator can't be empty" }
        val genInstance = this.generators[generator] ?: return
        genInstance.generate(path)
    }

    fun triggerAll(path: Path) {
        if (this.generators.isEmpty()) return
        this.generators.forEach { it.value.generate(path) }
    }
}
