package net.theevilreaper.vulpes.generator.domain.pack;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Min;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link PackRequest} contains all relevant information which are needed to generate a resource pack.
 *
 * @param packFormat  the pack format
 * @param packOption the pack options
 * @param description the description of the pack
 * @version 1.0.0
 * @since 1.0.0
 * @author theEvilReaper
 */
@Serdeable
public record PackRequest(
        @Min(value = 1, message = "The pack format must be greater than 0")
        int packFormat,
        @NotNull PackOption packOption,
        @Nullable String description
) {
}
