package net.onelitefeather.vulpes.generator.domain;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import net.onelitefeather.vulpes.generator.domain.error.ErrorResponse;

/**
 * Interface for code generation response DTOs.
 */
@Schema(description = "Response for code generation")
@Serdeable
public interface GenerateCodeResponse {

    /**
     * DTO for successful code generation response.
     */
    @Schema(description = "Successful code generation response")
    @Serdeable
    record GenerateCodeSuccessDTO(
            @Schema(description = "Status of the code generation", example = "success") String status,
            @Schema(description = "Message with details about the code generation", example = "Code generated successfully") String message
    ) implements GenerateCodeResponse {
        /**
         * Creates a success response with the given message.
         *
         * @param message the success message
         * @return a new GenerateCodeSuccessDTO instance
         */
        public static GenerateCodeSuccessDTO success(String message) {
            return new GenerateCodeSuccessDTO("success", message);
        }
    }

    /**
     * DTO for error code generation response.
     */
    @Schema(description = "Error code generation response")
    @Serdeable
    record GenerateCodeErrorDTO(
            @Schema(description = "Status of the code generation", example = "error") String status,
            @Schema(description = "Error message with details about the failure", example = "Failed to generate code") String errorMessage
    ) implements GenerateCodeResponse, ErrorResponse {
        /**
         * Creates an error response with the given message.
         *
         * @param message the error message
         * @return a new GenerateCodeErrorDTO instance
         */
        public static GenerateCodeErrorDTO error(String message) {
            return new GenerateCodeErrorDTO("error", message);
        }
    }
}