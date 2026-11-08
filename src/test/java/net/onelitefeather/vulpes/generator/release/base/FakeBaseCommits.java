package net.onelitefeather.vulpes.generator.release.base;

import net.onelitefeather.vulpes.generator.domain.client.GithubService;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * The commit resolver of the base tests. It answers with the commits given by {@link #with(String, String)} and
 * refuses every other branch as BASE_UNAVAILABLE, as GitHub does for a branch it does not have. No network.
 *
 * @author TheMeinerLP
 */
final class FakeBaseCommits extends GithubService {

    private final Map<String, String> heads = new HashMap<>();
    private int calls;

    FakeBaseCommits() {
        super(null, null);
    }

    /**
     * Makes a branch resolve to a commit.
     *
     * @param branch the branch name
     * @param commit the full SHA the branch head reports
     * @return this resolver
     */
    FakeBaseCommits with(String branch, String commit) {
        heads.put(branch, commit);
        return this;
    }

    @Override
    public String resolveCommit(String branch) {
        calls++;
        String commit = heads.get(branch);
        if (commit == null) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                    "branch " + branch + " cannot be resolved");
        }
        return commit;
    }

    /** The number of resolutions made so far. */
    int calls() {
        return calls;
    }
}
