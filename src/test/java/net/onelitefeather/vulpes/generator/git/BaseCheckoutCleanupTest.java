package net.onelitefeather.vulpes.generator.git;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the clone directory of a base checkout is deleted after success and after failure.
 *
 * @author TheMeinerLP
 */
class BaseCheckoutCleanupTest {

    @TempDir
    Path tempDir;

    @Test
    void cloneDirectoryIsDeletedAfterASuccessfulRequest() throws Exception {
        BaseCheckout checkout = checkoutWithOrigin();
        AtomicReference<Path> clone = new AtomicReference<>();

        checkout.withCheckout("develop", resolved -> {
            clone.set(resolved.directory());
            return resolved.commit();
        });

        assertTrue(clone.get() != null, "the action must have received the clone directory");
        assertFalse(Files.exists(clone.get()), "the clone directory must not exist after a successful request");
    }

    @Test
    void cloneDirectoryIsDeletedAfterAFailedRequest() throws Exception {
        BaseCheckout checkout = checkoutWithOrigin();
        AtomicReference<Path> clone = new AtomicReference<>();

        assertThrows(IllegalStateException.class, () -> checkout.withCheckout("develop", resolved -> {
            clone.set(resolved.directory());
            throw new IllegalStateException("generator failed");
        }));

        assertTrue(clone.get() != null, "the action must have received the clone directory");
        assertFalse(Files.exists(clone.get()), "the clone directory must not exist after a failed request");
    }

    private BaseCheckout checkoutWithOrigin() throws Exception {
        Path work = tempDir.resolve("source");
        try (Git git = Git.init().setDirectory(work.toFile()).setInitialBranch("develop").call()) {
            Files.writeString(work.resolve("file.txt"), "content", StandardCharsets.UTF_8);
            git.add().addFilepattern("file.txt").call();
            git.commit().setMessage("add file")
                    .setAuthor("test", "test@example.org")
                    .setCommitter("test", "test@example.org")
                    .call();
        }
        Path origin = tempDir.resolve("origin.git");
        Git.cloneRepository().setBare(true).setURI(work.toUri().toString()).setDirectory(origin.toFile()).call().close();
        return new BaseCheckout(origin.toUri().toString(), Duration.ofMinutes(2), tempDir);
    }
}
