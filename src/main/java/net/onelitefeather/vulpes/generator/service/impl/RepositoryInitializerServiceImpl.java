package net.onelitefeather.vulpes.generator.service.impl;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.dto.GenerateCodeRequest.ReferenceType;
import net.onelitefeather.vulpes.generator.service.GitService;
import net.onelitefeather.vulpes.generator.service.RepositoryInitializerService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

/**
 * Implementation of the RepositoryInitializerService interface.
 * Also listens for application startup events to initialize the repository.
 */
@Singleton
public class RepositoryInitializerServiceImpl implements RepositoryInitializerService, ApplicationEventListener<StartupEvent> {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryInitializerServiceImpl.class);
    private static final String BASE_REPO_DIR = "base-repo";
    private static final String TEMP_REPO_DIR = "temp-repo";

    private final String gitRepoUrl;
    private final String gitCommitAuthor;
    private final GitService gitService;
    private Git git;

    /**
     * Constructor with dependencies.
     *
     * @param gitRepoUrl the URL of the Git repository
     * @param gitCommitAuthor the author to use for Git commits
     * @param gitService the Git service
     */
    @Inject
    public RepositoryInitializerServiceImpl(
            @Value("${git.repo.url:https://github.com/yourusername/your-repo.git}") String gitRepoUrl,
            @Value("${git.commit.author:Vulpes Generator <generator@onelitefeather.net>}") String gitCommitAuthor,
            GitService gitService
    ) {
        this.gitRepoUrl = gitRepoUrl;
        this.gitCommitAuthor = gitCommitAuthor;
        this.gitService = gitService;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        logger.info("Application started, initializing repository...");
        initializeRepository();
    }

    @Override
    public boolean initializeRepository() {
        try {
            Path repoDir = Path.of(BASE_REPO_DIR);

            // Clean up existing directory if it exists
            if (Files.exists(repoDir)) {
                deleteDirectory(repoDir);
            }

            // Create directory
            Files.createDirectories(repoDir);

            // Clone the repository
            logger.info("Cloning repository from {} to {}", gitRepoUrl, BASE_REPO_DIR);
            git = Git.cloneRepository()
                    .setURI(gitRepoUrl)
                    .setDirectory(repoDir.toFile())
                    .call();

            logger.info("Repository initialized successfully");
            return true;
        } catch (GitAPIException | IOException e) {
            logger.error("Failed to initialize repository: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateRepository() {
        try {
            if (git == null) {
                logger.warn("Repository not initialized, initializing now...");
                return initializeRepository();
            }

            logger.info("Updating repository...");
            git.pull().call();
            logger.info("Repository updated successfully");
            return true;
        } catch (GitAPIException e) {
            logger.error("Failed to update repository: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public @NotNull String createTemporaryRepository(@NotNull String reference) {
        // Call the overloaded method with default reference type (BRANCH)
        return createTemporaryRepository(reference, ReferenceType.BRANCH);
    }

    @Override
    public @NotNull String createTemporaryRepository(@NotNull String reference, @NotNull ReferenceType referenceType) {
        try {
            // Update the base repository first
            updateRepository();

            // Create a unique directory for this temporary repository
            String tempRepoPath = TEMP_REPO_DIR + "/" + reference + "-" + UUID.randomUUID();
            Path tempRepoDir = Path.of(tempRepoPath);

            // Create the directory
            Files.createDirectories(tempRepoDir);

            // Copy the base repository to the temporary directory
            copyDirectory(Path.of(BASE_REPO_DIR), tempRepoDir);

            // Open the temporary repository
            try (Git tempGit = Git.open(tempRepoDir.toFile())) {
                // Checkout the specified reference
                // For now, we handle all reference types the same way
                // In the future, we could add special handling for different reference types
                tempGit.checkout()
                        .setName(reference)
                        .call();

                logger.info("Temporary repository created at: {} with reference type: {}", tempRepoPath, referenceType);
            }

            return tempRepoPath;
        } catch (GitAPIException | IOException e) {
            logger.error("Failed to create temporary repository: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Deletes a directory and all its contents using NIO.
     *
     * @param directory the directory to delete
     * @throws IOException if an I/O error occurs
     */
    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Copies a directory and all its contents using NIO.
     *
     * @param source the source directory
     * @param target the target directory
     * @throws IOException if an I/O error occurs
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
