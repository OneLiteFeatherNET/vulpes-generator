package net.onelitefeather.vulpes.generator.controller.release;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.serde.annotation.Serdeable;
import net.onelitefeather.vulpes.generator.release.ReleaseDiagnostic;

import java.util.List;

/**
 * The body of a refused release. {@code diagnostics} and {@code truncated} are written only when they are set:
 * a compilation refusal lists its compiler errors, and {@code truncated} is {@code true} when the list was cut.
 *
 * @param reason      the rejection reason, one of the names of {@code ReleaseRejectedException.Reason}
 * @param message     a human readable description
 * @param diagnostics the compiler errors, or {@code null} when there are none
 * @param truncated   {@code true} when the diagnostics were cut, or {@code null} when they were not
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record ReleaseErrorResponse(
        String reason,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ReleaseDiagnostic> diagnostics,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean truncated
) {

    /**
     * Creates an error body without diagnostics.
     *
     * @param reason  the rejection reason
     * @param message a human readable description
     */
    public ReleaseErrorResponse(String reason, String message) {
        this(reason, message, null, null);
    }
}
