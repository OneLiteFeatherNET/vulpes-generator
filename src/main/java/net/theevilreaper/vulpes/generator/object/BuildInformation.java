package net.theevilreaper.vulpes.generator.object;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Serdeable
@Introspected
public record BuildInformation(@NotNull Map<String, String> data) {
}
