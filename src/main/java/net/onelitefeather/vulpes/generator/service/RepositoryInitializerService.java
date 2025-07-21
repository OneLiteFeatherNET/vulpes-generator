package net.onelitefeather.vulpes.generator.service;

import net.onelitefeather.vulpes.generator.dto.GenerateCodeRequest.ReferenceType;
import org.jetbrains.annotations.NotNull;

/**
 * Service for initializing and updating the Git repository.
 */
public interface RepositoryInitializerService {

    /**
     * Initializes the repository by cloning it.
     * This method should be called once at application startup.
     *
     * @return true if initialization was successful, false otherwise
     */
    boolean initializeRepository();

    /**
     * Updates the repository by pulling the latest changes.
     * This method should be called before listing branches or generating code.
     *
     * @return true if update was successful, false otherwise
     */
    boolean updateRepository();

    /**
     * Creates a temporary copy of the repository for code generation.
     *
     * @param reference the Git reference (branch, tag, or commit) to checkout
     * @return the path to the temporary repository, or null if creation failed
     */
    @NotNull String createTemporaryRepository(@NotNull String reference);

    /**
     * Creates a temporary copy of the repository for code generation.
     *
     * @param reference the Git reference to checkout
     * @param referenceType the type of reference (branch, tag, or generic reference)
     * @return the path to the temporary repository, or null if creation failed
     */
    @NotNull String createTemporaryRepository(@NotNull String reference, @NotNull ReferenceType referenceType);
}
