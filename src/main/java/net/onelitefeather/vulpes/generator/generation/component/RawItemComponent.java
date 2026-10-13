package net.onelitefeather.vulpes.generator.generation.component;

import org.jetbrains.annotations.ApiStatus;

/**
 * A data component of an item that is not interpreted by the generator, kept as its raw JSON text.
 *
 * @param key       the component key, for example {@code "minecraft:custom_name"}
 * @param jsonValue the JSON text of the component value
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public record RawItemComponent(String key, String jsonValue) {
}
