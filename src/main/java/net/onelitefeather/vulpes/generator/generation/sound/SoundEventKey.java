package net.onelitefeather.vulpes.generator.generation.sound;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The namespaced key of a sound event, split into its namespace and its path.
 * <p>
 * The namespace is taken from the key name of the event. A key name without namespace uses the namespace of the
 * project key. The path is the JSON key of the event in the {@code sounds.json} of the namespace.
 *
 * @param namespace the namespace of the event, for example {@code eldoria}
 * @param path      the path of the event, for example {@code ui.click}
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public record SoundEventKey(String namespace, String path) {

    /**
     * Returns the namespaced key as text, for example {@code eldoria:ui.click}.
     *
     * @return the namespaced key
     */
    public @NotNull String asString() {
        return namespace + ":" + path;
    }
}
