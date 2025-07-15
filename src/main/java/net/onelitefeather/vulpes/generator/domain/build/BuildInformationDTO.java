package net.onelitefeather.vulpes.generator.domain.build;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Schema(
        requiredProperties = {
                "data"
        }
)
@Introspected
@Serdeable
public record BuildInformationDTO(@NotNull Map<String, String> data) {
}
