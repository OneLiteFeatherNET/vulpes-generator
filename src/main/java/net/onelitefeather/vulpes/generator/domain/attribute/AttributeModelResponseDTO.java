package net.onelitefeather.vulpes.generator.domain.attribute;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import net.onelitefeather.vulpes.api.model.AttributeEntity;
import net.onelitefeather.vulpes.generator.domain.error.ErrorResponse;

import java.util.UUID;

@Schema(description = "Response DTO for Attribute Model")
@Serdeable
public interface AttributeModelResponseDTO {

    @Schema(description = "Attribute Model Data")
    @Serdeable
    record AttributeModelDTO(
            @Schema(description = "UUID of the Attribute Model") UUID id,
            @Schema(description = "The name for the ui") String uiName,
            @Schema(description = "The name which represents the variable after the generation") String variableName,
            @Schema(description = "Default value of the attribute") double defaultValue,
            @Schema(description = "Maximum value of the attribute") double maximumValue
    ) implements AttributeModelResponseDTO {

        /**
         * Creates a DTO from an AttributeEntity.
         *
         * @param model the AttributeEntity to convert
         * @return a new AttributeModelDTO instance
         */
        public static AttributeModelDTO create(AttributeEntity model) {
            return new AttributeModelDTO(
                    model.getId(),
                    model.getUiName(),
                    model.getVariableName(),
                    model.getDefaultValue(),
                    model.getMaximumValue()
            );
        }
    }

    @Schema(description = "Error message for Attribute Model")
    @Serdeable
    record AttributeModelErrorDTO(
            @Schema(description = "Error message") String errorMessage
    ) implements AttributeModelResponseDTO, ErrorResponse {
    }
}