package net.onelitefeather.vulpes.generator.release;

import io.micronaut.serde.annotation.Serdeable;

/**
 * One compiler error of a refused release.
 *
 * @param file    the file of the generated source, relative to the generated root
 * @param line    the line of the error, starting at 1
 * @param message the message of the compiler
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record ReleaseDiagnostic(String file, long line, String message) {
}
