package net.onelitefeather.vulpes.generator.service.impl;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.dto.GenerateCodeRequest.ReferenceType;
import net.onelitefeather.vulpes.generator.registry.GeneratorRegistry;
import net.onelitefeather.vulpes.generator.registry.RegistryProvider;
import net.onelitefeather.vulpes.generator.service.FileService;
import net.onelitefeather.vulpes.generator.service.GeneratorService;
import net.onelitefeather.vulpes.generator.service.GitService;
import net.onelitefeather.vulpes.generator.service.RepositoryInitializerService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Implementation of the GeneratorService interface.
 */
@Singleton
public class GeneratorServiceImpl implements GeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(GeneratorServiceImpl.class);

    private final String gitRepoUrl;
    private final FileService fileService;
    private final GitService gitService;
    private final RepositoryInitializerService repositoryInitializerService;
    private final GeneratorRegistry generatorRegistry;

    @Inject
    public GeneratorServiceImpl(
            @Value("${git.repo.url:https://github.com/yourusername/your-repo.git}") String gitRepoUrl,
            FileService fileService,
            GitService gitService,
            RepositoryInitializerService repositoryInitializerService,
            RegistryProvider registryProvider
    ) {
        this.gitRepoUrl = gitRepoUrl;
        this.fileService = fileService;
        this.gitService = gitService;
        this.repositoryInitializerService = repositoryInitializerService;
        this.generatorRegistry = registryProvider.getGeneratorRegistry();
    }

    @Override
    public void generateProject(@NotNull String branchName) {
        // Call the overloaded method with default reference type (BRANCH)
        generateProject(branchName, ReferenceType.BRANCH);
    }

    @Override
    public void generateProject(@NotNull String reference, @NotNull ReferenceType referenceType) {
        Path generationDirectory = null;
        try {
            // Create a temporary repository
            String tempRepoPath = repositoryInitializerService.createTemporaryRepository(reference, referenceType);
            if (tempRepoPath.isEmpty()) {
                logger.error("Failed to create temporary repository for reference: {} (type: {})", reference, referenceType);
                return;
            }

            // Create a directory for generation
            generationDirectory = Path.of(tempRepoPath);

            // Delete the models package directory
            boolean deleteSuccess = fileService.deleteModelsPackage(generationDirectory);
            if (!deleteSuccess) {
                logger.warn("Models package directory not found or could not be deleted");
            }

            // Generate code using all registered generators
            Path javaPath = generationDirectory.resolve("src/main/java");
            generatorRegistry.triggerAll(javaPath);

            logger.info("Project generation completed for reference: {} (type: {})", reference, referenceType);
        } catch (Exception e) {
            logger.error("Error generating project for reference {} (type: {}): {}", reference, referenceType, e.getMessage(), e);
        } finally {
            // Clean up if necessary
            if (generationDirectory != null) {
                // Note: We don't delete the directory here as it will be needed for creating the ZIP
                // The controller will handle the cleanup after sending the ZIP to the client
            }
        }
    }

    @Override
    public Path generateProjectAndCreateZip(@NotNull String reference) {
        // Call the overloaded method with default reference type (BRANCH)
        return generateProjectAndCreateZip(reference, ReferenceType.BRANCH);
    }

    @Override
    public Path generateProjectAndCreateZip(@NotNull String reference, @NotNull ReferenceType referenceType) {
        Path generationDirectory = null;
        try {
            // Create a temporary repository
            String tempRepoPath = repositoryInitializerService.createTemporaryRepository(reference, referenceType);
            if (tempRepoPath.isEmpty()) {
                logger.error("Failed to create temporary repository for reference: {} (type: {})", reference, referenceType);
                return null;
            }

            // Create a directory for generation
            generationDirectory = Path.of(tempRepoPath);

            // Delete the models package directory
            boolean deleteSuccess = fileService.deleteModelsPackage(generationDirectory);
            if (!deleteSuccess) {
                logger.warn("Models package directory not found or could not be deleted");
            }

            // Generate code using all registered generators
            Path javaPath = generationDirectory.resolve("src/main/java");
            generatorRegistry.triggerAll(javaPath);

            // Create ZIP archive
            Path zipPath = fileService.createZipArchive(generationDirectory);

            logger.info("Project generation and ZIP creation completed for reference: {} (type: {})", reference, referenceType);
            return zipPath;
        } catch (Exception e) {
            logger.error("Error generating project for reference {} (type: {}): {}", reference, referenceType, e.getMessage(), e);
            return null;
        } finally {
            // Clean up the generation directory
            if (generationDirectory != null) {
                fileService.deleteGenerationDirectory(generationDirectory);
            }
        }
    }
}
