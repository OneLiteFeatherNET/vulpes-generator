package net.onelitefeather.vulpes.generator.generation.component;

import net.minestom.server.codec.Codec;
import org.jetbrains.annotations.ApiStatus;

/**
 * Codec for the stack amount of an item, stored as a JSON number.
 * <p>
 * Only amounts between {@link #MIN_AMOUNT} and {@link #MAX_AMOUNT} (both inclusive) are accepted in both directions.
 * Any other value produces an error result.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public final class AmountCodec {

    /**
     * The smallest accepted stack amount.
     */
    public static final int MIN_AMOUNT = 1;

    /**
     * The largest accepted stack amount.
     */
    public static final int MAX_AMOUNT = 99;

    /**
     * The codec which maps a JSON number to a validated stack amount and back.
     */
    public static final Codec<Integer> CODEC = Codec.INT.transform(AmountCodec::validate, AmountCodec::validate);

    /**
     * Checks that the given amount lies in the accepted range.
     *
     * @param amount the amount to check
     * @return the same amount if it is valid
     * @throws IllegalArgumentException if the amount is outside {@link #MIN_AMOUNT} to {@link #MAX_AMOUNT}
     */
    private static Integer validate(Integer amount) {
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("amount must be between " + MIN_AMOUNT + " and " + MAX_AMOUNT + ", but was " + amount);
        }
        return amount;
    }

    private AmountCodec() {
        // Utility class, no instances
    }
}
