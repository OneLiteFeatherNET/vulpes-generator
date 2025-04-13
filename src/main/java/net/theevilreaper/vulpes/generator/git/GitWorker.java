package net.theevilreaper.vulpes.generator.git;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.properties.GitlabProperties;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
 *
 * <p>
 * This class uses {@link GitlabProperties} for configuration and is managed as a singleton by Micronaut.
 * </p>
 *
 * @see GitlabProperties
 */
@Singleton
@Requires(beans = GitlabProperties.class) // Ensures GitWorker is only created if GitlabProperties exists
public final class GitWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitWorker.class);

    /**
     * Default branches that will be cloned if none are specified in {@link GitlabProperties}.
     */
    private static final Set<String> DEFAULT_BRANCHES = Set.of("master", "main", "develop");

    private final GitlabProperties gitlabProperties;
    private final UsernamePasswordCredentialsProvider passwordCredentialsProvider;

    /**
     * Constructs a new GitWorker instance.
     *
     * @param gitlabProperties The GitLab configuration properties injected by Micronaut.
     */
    public GitWorker(@NotNull GitlabProperties gitlabProperties) {
        this.gitlabProperties = gitlabProperties;
        this.passwordCredentialsProvider = new UsernamePasswordCredentialsProvider(
                gitlabProperties.user(),
                gitlabProperties.password()
        );
    }

    /**
     * Clones a remote Git repository and checks out the specified reference.
     * <p>
     * This method attempts to clone the repository defined in {@link GitlabProperties#remoteUrl()} to
     * the given local path and then checks out the specified reference (branch, commit, or tag).
     * </p>
     *
     * @param reference The reference (branch, commit hash, or tag) to check out.
     * @param path      The local directory where the repository should be cloned.
     */
    public void cloneAndCheckout(@NotNull String reference, @NotNull Path path) {
        try {
            // Initialize the Git clone command with the necessary credentials
            CloneCommand rawGit = Git.cloneRepository()
                    .setCredentialsProvider(this.passwordCredentialsProvider)
                    .setURI(gitlabProperties.remoteUrl())
                    .setDirectory(path.toFile())
                    .setBranchesToClone(getBranches());

            // Execute the clone operation
            Git gitInstance = rawGit.call();

            // Resolve the requested reference (branch, tag, or commit)
            ObjectId resolve = gitInstance.getRepository().resolve(reference);

            if (resolve == null) {
                LOGGER.warn("Unable to fetch data from git with the reference: {}", reference);
                return;
            }

            // Checkout the specified reference
            gitInstance.checkout().setName(resolve.name()).call();
        } catch (Exception exception) {
            LOGGER.warn("Something went wrong during the fetch process", exception);
        }
    }

    public @NotNull List<String> getGitRefs() {
        try {
            return Git.lsRemoteRepository()
                    .setCredentialsProvider(passwordCredentialsProvider)
                    .setHeads(true)
                    .setRemote(gitlabProperties.remoteUrl())
                    .call()
                    .stream().map(Ref::getName).toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    public @NotNull CloneCommand getCloneCommand(@NotNull Path output) {
        return Git.cloneRepository()
                .setCredentialsProvider(passwordCredentialsProvider)
                .setDirectory(output.toFile())
                .setCloneAllBranches(true) ;
    }

    /**
     *
     * @param git
     */
    public void push(@NotNull PushCommand git) {
        try {
            git.call();
        } catch (GitAPIException exception) {
            LOGGER.info("Unable to push content to ", exception);
        }
    }

    public @NotNull String getDeployUrl() {
        return this.gitlabProperties.deployUrl();
    }

    /**
     * Retrieves the list of branches that should be cloned from the remote repository.
     * <p>
     * If no branches are explicitly configured in {@link GitlabProperties#branches()},
     * the default branches ("master", "main", "develop") will be used.
     * </p>
     *
     * @return A collection of branch names to be cloned.
     */
    private @NotNull Collection<String> getBranches() {
        return this.gitlabProperties.branches().isEmpty() ? DEFAULT_BRANCHES : this.gitlabProperties.branches();
    }
}
