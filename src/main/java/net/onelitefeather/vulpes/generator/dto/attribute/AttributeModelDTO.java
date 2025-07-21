package net.onelitefeather.vulpes.generator.dto.attribute;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import net.onelitefeather.vulpes.api.model.AttributeEntity;

import java.util.UUID;

@Schema()
@Serdeable
public class AttributeModelDTO {
    private final UUID id;
    private final String uiName;
    private final String variableName;
    private final double defaultValue;
    private final double maximumValue;


    public AttributeModelDTO(
            @Schema(description = "ID of the attribute", requiredMode = Schema.RequiredMode.NOT_REQUIRED) UUID id,
            @Schema(description = "The name for the ui", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @NotEmpty String uiName,
            @Schema(description = "The name which represents the variable after the generation", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @NotEmpty String variableName,
            @Schema(description = "Default value of the attribute", requiredMode = Schema.RequiredMode.REQUIRED) double defaultValue,
            @Schema(description = "Maximum value of the attribute", requiredMode = Schema.RequiredMode.REQUIRED) double maximumValue) {
        this.id = id;
        this.uiName = uiName;
        this.variableName = variableName;
        this.defaultValue = defaultValue;
        this.maximumValue = maximumValue;
    }

    public UUID getId() {
        return id;
    }

    public String getUiName() {
        return uiName;
    }

    public String getVariableName() {
        return variableName;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public double getMaximumValue() {
        return maximumValue;
    }

    public AttributeEntity toAttributeModel() {
        return new AttributeEntity(id, uiName, variableName, defaultValue, maximumValue);
    }
}
