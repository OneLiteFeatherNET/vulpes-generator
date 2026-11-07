package net.onelitefeather.vulpes.generator.release.codegen;

/**
 * Signals that the code generator refused the snapshot content, for example an element it cannot express in Java.
 * The adapter to the code generator library translates the library's own refusal into this exception.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public class CodegenRefusedException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message the message of the generator, which names the refused content
     */
    public CodegenRefusedException(String message) {
        super(message);
    }
}
