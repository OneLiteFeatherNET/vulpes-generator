package net.onelitefeather.vulpes.generator.domain.generation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.generation.exception.GenerationException;
import net.onelitefeather.vulpes.generator.git.GitProjectWorker;
import net.onelitefeather.vulpes.generator.registry.GeneratorRegistry;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.onelitefeather.vulpes.generator.domain.generation.exception.GenerationException.*;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;
import static net.onelitefeather.vulpes.generator.util.Constants.OUT_PUT_FOLDER;
import static net.onelitefeather.vulpes.generator.util.Constants.TEMP_PREFIX;

/**
 * Service implementation which provides the functionality to generate the additional part for the Vulpes project.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 */
@Singleton
public class VulpesGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VulpesGenerationService.class);
    private final GeneratorRegistry registry;
    private final GitProjectWorker gitProjectWorker;

    /**
     * Creates a new instance of the service.
     *
     * @param registry         the generator registry
     * @param gitProjectWorker the git project worker
     */
    @Inject
    public VulpesGenerationService(GeneratorRegistry registry, GitProjectWorker gitProjectWorker) {
        this.registry = registry;
        this.gitProjectWorker = gitProjectWorker;
    }

    /**
     * Triggers the generation logic for the Vulpes build
     *
     * @param targetBranch the branch to generate from
     * @return the path to the generated code base
     * @throws GenerationException when something went wrong during a generation
     */
    public Path getVulpesGeneration(String targetBranch) throws GenerationException {
        Path tempPath;
        try {
            tempPath = Files.createTempDirectory(TEMP_PREFIX);
        } catch (IOException exception) {
            LOGGER.error("Unable to create temp directory for generation", exception);
            throw new GenerationException(Type.VULPES, "Unable to create temp directory for generation");
        }
        Path output = tempPath.resolve(OUT_PUT_FOLDER);
        Path javaPath = output.resolve(JAVA_MAIM_FOLDER);

        try {
            Files.createDirectories(output);
        } catch (IOException e) {
            LOGGER.error("Unable to create output directory for generation", e);
            throw new GenerationException(Type.VULPES, "Unable to create output directory for generation");
        }

        try (Git git = gitProjectWorker.cloneBaseRepo(output, List.of("refs/heads/" + targetBranch))) {
            if (git == null) {
                LOGGER.warn("Git clone operation failed for branch: {}", targetBranch);
                throw new GenerationException(Type.VULPES, "Git clone operation failed for branch: " + targetBranch);
            }
        }

        registry.triggerAll(javaPath);
        return output;
    }
}
