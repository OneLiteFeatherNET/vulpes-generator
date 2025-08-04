package net.theevilreaper.vulpes.generator.generation;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * The {@link Generator} interface defines the base method structure which each implementations needs to work with the current system approach.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 */
public interface Generator {

    /**
     * Each generator must implement his own logic for this method
     */
    void generate(@NotNull Path javaPath);

    /**
     * Returns the name of the generator
     */
    @NotNull String getName();

    /**
     * Cleanup the generator
     */
    void cleanup();
}

