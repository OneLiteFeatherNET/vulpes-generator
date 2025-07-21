package net.onelitefeather.vulpes.generator.service.impl;

import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.service.GitService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the GitService interface.
 */
@Singleton
public class GitServiceImpl implements GitService {

    private static final Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);
    private static final String TEMP_DIR = "temp";

    @Override
    public boolean cloneRepository(@NotNull String repoUrl, @NotNull String branch, @NotNull String targetDirectory) {
        try {
            Path directory = Path.of(targetDirectory);

            // Clean up existing directory if it exists
            if (Files.exists(directory)) {
                cleanupRepository(targetDirectory);
            }

            // Create directory if it doesn't exist
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // Clone the repository
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(directory.toFile())
                    .setBranch(branch)
                    .call();

            logger.info("Repository cloned successfully to {}", targetDirectory);
            return true;
        } catch (GitAPIException | IOException e) {
            logger.error("Failed to clone repository: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean cleanupRepository(@NotNull String targetDirectory) {
        try {
            Path directory = Path.of(targetDirectory);
            if (Files.exists(directory)) {
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
                logger.info("Repository directory cleaned up: {}", targetDirectory);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to clean up repository directory: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public @NotNull List<String> listBranches(@NotNull String repoUrl) {
        Git git = null;
        Path tempDir = null;

        try {
            // Create a temporary directory for cloning
            tempDir = Files.createDirectories(Path.of(TEMP_DIR, UUID.randomUUID().toString()));

            // Clone the repository without checking out any branch (bare clone)
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile())
                    .setBare(true)
                    .call();

            // List all branches
            List<Ref> branches = git.branchList()
                    .setListMode(ListBranchCommand.ListMode.ALL)
                    .call();

            // Extract branch names
            return branches.stream()
                    .map(ref -> ref.getName().replaceFirst("^refs/heads/", "")
                                    .replaceFirst("^refs/remotes/origin/", ""))
                    .distinct()
                    .collect(Collectors.toList());
        } catch (GitAPIException | IOException e) {
            logger.error("Failed to list branches: {}", e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            // Close Git repository
            if (git != null) {
                git.close();
            }

            // Clean up temporary directory
            cleanupTempDir(tempDir);
        }
    }

    @Override
    public @NotNull List<String> listTags(@NotNull String repoUrl) {
        Git git = null;
        Path tempDir = null;

        try {
            // Create a temporary directory for cloning
            tempDir = Files.createDirectories(Path.of(TEMP_DIR, UUID.randomUUID().toString()));

            // Clone the repository without checking out any branch (bare clone)
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile())
                    .setBare(true)
                    .call();

            // List all tags
            List<Ref> tags = git.tagList().call();

            // Extract tag names
            return tags.stream()
                    .map(ref -> ref.getName().replaceFirst("^refs/tags/", ""))
                    .collect(Collectors.toList());
        } catch (GitAPIException | IOException e) {
            logger.error("Failed to list tags: {}", e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            // Close Git repository
            if (git != null) {
                git.close();
            }

            // Clean up temporary directory
            cleanupTempDir(tempDir);
        }
    }

    @Override
    public @NotNull List<String> listReferences(@NotNull String repoUrl) {
        List<String> branches = listBranches(repoUrl);
        List<String> tags = listTags(repoUrl);

        // Combine branches and tags
        List<String> references = new ArrayList<>(branches);
        references.addAll(tags);
        return references;
    }

    /**
     * Helper method to clean up a temporary directory.
     *
     * @param tempDir the directory to clean up
     */
    private void cleanupTempDir(Path tempDir) {
        if (tempDir != null) {
            try {
                Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
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
            } catch (IOException e) {
                logger.error("Failed to clean up temporary directory: {}", e.getMessage(), e);
            }
        }
    }
}
