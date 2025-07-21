package net.onelitefeather.vulpes.generator.service;

import net.onelitefeather.vulpes.generator.dto.GenerateCodeRequest.ReferenceType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Service for generating code.
 */
public interface GeneratorService {

    /**
     * Generates a project based on the provided branch name.
     *
     * @param branchName the name of the branch to generate the project for
     */
    void generateProject(@NotNull String branchName);

    /**
     * Generates a project based on the provided reference and reference type.
     *
     * @param reference the Git reference to use
     * @param referenceType the type of reference (branch, tag, or generic reference)
     */
    void generateProject(@NotNull String reference, @NotNull ReferenceType referenceType);

    /**
     * Generates a project and creates a ZIP archive of the generated code.
     *
     * @param reference the Git reference (branch, tag, or commit) to use
     * @return the path to the created ZIP file, or null if generation failed
     */
    Path generateProjectAndCreateZip(@NotNull String reference);

    /**
     * Generates a project and creates a ZIP archive of the generated code.
     *
     * @param reference the Git reference to use
     * @param referenceType the type of reference (branch, tag, or generic reference)
     * @return the path to the created ZIP file, or null if generation failed
     */
    Path generateProjectAndCreateZip(@NotNull String reference, @NotNull ReferenceType referenceType);
}
