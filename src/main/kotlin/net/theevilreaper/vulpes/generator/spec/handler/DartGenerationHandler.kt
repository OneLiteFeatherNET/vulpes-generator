package net.theevilreaper.vulpes.generator.spec.handler

import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import net.theevilreaper.vulpes.generator.registry.GeneratorRegistry
import net.theevilreaper.vulpes.generator.registry.RegistryProvider
import net.theevilreaper.vulpes.generator.util.FileHelper
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files

/**
 * DartGenerationHandler contains the endpoint to trigger the code generation for dart.
 * The generated code contains an enum for materials and enchantments.
 * Each specific enum is used by Stelaris for the model creation.
 * @author theEvilReaper
 * @since 1.0.0
 */
//@CrossOrigin(origins = ["*"], maxAge = 4800, allowCredentials = "false")
@RestController
class DartGenerationHandler(registryProvider: RegistryProvider) {
    private val dartGeneratorRegistry: GeneratorRegistry = registryProvider.getRegistry(GeneratorType.DART)
    private val outputName = "vulpes-dart"
    private val tempDirName = "Dart"
    private val attachmentFile: String = "attachment;filename=$outputName.zip"

    /**
     * The method defines the endpoint which can be called to start the code generation for the programming language Dart.
     */
    @GetMapping("/dart/generate/", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun generateDartClasses(): ResponseEntity<Any> {
        val temp = Files.createTempDirectory(tempDirName)
        dartGeneratorRegistry.triggerAll(path = temp)
        val zipFile = temp.resolve("$outputName.zip")
        FileHelper.zipFile(temp, zipFile)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, attachmentFile)
            .body(FileSystemResource(zipFile.toFile()))
    }
}
