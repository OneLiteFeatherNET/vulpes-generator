package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The branch resolver of the test contexts. It knows the branches {@code develop} and {@code feature/font} and never
 * asks GitHub. {@link #setDown(boolean)} makes the branch list fail.
 *
 * @author TheMeinerLP
 */
@Singleton
@Replaces(BaseBranchResolver.class)
class FakeBaseBranches extends BaseBranchResolver {

    private final Set<String> branches = new LinkedHashSet<>(List.of("develop", "feature/font"));
    private boolean down;
    private int calls;

    FakeBaseBranches() {
        super(null);
    }

    @Override
    public String resolve(String requested) {
        calls++;
        if (down) {
            throw new ReleaseRejectedException(Reason.BASE_UNAVAILABLE, "the branch list of vulpes-base is unavailable");
        }
        String branch = requested == null || requested.isBlank() ? DEFAULT_BRANCH : requested;
        if (!branches.contains(branch)) {
            throw new ReleaseRejectedException(Reason.UNKNOWN_BASE_BRANCH, "base branch " + branch + " does not exist");
        }
        return branch;
    }

    /** Makes the branch list fail, as an unreachable GitHub does. */
    void setDown(boolean down) {
        this.down = down;
    }

    /** The number of resolutions made since the last reset. */
    int callCount() {
        return calls;
    }

    /** Restores the default branches and the default state. */
    void reset() {
        down = false;
        calls = 0;
    }
}
