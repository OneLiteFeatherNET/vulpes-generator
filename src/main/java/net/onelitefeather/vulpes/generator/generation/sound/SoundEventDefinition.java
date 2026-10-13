package net.onelitefeather.vulpes.generator.generation.sound;

import net.onelitefeather.vulpes.api.model.sound.SoundEventEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.Comparator;
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

    /**
     * Maps a sound event of the database to its definition.
     *
     * @param entity the event to map, its sound data is loaded lazily
     * @return the definition of the event
     */
    public static SoundEventDefinition from(SoundEventEntity entity) {
        List<SoundEntry> entries = entity.getSoundData() == null ? List.of() : entity.getSoundData().stream()
                .map(SoundEntry::from)
                .sorted(Comparator.comparing(SoundEntry::name, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        // TODO(vulpes-model): use isReplace() once released
        return new SoundEventDefinition(false, entity.getSubTitle(), entries);
    }
}
