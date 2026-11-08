package net.onelitefeather.vulpes.generator.git;

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
 * Tests the head commit read with ls-remote against a local bare repository with two branches, one of them named
 * {@code feature/font}. No network is used.
 *
 * @author TheMeinerLP
 */
class BaseCheckoutHeadTest {

    @TempDir
    Path tempDir;

    @Test
    void headCommitReturnsTheFullShaOfTheBranchHead() throws Exception {
        Heads heads = createOrigin();
        BaseCheckout checkout = checkout(heads);

        assertEquals(heads.develop(), checkout.headCommit("develop"),
                "the commit must be the head of develop as the remote reports it");
    }

    @Test
    void headCommitWorksForABranchWithASlash() throws Exception {
        Heads heads = createOrigin();
        BaseCheckout checkout = checkout(heads);

        assertEquals(heads.font(), checkout.headCommit("feature/font"),
                "a branch containing a slash must resolve to its own head, not to develop");
    }

    @Test
    void headCommitIsLowerCaseHexadecimalOf40Characters() throws Exception {
        Heads heads = createOrigin();
        String commit = checkout(heads).headCommit("develop");

        assertTrue(commit.matches("[0-9a-f]{40}"), "the commit must be a full lower-case SHA-1: " + commit);
    }

    @Test
    void missingBranchGivesBaseUnavailable() throws Exception {
        Heads heads = createOrigin();
        BaseCheckout checkout = checkout(heads);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> checkout.headCommit("no-such-branch"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "a branch that the remote does not have must be reported as BASE_UNAVAILABLE");
    }

    @Test
    void unreachableRemoteGivesBaseUnavailable() {
        BaseCheckout checkout = new BaseCheckout(tempDir.resolve("does-not-exist.git").toUri().toString(),
                Duration.ofMinutes(2), tempDir);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> checkout.headCommit("develop"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "an unreachable remote must be reported as BASE_UNAVAILABLE");
    }

    private BaseCheckout checkout(Heads heads) {
        return new BaseCheckout(heads.origin().toUri().toString(), Duration.ofMinutes(2), tempDir);
    }

    /** The head commits of the two branches of the local origin. */
    private record Heads(Path origin, String develop, String font) {
    }

    /** Creates a source repository with develop and feature/font, then a bare copy of it as origin. */
    private Heads createOrigin() throws Exception {
        Path work = tempDir.resolve("source");
        AtomicReference<String> develop = new AtomicReference<>();
        AtomicReference<String> font = new AtomicReference<>();
        try (Git git = Git.init().setDirectory(work.toFile()).setInitialBranch("develop").call()) {
            commitFile(git, work, "first.txt", "first");
            develop.set(git.getRepository().resolve("HEAD").name());
            git.checkout().setName("feature/font").setCreateBranch(true).call();
            commitFile(git, work, "font.txt", "font");
            font.set(git.getRepository().resolve("HEAD").name());
        }
        Path origin = tempDir.resolve("origin.git");
        Git.cloneRepository()
                .setBare(true)
                .setURI(work.toUri().toString())
                .setDirectory(origin.toFile())
                .call()
                .close();
        return new Heads(origin, develop.get(), font.get());
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
