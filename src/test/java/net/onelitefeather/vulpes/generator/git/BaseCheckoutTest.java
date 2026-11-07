package net.onelitefeather.vulpes.generator.git;

import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the shallow clone of a base branch. The origin is a local bare repository created with JGit in a temporary
 * directory; no network is used.
 *
 * @author TheMeinerLP
 */
class BaseCheckoutTest {

    @TempDir
    Path tempDir;

    @Test
    void shallowCloneResolvesTheBranchHeadToItsFullSha() throws Exception {
        String head = createOriginWithTwoCommits();
        BaseCheckout checkout = new BaseCheckout(origin().toUri().toString(), Duration.ofMinutes(2), tempDir);

        String commit = checkout.resolveCommit("develop");

        assertEquals(head, commit, "the commit must be the head of the branch at clone time");
        assertEquals(40, commit.length(), "the commit must be the full 40-character SHA");
        assertTrue(commit.matches("[0-9a-f]{40}"), "the commit must be lower-case hexadecimal: " + commit);
    }

    @Test
    void checkoutIsAShallowCloneOfTheBranch() throws Exception {
        createOriginWithTwoCommits();
        BaseCheckout checkout = new BaseCheckout(origin().toUri().toString(), Duration.ofMinutes(2), tempDir);

        boolean shallow = checkout.withCheckout("develop",
                resolved -> Files.exists(resolved.directory().resolve(".git").resolve("shallow")));

        assertTrue(shallow, "the clone must be shallow, so that only the branch head is fetched");
    }

    @Test
    void unreachableCloneUrlGivesBaseUnavailable() {
        Path missing = tempDir.resolve("does-not-exist.git");
        BaseCheckout checkout = new BaseCheckout(missing.toUri().toString(), Duration.ofMinutes(2), tempDir);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> checkout.resolveCommit("develop"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "an unreachable vulpes-base must be reported as BASE_UNAVAILABLE");
    }

    @Test
    void cloneUrlIsDerivedFromTheBaseRepository() {
        BaseConfiguration base = new BaseConfiguration("OneLiteFeatherNET/vulpes-base", "", 3,
                Duration.ofMinutes(2), Duration.ofMinutes(10));
        BaseCheckout checkout = new BaseCheckout(base, tempDir.toString());

        assertEquals("https://github.com/OneLiteFeatherNET/vulpes-base.git", checkout.cloneUrl(),
                "the clone URL must derive from vulpes.base.repository");
    }

    private Path origin() {
        return tempDir.resolve("origin.git");
    }

    /** Creates a non-bare repository with two commits on develop, then a bare copy of it as origin. */
    private String createOriginWithTwoCommits() throws Exception {
        Path work = tempDir.resolve("source");
        AtomicReference<String> head = new AtomicReference<>();
        try (Git git = Git.init().setDirectory(work.toFile()).setInitialBranch("develop").call()) {
            commitFile(git, work, "first.txt", "first");
            commitFile(git, work, "second.txt", "second");
            head.set(git.getRepository().resolve("HEAD").name());
        }
        Git.cloneRepository()
                .setBare(true)
                .setURI(work.toUri().toString())
                .setDirectory(origin().toFile())
                .call()
                .close();
        return head.get();
    }

    private static void commitFile(Git git, Path work, String name, String content) throws Exception {
        Files.writeString(work.resolve(name), content, StandardCharsets.UTF_8);
        git.add().addFilepattern(name).call();
        git.commit().setMessage("add " + name)
                .setAuthor("test", "test@example.org")
                .setCommitter("test", "test@example.org")
                .call();
    }
}
