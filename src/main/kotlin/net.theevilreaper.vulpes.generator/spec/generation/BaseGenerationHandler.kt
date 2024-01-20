package net.theevilreaper.vulpes.generator.spec.generation

import org.springframework.http.ResponseEntity

/**
 * The interface defines the basic operations to download a template or trigger a new generation.
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
@Deprecated(message = "This interface will be removed in the future")
interface BaseGenerationHandler {

    /**
     * Download a specific template by a given name.
     * @param template the name from the template
     */
    fun downloadTemplate(template: String): ResponseEntity<Any>

    /**
     * Trigger a new build process from the Vulpes project.
     */
    fun triggerGeneration(): ResponseEntity<Any>
}