package net.onelitefeather.vulpes.generator.domain.generation;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

@Serdeable
public interface GenerationResponse {


    @Schema(
            name = "GenerationSuccessResponse",
            description = "Response indicating successful generation"
    )
    @Serdeable
    record GenerationSuccessResponseDTO(
            @Schema(description = "Message indicating success") String message
    ) implements GenerationResponse {

    }

    @Schema(
            name = "GenerationPackSuccessResponse",
            description = "Response indicating successful generation of a resource pack"
    )
    @Serdeable
    record GenerationPackSuccessResponseDTO(
            @Schema(description = "Message indicating success") String message
    ) implements GenerationResponse {

    }

    @Schema(
            name = "GenerationErrorResponse",
            description = "Response indicating an error during generation"
    )
    @Serdeable
    record GenerationErrorResponseDTO(
            @Schema(description = "Error message describing the issue") String errorMessage
    ) implements GenerationResponse {

    }
}
