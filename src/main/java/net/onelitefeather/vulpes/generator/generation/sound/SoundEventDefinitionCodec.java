package net.onelitefeather.vulpes.generator.generation.sound;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.ApiStatus;

/**
 * Codec for the object of one sound event in a {@code sounds.json} file.
 * <p>
 * The fields are {@code replace}, {@code subtitle} and {@code sounds}. {@code replace} is only written when it is
 * {@code true} and {@code subtitle} only when it is present.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public final class SoundEventDefinitionCodec {

    /**
     * The codec of a sound event definition.
     */
    public static final StructCodec<SoundEventDefinition> CODEC = StructCodec.struct(
            "replace", Codec.BOOLEAN.optional(), definition -> definition.replace() ? Boolean.TRUE : null,
            "subtitle", Codec.STRING.optional(), SoundEventDefinition::subtitle,
            "sounds", SoundEntryCodec.CODEC.list(), SoundEventDefinition::sounds,
            (replace, subtitle, sounds) -> new SoundEventDefinition(Boolean.TRUE.equals(replace), subtitle, sounds)
    );

    private SoundEventDefinitionCodec() {
        // Utility class, no instances
    }
}
