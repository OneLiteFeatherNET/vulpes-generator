package net.theevilreaper.vulpes.generator.git;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.domain.configuration.GithubConfiguration;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * A singleton service responsible for interacting with a remote Git repository.
 * <p>
 * This class provides functionality to clone and checkout specific references (branches, tags, commits)
 * from a remote Git repository using credentials provided in the application configuration.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>
 * {@code
 * @Inject
 * private GitWorker gitWorker;
 *
 * gitWorker.cloneAndCheckout("main", Paths.get("/local/path"));
 * }
 * </pre>
 */
@Singleton
public final class GitProjectWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitProjectWorker.class);

    private final GithubConfiguration configuration;
    private final UsernamePasswordCredentialsProvider credentialsProvider;

    /**
     * Constructs a new GitWorker instance.
     * @param configuration the {@link GithubConfiguration} which contains several important data
     */
    @Inject
    public GitProjectWorker(GithubConfiguration configuration) {
        this.configuration = configuration;
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(
                this.configuration.username(),
                this.configuration.password()
        );
    }

    /**
     * Clones the base repository to the specified path and checks out the configured branch.
     * @param path the local directory where the repository should be cloned.
     */
    public Git cloneBaseRepo(@NotNull Path path) {
        return this.cloneBaseRepo(path, null);
    }

    /**
     * Clones the base repository to the specified path and checks out the specified branches.
     * @param path the local directory where the repository should be cloned.
     * @param branches the list of branches to clone. If null, all branches will be cloned.
     */
    public Git cloneBaseRepo(@NotNull Path path, List<String> branches) {
        try {
            CloneCommand rawGit = Git.cloneRepository()
                    //.setCredentialsProvider(this.credentialsProvider)
                    .setURI(this.configuration.cloneUrl())
                    .setDirectory(path.toFile());

            if (branches == null) {
                rawGit.setCloneAllBranches(true);
            } else {
                rawGit.setBranchesToClone(branches);
            }
            return rawGit.call();
        } catch (Exception exception) {
            LOGGER.warn("Something went wrong during the fetch process", exception);
            return null;
        }
    }

    /**
     * @param git
     */
    public void push(@NotNull PushCommand git) {
        try {
            git.call();
        } catch (GitAPIException exception) {
            LOGGER.info("Unable to push content to ", exception);
        }
    }
}
