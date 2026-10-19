package net.onelitefeather.vulpes.generator.generation;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;

/**
 * The {@link Generator} interface defines the base method structure which each implementations needs to work with the current system approach.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 */
public interface Generator {

    /**
     * Writes the generated files below the given project root. Java generators resolve
     * {@code src/main/java}, resource generators resolve {@code src/main/resources}.
     *
     * @param root      the project root directory of the generated code base
     * @param projectId the project to generate the files for
     */
    void generate(@NotNull Path root, @NotNull UUID projectId);

    /**
     * Returns the name of the generator
     */
    @NotNull String getName();

    /**
     * Cleanup the generator
     */
    void cleanup();
}

