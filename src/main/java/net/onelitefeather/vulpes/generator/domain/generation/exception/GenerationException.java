package net.onelitefeather.vulpes.generator.domain.generation.exception;

/**
 * Exception thrown when something goes wrong during the generation process.
 * <p>
 * This exception helps distinguish between different types of generation failures
 * by providing a {@link Type} that indicates whether the issue occurred during
 * Vulpes generation or ResourcePack generation.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @see Type
 * @since 0.1.0
 */
public final class GenerationException extends RuntimeException {

    private final Type type;

    /**
     * Creates a new generation exception with the specified type and message.
     *
     * @param type    the type of generation that failed
     * @param message a detailed description of what went wrong
     */
    public GenerationException(Type type, String message) {
        super(message);
        this.type = type;
    }

    /**
     * Returns the type of generation that failed.
     *
     * @return the generation type
     */
    public Type getType() {
        return type;
    }

    /**
     * Identifies which type of generation process failed.
     */
    public enum Type {

        /**
         * Indicates a failure during Vulpes code generation
         */
        VULPES,

        /**
         * Indicates a failure during ResourcePack generation
         */
        PACK
    }
}
