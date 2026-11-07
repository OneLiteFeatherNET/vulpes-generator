package net.onelitefeather.vulpes.generator.release;

import net.onelitefeather.vulpes.generator.domain.client.GithubService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the choice of the {@code vulpes-base} branch a release is built against.
 *
 * @author TheMeinerLP
 */
class BaseBranchResolverTest {

    private final GithubService github = mock(GithubService.class);
    private final BaseBranchResolver resolver = new BaseBranchResolver(github);

    @Test
    void omittedBranchResolvesToDevelop() {
        when(github.getBranches()).thenReturn(List.of("main", "develop"));

        assertEquals("develop", resolver.resolve(null), "an omitted baseBranch must resolve to develop");
    }

    @Test
    void listedBranchIsAccepted() {
        when(github.getBranches()).thenReturn(List.of("main", "develop", "feature/font"));

        assertEquals("feature/font", resolver.resolve("feature/font"), "a branch that exists must be accepted");
    }

    @Test
    void unlistedBranchIsRefusedWithUnknownBaseBranch() {
        when(github.getBranches()).thenReturn(List.of("main", "develop"));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> resolver.resolve("does-not-exist"));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_BASE_BRANCH, e.getReason(),
                "a branch that vulpes-base does not have must be reported as UNKNOWN_BASE_BRANCH");
    }

    @Test
    void unreadableBranchListGivesBaseUnavailableAndDoesNotAssumeTheBranch() {
        when(github.getBranches()).thenThrow(new ReleaseRejectedException(
                ReleaseRejectedException.Reason.BASE_UNAVAILABLE, "branches of OneLiteFeatherNET/vulpes-base"));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> resolver.resolve("develop"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "a failing branch list must be reported as BASE_UNAVAILABLE, never as an assumed branch");
    }
}
