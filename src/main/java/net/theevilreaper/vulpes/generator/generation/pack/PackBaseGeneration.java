package net.theevilreaper.vulpes.generator.generation.pack;

import com.google.gson.JsonObject;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface PackBaseGeneration {

    /**
     * Generate the pack.mcmeta file for the resource pack.
     * @param packVersion the pack version
     * @param description the description of the pack
     * @return the generated pack.mcmeta file as {@link JsonObject}
     */
    default @NotNull JsonObject generatePackFile(
            int packVersion,
            @Nullable String description
    ) {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", packVersion);
        if (description != null) {
            pack.addProperty("description", description);
        }
        return pack;
    }
}
