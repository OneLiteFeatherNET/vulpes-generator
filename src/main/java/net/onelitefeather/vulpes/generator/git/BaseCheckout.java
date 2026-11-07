package net.onelitefeather.vulpes.generator.git;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import net.onelitefeather.vulpes.generator.domain.configuration.GithubConfiguration;
import net.onelitefeather.vulpes.generator.release.BuildWorkspace;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Function;

/**
 * Makes a shallow clone of one branch of the {@code vulpes-base} repository and resolves the commit the branch points to.
 * The clone lives in a fresh directory of the work directory, which is deleted when the action has finished, whether it
 * succeeded or failed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BaseCheckout {

    private final String cloneUrl;
    private final Duration cloneTimeout;
    private final Path workDir;

    /**
     * Creates the checkout for the configured repository.
     *
     * @param github  the GitHub configuration, whose clone URL is the URL of {@code vulpes-base}
     * @param base    the {@code vulpes.base.*} configuration with the clone time limit
     * @param workDir the base directory for request directories; blank means the JVM temp directory
     */
    @Inject
    public BaseCheckout(GithubConfiguration github, BaseConfiguration base,
                        @Value("${vulpes.release.work-dir:}") String workDir) {
        this(github.cloneUrl(), base.cloneTimeout(),
                workDir.isBlank() ? Path.of(System.getProperty("java.io.tmpdir")) : Path.of(workDir));
    }

    /**
     * Creates the checkout for an explicit clone URL. The tests use this constructor with a local repository.
     *
     * @param cloneUrl     the URL the branches are cloned from
     * @param cloneTimeout the time limit of the clone
     * @param workDir      the base directory for clone directories, which must exist
     */
    BaseCheckout(String cloneUrl, Duration cloneTimeout, Path workDir) {
        this.cloneUrl = cloneUrl;
        this.cloneTimeout = cloneTimeout;
        this.workDir = workDir;
    }

    /**
     * The shallow clone of a branch, valid while the action of {@link #withCheckout(String, Function)} runs.
     *
     * @param directory the directory of the clone
     * @param commit    the full 40-character lower-case SHA-1 the branch head pointed to at clone time
     */
    public record Checkout(Path directory, String commit) {
    }

    /**
     * Clones the branch, runs the action with the clone and deletes the clone afterwards.
     *
     * @param branch the branch to clone
     * @param action the work that uses the clone
     * @param <T>    the result of the action
     * @return the result of the action
     * @throws ReleaseRejectedException with {@code BASE_UNAVAILABLE} when the branch cannot be cloned
     */
    public <T> T withCheckout(String branch, Function<Checkout, T> action) {
        try (BuildWorkspace workspace = BuildWorkspace.create(workDir)) {
            String commit = cloneShallow(branch, workspace.dir());
            return action.apply(new Checkout(workspace.dir(), commit));
        }
    }

    /**
     * Returns the full SHA of the head of a branch. The clone is deleted before this method returns.
     *
     * @param branch the branch to resolve
     * @return the full 40-character lower-case SHA-1
     * @throws ReleaseRejectedException with {@code BASE_UNAVAILABLE} when the branch cannot be cloned
     */
    public String resolveCommit(String branch) {
        return withCheckout(branch, Checkout::commit);
    }

    private String cloneShallow(String branch, Path target) {
        try (Git git = Git.cloneRepository()
                .setURI(cloneUrl)
                .setDirectory(target.toFile())
                .setBranch(Constants.R_HEADS + branch)
                .setDepth(1)
                .setNoTags()
                .setTimeout((int) Math.max(1, cloneTimeout.toSeconds()))
                .call()) {
            ObjectId head = git.getRepository().resolve(Constants.HEAD);
            if (head == null) {
                throw unavailable(branch);
            }
            return head.name();
        } catch (GitAPIException | IOException e) {
            throw unavailable(branch);
        }
    }

    private static ReleaseRejectedException unavailable(String branch) {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                "branch " + branch + " of the vulpes-base repository cannot be cloned");
    }
}
