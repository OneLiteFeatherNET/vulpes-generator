package net.onelitefeather.vulpes.generator.service;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Service for file operations.
 */
public interface FileService {

    /**
     * Creates a directory for code generation.
     *
     * @param branchName the name of the branch to generate code for
     * @return the path to the created directory
     */
    Path createGenerationDirectory(@NotNull String branchName);

    /**
     * Deletes a generation directory.
     *
     * @param generationDirectory the path to the directory to delete
     */
    void deleteGenerationDirectory(@NotNull Path generationDirectory);

    /**
     * Creates a ZIP archive of the generated code.
     *
     * @param sourcePath the path to the source directory
     * @return the path to the created ZIP file
     */
    Path createZipArchive(@NotNull Path sourcePath);

    /**
     * Deletes the models package directory from the cloned repository.
     *
     * @param repoPath the path to the cloned repository
     * @return true if the directory was deleted, false otherwise
     */
    boolean deleteModelsPackage(@NotNull Path repoPath);
}
