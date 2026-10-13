package net.onelitefeather.vulpes.generator.generation.sound;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.utils.Either;
import org.jetbrains.annotations.ApiStatus;

/**
 * Codec for one entry of the {@code sounds} array of a {@code sounds.json} event.
 * <p>
 * An entry is written as a plain name if all of its values are the Vanilla defaults, otherwise as an object. Fields
 * with a Vanilla default value are left out when encoding. The legacy type {@code type_file} is read and written as
 * {@code file}. Every value is validated in both directions, invalid values produce an error result.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public final class SoundEntryCodec {

    /**
     * The Vanilla default volume of a sound.
     */
    public static final float DEFAULT_VOLUME = 1f;

    /**
     * The Vanilla default pitch of a sound.
     */
    public static final float DEFAULT_PITCH = 1f;

    /**
     * The Vanilla default weight of a sound.
     */
    public static final int DEFAULT_WEIGHT = 1;

    /**
     * The Vanilla default attenuation distance of a sound.
     */
    public static final int DEFAULT_ATTENUATION_DISTANCE = 16;

    /**
     * The type of an entry that refers to a sound file.
     */
    public static final String TYPE_FILE = "file";

    /**
     * The type of an entry that refers to another sound event.
     */
    public static final String TYPE_EVENT = "event";

    /**
     * The legacy type value of the database, which is treated as {@link #TYPE_FILE}.
     */
    static final String LEGACY_TYPE_FILE = "type_file";

    private static final StructCodec<SoundEntry> STRUCT = StructCodec.struct(
            "name", Codec.STRING, SoundEntry::name,
            "volume", Codec.FLOAT.optional(), entry -> entry.volume() == DEFAULT_VOLUME ? null : entry.volume(),
            "pitch", Codec.FLOAT.optional(), entry -> entry.pitch() == DEFAULT_PITCH ? null : entry.pitch(),
            "weight", Codec.INT.optional(), entry -> entry.weight() == DEFAULT_WEIGHT ? null : entry.weight(),
            "stream", Codec.BOOLEAN.optional(), entry -> entry.stream() ? true : null,
            "attenuation_distance", Codec.INT.optional(),
            entry -> entry.attenuationDistance() == DEFAULT_ATTENUATION_DISTANCE ? null : entry.attenuationDistance(),
            "preload", Codec.BOOLEAN.optional(), entry -> entry.preload() ? true : null,
            "type", Codec.STRING.optional(), entry -> TYPE_FILE.equals(entry.type()) ? null : entry.type(),
            (name, volume, pitch, weight, stream, attenuationDistance, preload, type) -> new SoundEntry(
                    name,
                    orDefault(volume, DEFAULT_VOLUME),
                    orDefault(pitch, DEFAULT_PITCH),
                    orDefault(weight, DEFAULT_WEIGHT),
                    Boolean.TRUE.equals(stream),
                    orDefault(attenuationDistance, DEFAULT_ATTENUATION_DISTANCE),
                    Boolean.TRUE.equals(preload),
                    type == null ? TYPE_FILE : type
            )
    );

    /**
     * The codec of a sound entry. A plain name is decoded with the Vanilla defaults.
     */
    public static final Codec<SoundEntry> CODEC = Codec.Either(Codec.STRING, STRUCT)
            .transform(SoundEntryCodec::fromEither, SoundEntryCodec::toEither);

    private static SoundEntry fromEither(Either<String, SoundEntry> either) {
        return either.unify(
                name -> check(new SoundEntry(name, DEFAULT_VOLUME, DEFAULT_PITCH, DEFAULT_WEIGHT, false,
                        DEFAULT_ATTENUATION_DISTANCE, false, TYPE_FILE)),
                SoundEntryCodec::check
        );
    }

    private static Either<String, SoundEntry> toEither(SoundEntry entry) {
        SoundEntry checked = check(entry);
        if (checked.equals(defaults(checked.name()))) {
            return Either.left(checked.name());
        }
        return Either.right(checked);
    }

    /**
     * Validates an entry and normalizes its type.
     *
     * @param entry the entry to check
     * @return the checked entry with the normalized type
     * @throws IllegalArgumentException if a value is invalid
     */
    static SoundEntry check(SoundEntry entry) {
        String name = entry.name();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        requirePositive("volume", entry.volume());
        requirePositive("pitch", entry.pitch());
        if (entry.weight() < 1) {
            throw new IllegalArgumentException("weight must be at least 1, but was " + entry.weight());
        }
        if (entry.attenuationDistance() < 1) {
            throw new IllegalArgumentException("attenuation_distance must be at least 1, but was " + entry.attenuationDistance());
        }

        String type = LEGACY_TYPE_FILE.equals(entry.type()) ? TYPE_FILE : entry.type();
        if (!TYPE_FILE.equals(type) && !TYPE_EVENT.equals(type)) {
            throw new IllegalArgumentException("type must be file or event, but was " + type);
        }
        return new SoundEntry(name, entry.volume(), entry.pitch(), entry.weight(), entry.stream(),
                entry.attenuationDistance(), entry.preload(), type);
    }

    private static void requirePositive(String field, float value) {
        if (!(value > 0f)) {
            throw new IllegalArgumentException(field + " must be greater than 0, but was " + value);
        }
    }

    private static SoundEntry defaults(String name) {
        return new SoundEntry(name, DEFAULT_VOLUME, DEFAULT_PITCH, DEFAULT_WEIGHT, false,
                DEFAULT_ATTENUATION_DISTANCE, false, TYPE_FILE);
    }

    private static <T> T orDefault(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private SoundEntryCodec() {
        // Utility class, no instances
    }
}
