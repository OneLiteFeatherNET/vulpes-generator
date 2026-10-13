package net.onelitefeather.vulpes.generator.generation.sound;

import net.onelitefeather.vulpes.api.model.sound.SoundFileSource;
import org.jetbrains.annotations.ApiStatus;

/**
 * One entry of the {@code sounds} array of a {@code sounds.json} event.
 * <p>
 * The values are the raw values of the entry. Validation and the Vanilla defaults are applied by the
 * {@link SoundEntryCodec}.
 *
 * @param name                the sound file path or the event name
 * @param volume              the volume of the sound
 * @param pitch               the pitch of the sound
 * @param weight              the weight of the sound in the event
 * @param stream              whether the sound is streamed
 * @param attenuationDistance the attenuation distance of the sound
 * @param preload             whether the sound is preloaded
 * @param type                the type of the entry, {@code file} or {@code event}
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public record SoundEntry(
        String name,
        float volume,
        float pitch,
        int weight,
        boolean stream,
        int attenuationDistance,
        boolean preload,
        String type
) {

    /**
     * Creates the entry of a sound file source of the database.
     *
     * @param source the source to map
     * @return the entry with the values of the source
     */
    public static SoundEntry from(SoundFileSource source) {
        return new SoundEntry(
                source.getName(),
                source.getVolume(),
                source.getPitch(),
                source.getWeight(),
                source.isStreamable(),
                source.getAttenuationDistance(),
                source.isPreloadable(),
                source.getType()
        );
    }
}
