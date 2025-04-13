package net.theevilreaper.vulpes.generator.generation;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

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

