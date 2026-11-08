package net.onelitefeather.vulpes.generator.generation.sound;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * One sound event of a {@code sounds.json} file, without its key.
 *
 * @param replace  whether the event replaces the Vanilla event with the same key
 * @param subtitle the subtitle of the event, {@code null} if the event has none
 * @param sounds   the entries of the event, sorted by name so the output is deterministic
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public record SoundEventDefinition(boolean replace, String subtitle, List<SoundEntry> sounds) {

    /**
     * Creates the definition and copies the given entries.
     *
     * @param replace  whether the event replaces the Vanilla event
     * @param subtitle the subtitle, may be {@code null}
     * @param sounds   the entries, {@code null} is treated as an empty list
     */
    public SoundEventDefinition {
        sounds = sounds == null ? List.of() : List.copyOf(sounds);
    }
}
