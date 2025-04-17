package net.theevilreaper.vulpes.generator.properties;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Property;
import org.jetbrains.annotations.Nullable;

/**
 * {@link PackProperties} provides a configurable way to set important data for the resource pack.
 * If the data provided via a controller does not meet the required criteria, the system will fall back to default values.
 * This configuration class enables that behavior: if the service configuration does not contain the necessary data, code-defined default values are used.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@ConfigurationProperties("vulpes.pack")
public record PackProperties(
        @Property(name = "pack.version", defaultValue = "46") int packVersion,
        @Nullable @Property(name = "description", defaultValue = "Default description") String description
) {
}
