package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.client.GithubService;

import java.util.List;

/**
 * Chooses the {@code vulpes-base} branch that a local build or a release is compiled against.
 * An omitted branch means {@link #DEFAULT_BRANCH}; a branch that the repository does not list is refused.
 * When the branch list cannot be read, the branch is not assumed to exist.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BaseBranchResolver {

    /** The branch used when the caller names none. */
    public static final String DEFAULT_BRANCH = "develop";

    private final GithubService github;

    /**
     * Creates the resolver.
     *
     * @param github the source of the branch list of {@code vulpes-base}
     */
    public BaseBranchResolver(GithubService github) {
        this.github = github;
    }

    /**
     * Returns the branch to build against.
     *
     * @param requested the branch the caller named, {@code null} or blank when omitted
     * @return the requested branch, or {@link #DEFAULT_BRANCH} when none was named
     * @throws ReleaseRejectedException with {@code UNKNOWN_BASE_BRANCH} when the branch does not exist and with
     *                                  {@code BASE_UNAVAILABLE} when the branch list cannot be read
     */
    public String resolve(String requested) {
        String branch = requested == null || requested.isBlank() ? DEFAULT_BRANCH : requested;
        List<String> branches = github.getBranches();
        if (!branches.contains(branch)) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.UNKNOWN_BASE_BRANCH,
                    "base branch " + branch + " does not exist in the vulpes-base repository");
        }
        return branch;
    }
}
