package net.onelitefeather.vulpes.generator.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for code generation request.
 */
@Schema(description = "Request for generating code")
@Serdeable
public record GenerateCodeRequest(
        @Schema(description = "Git reference (branch, tag, or commit) to use for code generation", 
                example = "main", 
                requiredMode = Schema.RequiredMode.REQUIRED) 
        @NotNull 
        @NotBlank 
        String reference,

        @Schema(description = "Type of reference (BRANCH, TAG, or REFERENCE)", 
                example = "BRANCH", 
                requiredMode = Schema.RequiredMode.REQUIRED,
                defaultValue = "BRANCH") 
        @NotNull 
        ReferenceType referenceType
) {
    /**
     * Enum for reference types.
     */
    public enum ReferenceType {
        /**
         * Branch reference type.
         */
        BRANCH,

        /**
         * Tag reference type.
         */
        TAG,

        /**
         * Generic reference type (could be branch, tag, or commit).
         */
        REFERENCE
    }
}
