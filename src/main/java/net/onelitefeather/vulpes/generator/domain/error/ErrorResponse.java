package net.onelitefeather.vulpes.generator.domain.error;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

public interface ErrorResponse {

    @Schema(description = "Error message")
    String errorMessage();

    @Schema(description = "Error message")
    @Serdeable
    record ErrorResponseDTO(
            @Schema(description = "Error message") String errorMessage
    ) implements ErrorResponse {
    }
}