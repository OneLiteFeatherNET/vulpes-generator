package net.onelitefeather.vulpes.generator.service;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Service for Git operations.
 */
public interface GitService {

    /**
     * Clones a Git repository.
     *
     * @param repoUrl the URL of the repository to clone
     * @param branch the branch, tag, or commit to checkout
     * @param targetDirectory the directory to clone the repository to
     * @return true if the clone was successful, false otherwise
     */
    boolean cloneRepository(@NotNull String repoUrl, @NotNull String branch, @NotNull String targetDirectory);

    /**
     * Cleans up the Git repository directory.
     *
     * @param targetDirectory the directory to clean up
     * @return true if the cleanup was successful, false otherwise
     */
    boolean cleanupRepository(@NotNull String targetDirectory);

    /**
     * Lists all branches in the repository.
     *
     * @param repoUrl the URL of the repository
     * @return a list of branch names
     */
    @NotNull List<String> listBranches(@NotNull String repoUrl);

    /**
     * Lists all tags in the repository.
     *
     * @param repoUrl the URL of the repository
     * @return a list of tag names
     */
    @NotNull List<String> listTags(@NotNull String repoUrl);

    /**
     * Lists all references (branches and tags) in the repository.
     *
     * @param repoUrl the URL of the repository
     * @return a list of reference names
     */
    @NotNull List<String> listReferences(@NotNull String repoUrl);
}
