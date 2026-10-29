package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.serde.annotation.Serdeable;

/**
 * The body of a refused {@code POST /releases}.
 *
 * @param reason  the rejection reason, one of the names of {@code ReleaseRejectedException.Reason}
 * @param message a human readable description
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record ReleaseErrorResponse(String reason, String message) {
}
