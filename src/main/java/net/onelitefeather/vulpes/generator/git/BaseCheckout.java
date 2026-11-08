package net.onelitefeather.vulpes.generator.git;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import net.onelitefeather.vulpes.generator.release.BuildWorkspace;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;

/**
 * Makes a shallow clone of one branch of the {@code vulpes-base} repository and reads the commit the branch points to.
 * A failed clone is logged with its cause and retried once in a fresh directory. The clone lives in a fresh directory
 * of the work directory, which is deleted when the action has finished, whether it succeeded or failed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BaseCheckout {

    private static final Logger LOG = LoggerFactory.getLogger(BaseCheckout.class);
    private static final int CLONE_ATTEMPTS = 2;

    private final String cloneUrl;
    private final Duration cloneTimeout;
    private final Path workDir;

    /**
     * Creates the checkout for the configured repository.
     *
     * @param base    the {@code vulpes.base.*} configuration; the clone URL derives from its repository
     * @param workDir the base directory for request directories; blank means the JVM temp directory
     */
    @Inject
    public BaseCheckout(BaseConfiguration base, @Value("${vulpes.release.work-dir:}") String workDir) {
        this(cloneUrlOf(base.repository()), base.cloneTimeout(),
                workDir.isBlank() ? Path.of(System.getProperty("java.io.tmpdir")) : Path.of(workDir));
    }

    /**
     * Creates the checkout for an explicit clone URL. The tests use this constructor with a local repository.
     *
     * @param cloneUrl     the URL the branches are cloned from
     * @param cloneTimeout the time limit of the clone
     * @param workDir      the base directory for clone directories
     */
    public BaseCheckout(String cloneUrl, Duration cloneTimeout, Path workDir) {
        this.cloneUrl = cloneUrl;
        this.cloneTimeout = cloneTimeout;
        this.workDir = workDir;
    }

    private static String cloneUrlOf(String repository) {
        return "https://github.com/" + repository + ".git";
    }

    /**
     * Returns the URL the branches are cloned from.
     *
     * @return the clone URL
     */
    String cloneUrl() {
        return cloneUrl;
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
            String commit = cloneWithRetry(branch, workspace.dir());
            return action.apply(new Checkout(workspace.dir(), commit));
        }
    }

    /**
     * Returns the full SHA of the head of a branch, read with {@code git ls-remote} from the clone URL. No clone is
     * made and no API is called, so a warm build costs one Git request. A failed request is logged with its cause and
     * retried once.
     *
     * @param branch the branch to resolve, such as {@code develop} or {@code feature/font}
     * @return the full 40-character lower-case SHA-1 of the branch head
     * @throws ReleaseRejectedException with {@code BASE_UNAVAILABLE} when the branch does not exist or the remote
     *                                  cannot be read
     */
    public String headCommit(String branch) {
        for (int attempt = 1; attempt <= CLONE_ATTEMPTS; attempt++) {
            try {
                String commit = remoteHead(branch);
                if (commit == null) {
                    throw unavailable(branch);
                }
                return commit;
            } catch (GitAPIException e) {
                LOG.warn("ls-remote of branch {} from {} failed (attempt {} of {}): {}", branch, cloneUrl, attempt,
                        CLONE_ATTEMPTS, causeChain(e));
            }
        }
        throw unavailable(branch);
    }

    /**
     * Asks the remote for the head of one branch.
     *
     * @return the SHA of the head, or {@code null} when the remote has no such branch
     */
    String remoteHead(String branch) throws GitAPIException {
        Collection<Ref> heads = Git.lsRemoteRepository()
                .setRemote(cloneUrl)
                .setHeads(true)
                .setTimeout((int) Math.max(1, cloneTimeout.toSeconds()))
                .call();
        return heads.stream()
                .filter(ref -> ref.getName().equals(Constants.R_HEADS + branch))
                .findFirst()
                .map(ref -> ref.getObjectId().name())
                .orElse(null);
    }

    /**
     * Clones the branch, retrying a failed clone once. Every failed attempt is logged with its cause and its partial
     * clone is deleted, so the next attempt starts in an empty directory.
     */
    private String cloneWithRetry(String branch, Path target) {
        for (int attempt = 1; attempt <= CLONE_ATTEMPTS; attempt++) {
            try {
                return cloneShallow(branch, target);
            } catch (GitAPIException | IOException e) {
                LOG.warn("clone of branch {} from {} failed (attempt {} of {}): {}", branch, cloneUrl, attempt,
                        CLONE_ATTEMPTS, causeChain(e));
                BuildWorkspace.deleteTree(target);
            }
        }
        throw unavailable(branch);
    }

    String cloneShallow(String branch, Path target) throws GitAPIException, IOException {
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
        }
    }

    private static String causeChain(Throwable e) {
        StringBuilder chain = new StringBuilder();
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (!chain.isEmpty()) {
                chain.append(" <- ");
            }
            chain.append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
        }
        return chain.toString();
    }

    private static ReleaseRejectedException unavailable(String branch) {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                "branch " + branch + " of the vulpes-base repository cannot be cloned");
    }
}
