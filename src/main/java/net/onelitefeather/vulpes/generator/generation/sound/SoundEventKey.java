package net.onelitefeather.vulpes.generator.generation.sound;

import net.kyori.adventure.key.Key;
import net.onelitefeather.vulpes.api.model.sound.SoundEventEntity;
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
     * Resolves the key of a sound event entity.
     *
     * @param entity the sound event to resolve
     * @return the validated key of the event
     * @throws IllegalArgumentException if the key name is missing, has no project or does not form a valid key
     */
    public static @NotNull SoundEventKey of(@NotNull SoundEventEntity entity) {
        String keyName = entity.getKeyName();
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("sound '" + entity.getKey() + "' has no key name");
        }

        String namespace;
        String path;
        int separator = keyName.indexOf(':');
        if (separator >= 0) {
            namespace = keyName.substring(0, separator);
            path = keyName.substring(separator + 1);
        } else {
            if (entity.getProject() == null) {
                throw new IllegalArgumentException("sound '" + keyName + "' has no project");
            }
            String namespacedKey = entity.getNamespacedKey();
            namespace = namespacedKey.substring(0, namespacedKey.indexOf(':'));
            path = keyName;
        }

        try {
            Key.key(namespace, path);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("sound '" + keyName + "' has an invalid key: " + e.getMessage(), e);
        }
        return new SoundEventKey(namespace, path);
    }

    /**
     * Returns the namespaced key as text, for example {@code eldoria:ui.click}.
     *
     * @return the namespaced key
     */
    public @NotNull String asString() {
        return namespace + ":" + path;
    }
}
