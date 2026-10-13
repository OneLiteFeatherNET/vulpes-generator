package net.onelitefeather.vulpes.generator.generation.component;

import com.google.gson.JsonElement;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * An advancement of a project, validated and converted to the values that the generators emit.
 * <p>
 * Instances are created by {@link AdvancementTree}, which also guarantees that the parent of an advancement comes
 * before the advancement itself in the list it returns.
 *
 * @param key               the local key of the advancement, for example {@code first_kill}
 * @param namespacedKey     the key with the project namespace, for example {@code vulpes:first_kill}
 * @param variableName      the upper case variable name of the advancement
 * @param uiName            the user interface name, {@code null} if not set
 * @param parentNamespacedKey the namespaced key of the parent, {@code null} for a root
 * @param rootNamespacedKey the namespaced key of the root this advancement belongs to, used as tab id
 * @param material          the icon of the advancement
 * @param frameType         the Minestom frame type of the advancement
 * @param title             the parsed title as JSON text component, {@code null} if not set
 * @param description       the parsed description as JSON text component, {@code null} if not set
 * @param background        the background resource location, only set for roots, {@code null} otherwise
 * @param x                 the horizontal position in the advancement tree
 * @param y                 the vertical position in the advancement tree
 * @param showToast         whether a toast is shown when the advancement is achieved
 * @param announceToChat    whether the achievement is announced in the chat, not supported by Minestom
 * @param hidden            whether the advancement stays hidden until it is achieved
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public record AdvancementData(
        String key,
        String namespacedKey,
        String variableName,
        @Nullable String uiName,
        @Nullable String parentNamespacedKey,
        String rootNamespacedKey,
        Material material,
        FrameType frameType,
        @Nullable JsonElement title,
        @Nullable JsonElement description,
        @Nullable String background,
        float x,
        float y,
        boolean showToast,
        boolean announceToChat,
        boolean hidden
) {

    /**
     * Returns whether this advancement is a root, which has no parent.
     *
     * @return {@code true} if there is no parent
     */
    public boolean isRoot() {
        return parentNamespacedKey == null;
    }
}
