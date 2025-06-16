package net.theevilreaper.vulpes.generator.util;

import net.minestom.server.advancements.FrameType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for parsing {@link FrameType} from string keys.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 */
@ApiStatus.Internal
public final class FrameTypeParser {

    private static final FrameType[] FRAME_TYPES = FrameType.values();

    /**
     * Parses a frame type from a given key.
     *
     * @param key the key to parse the frame type from, can be null or empty.
     * @return the parsed frame type, or {@link FrameType#TASK} if the key is null or empty, or if the key does not correspond to a valid frame type.
     */
    public static @NotNull FrameType fromKey(String key) {
        if (key == null || key.isEmpty()) {
            return FrameType.TASK; // Default frame type if key is null or empty
        }

        FrameType type = null;
        for (int i = 0; i < FRAME_TYPES.length && type == null; i++) {
            FrameType current = FRAME_TYPES[i];
            if (key.equalsIgnoreCase(current.name())) {
                type = current;
            }
        }

        return type == null ? FrameType.TASK : type; // Return the found frame type or default if no match found
    }

    private FrameTypeParser() {

    }
}
