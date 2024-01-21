package net.theevilreaper.vulpes.generator.spec.handler

import net.theevilreaper.vulpes.generator.cache.MinecraftDataCache
import net.theevilreaper.vulpes.generator.generation.Generator
import net.theevilreaper.vulpes.generator.generation.GeneratorRegistry
import net.theevilreaper.vulpes.generator.generation.dart.EnchantmentGenerator
import net.theevilreaper.vulpes.generator.generation.dart.MaterialGenerator
import net.theevilreaper.vulpes.generator.generation.dart.SoundTypeGenerator
import net.theevilreaper.vulpes.generator.generation.type.DartGenerationType
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import net.theevilreaper.vulpes.generator.spec.MinecraftDataSpec
import net.theevilreaper.vulpes.generator.util.FileHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

/**
 * DartGenerationHandler contains the endpoint to trigger the code generation for dart.
 * The generated code contains an enum for materials and enchantments.
 * Each specific enum is used by Stelaris for the model creation.
 * @author theEvilReaper
 * @since 1.0.0
 */
@CrossOrigin(origins = ["*"], maxAge = 4800, allowCredentials = "false")
@RestController
class DartGenerationHandler(
    registry: GeneratorRegistry,
    private val specCache: MinecraftDataCache,
) {
    @Value(value = "19")
    private lateinit var supportedMainVersion: String

    private val logger: Logger = LoggerFactory.getLogger(DartGenerationHandler::class.java)
    private val dartGenerators: Map<String, Generator> =
        registry.getGenerators().values.filter { it.getType() == GeneratorType.DART }.associateBy { it.getName() }
    private val outputName = "vulpes-dart"
    private val tempDirName = "Dart"

    /**
     * The method represents the endpoint which triggers the code generation for dart.
     * @param version the minecraft version to build the right data for the right version
     * @param options a list which contains all generator value which should be triggered.
     */
    @GetMapping("/dart/generate/{version}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun generateDartClasses(
        @PathVariable("version") version: String,
        @RequestBody options: List<String>?,
    ): ResponseEntity<Any> {
        if (!version.contains(supportedMainVersion)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The generation only supports a minecraft version with the minor version of $supportedMainVersion"
            )
        }

        if (options.isNullOrEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The dart generation needs some option for the generation"
            )
        }

        logger.info("Dart generation is triggered for minecraft version: $version")
        val temp = createTempDirectory(tempDirName)
        val specDataSpec: MinecraftDataSpec = specCache.getDataFromCache(version).join()

        options.forEach {
            val generator = dartGenerators[it] ?: return@forEach
            when (DartGenerationType.getGenerationType(generator.getName())) {
                DartGenerationType.MATERIAL -> generateMaterial(temp, specDataSpec, generator)
                DartGenerationType.ENCHANTMENT -> EnchantmentGenerator(specDataSpec.enchantments).generate(temp)
                else -> generator.generate(temp)
            }
        }

        val zipFile = temp.resolve("$outputName.zip")
        FileHelper.zipFile(temp, zipFile)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=materials.dart")
            .body(FileSystemResource(zipFile.toFile()))
    }

    /**
     * Generates all given enchantments over the [MaterialGenerator].
     * @param javaPath the [Path] to write the file
     * @param spec the spec object which contains the data about the materials
     * @param generator the [Generator] implementation
     */
    private fun generateMaterial(javaPath: Path, spec: MinecraftDataSpec, generator: Generator) {
        val materialGenerator = generator as MaterialGenerator
        materialGenerator.updateGenerationData(spec.materials)
        materialGenerator.generate(javaPath)
    }
}
