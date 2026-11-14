package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.serde.annotation.Serdeable;
import net.onelitefeather.vulpes.generator.release.ReleaseDiagnostic;

import java.util.List;

/**
 * The answer of the content validation: every problem of the current content, empty when the content is valid.
 *
 * @param problems the problems, one diagnostic each, naming the content entry and what is wrong
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record ContentValidationResponse(List<ReleaseDiagnostic> problems) {
}
