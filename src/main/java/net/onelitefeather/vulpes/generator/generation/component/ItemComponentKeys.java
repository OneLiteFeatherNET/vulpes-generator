package net.onelitefeather.vulpes.generator.generation.component;

import net.minestom.server.item.Material;
import org.jetbrains.annotations.ApiStatus;

/**
 * Constants for the item component keys that the Vulpes backend stores next to the regular Minecraft data components.
 * <p>
 * {@link #MATERIAL} is required by the backend and {@link #AMOUNT} is optional. When a key is missing the
 * corresponding default is used. Every key in the {@link #CUSTOM_NAMESPACE} namespace is reserved for Vulpes.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public final class ItemComponentKeys {

    /**
     * The component key which holds the material of the item as a JSON string.
     */
    public static final String MATERIAL = "stelaris:material";

    /**
     * The component key which holds the stack amount of the item as a JSON number.
     */
    public static final String AMOUNT = "stelaris:amount";

    /**
     * The namespace which is reserved for Vulpes specific component keys.
     */
    public static final String CUSTOM_NAMESPACE = "stelaris";

    /**
     * The material which is used when no {@link #MATERIAL} component is present.
     */
    public static final Material DEFAULT_MATERIAL = Material.DIRT;

    /**
     * The amount which is used when no {@link #AMOUNT} component is present.
     */
    public static final int DEFAULT_AMOUNT = 1;

    private ItemComponentKeys() {
        // Utility class, no instances
    }
}
