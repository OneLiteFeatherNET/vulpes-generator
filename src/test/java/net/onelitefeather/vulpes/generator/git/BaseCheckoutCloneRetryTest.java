package net.onelitefeather.vulpes.generator.git;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the retry and the logging of a failed clone and of a failed ls-remote. Both are replaced by scripted attempts,
 * so no network and no Git transport is used; the real transport is covered by {@link BaseCheckoutTest} and
 * {@link BaseCheckoutHeadTest}.
 *
 * @author TheMeinerLP
 */
class BaseCheckoutCloneRetryTest {

    private static final String COMMIT = "0123456789abcdef0123456789abcdef01234567";
    private static final String CLONE_URL = "https://example.invalid/OneLiteFeatherNET/vulpes-base.git";
    private static final String PARTIAL = "partial-clone.txt";

    @TempDir
    Path tempDir;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(BaseCheckout.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void failedCloneIsLoggedWithBranchUrlAndTheCauseChain() {
        ScriptedCheckout checkout = new ScriptedCheckout(tempDir, 1, false);

        checkout.withCheckout("develop", resolved -> resolved.commit());

        List<ILoggingEvent> warnings = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN).toList();
        assertEquals(1, warnings.size(), "the failed attempt must be logged as one WARN");
        String message = warnings.getFirst().getFormattedMessage();
        assertTrue(message.contains("develop") && message.contains(CLONE_URL),
                "the warning must name the branch and the clone URL, was: " + message);
        assertTrue(message.contains("cannot connect") && message.contains("connect timed out"),
                "the warning must carry the message and the cause of the failure, was: " + message);
    }

    @Test
    void failedCloneIsRetriedOnceAndThenSucceeds() {
        ScriptedCheckout checkout = new ScriptedCheckout(tempDir, 1, false);

        String commit = checkout.withCheckout("develop", resolved -> resolved.commit());

        assertEquals(COMMIT, commit, "the second attempt must deliver the head of the branch");
        assertEquals(2, checkout.attempts(), "one failed clone must be retried exactly once");
    }

    @Test
    void twoFailedClonesGiveBaseUnavailable() {
        ScriptedCheckout checkout = new ScriptedCheckout(tempDir, 2, false);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> checkout.withCheckout("develop", resolved -> resolved.commit()));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "two failed clones must be reported as BASE_UNAVAILABLE");
        assertEquals(2, checkout.attempts(), "the clone must be attempted twice and no more");
    }

    @Test
    void retryStartsInAFreshDirectoryWithoutThePartialClone() {
        ScriptedCheckout checkout = new ScriptedCheckout(tempDir, 1, true);

        checkout.withCheckout("develop", resolved -> resolved.commit());

        assertFalse(checkout.partialCloneSeenByRetry(), "the retry must not find the partial clone of the first attempt");
    }

    @Test
    void failedLsRemoteIsRetriedOnceAndThenReturnsTheHead() throws GitAPIException {
        ScriptedRemote remote = new ScriptedRemote(tempDir, 1);

        assertEquals(COMMIT, remote.headCommit("develop"), "the second ls-remote must deliver the head");
        assertEquals(2, remote.attempts(), "one failed ls-remote must be retried exactly once");
    }

    @Test
    void twoFailedLsRemotesGiveBaseUnavailable() {
        ScriptedRemote remote = new ScriptedRemote(tempDir, 2);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> remote.headCommit("develop"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "two failed ls-remotes must be reported as BASE_UNAVAILABLE");
        assertEquals(2, remote.attempts(), "ls-remote must be attempted twice and no more");
    }

    @Test
    void failedLsRemoteIsLoggedWithBranchUrlAndTheCauseChain() {
        ScriptedRemote remote = new ScriptedRemote(tempDir, 1);

        remote.headCommit("develop");

        List<ILoggingEvent> warnings = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN).toList();
        assertEquals(1, warnings.size(), "the failed ls-remote must be logged as one WARN");
        String message = warnings.getFirst().getFormattedMessage();
        assertTrue(message.contains("develop") && message.contains(CLONE_URL),
                "the warning must name the branch and the URL, was: " + message);
        assertTrue(message.contains("cannot connect") && message.contains("connect timed out"),
                "the warning must carry the message and the cause of the failure, was: " + message);
    }

    /**
     * A checkout whose first {@code failingAttempts} clones fail with a cause chain and write a partial clone first;
     * the next clone succeeds with {@link #COMMIT}.
     */
    private static final class ScriptedCheckout extends BaseCheckout {

        private final int failingAttempts;
        private final boolean leavePartialClone;
        private final List<Boolean> partialSeen = new ArrayList<>();

        ScriptedCheckout(Path workDir, int failingAttempts, boolean leavePartialClone) {
            super(CLONE_URL, Duration.ofMinutes(2), workDir);
            this.failingAttempts = failingAttempts;
            this.leavePartialClone = leavePartialClone;
        }

        @Override
        String cloneShallow(String branch, Path target) throws IOException {
            partialSeen.add(Files.exists(target.resolve(PARTIAL)));
            Files.createDirectories(target);
            if (partialSeen.size() <= failingAttempts) {
                if (leavePartialClone) {
                    Files.writeString(target.resolve(PARTIAL), "half a clone");
                }
                throw new IOException("cannot connect", new SocketTimeoutException("connect timed out"));
            }
            return COMMIT;
        }

        int attempts() {
            return partialSeen.size();
        }

        boolean partialCloneSeenByRetry() {
            return partialSeen.size() > 1 && partialSeen.get(1);
        }
    }

    /** A checkout whose first {@code failingAttempts} ls-remote requests fail; the next one returns {@link #COMMIT}. */
    private static final class ScriptedRemote extends BaseCheckout {

        private final int failingAttempts;
        private int attempts;

        ScriptedRemote(Path workDir, int failingAttempts) {
            super(CLONE_URL, Duration.ofMinutes(2), workDir);
            this.failingAttempts = failingAttempts;
        }

        @Override
        String remoteHead(String branch) throws GitAPIException {
            attempts++;
            if (attempts <= failingAttempts) {
                TransportException failure = new TransportException("cannot connect");
                failure.initCause(new SocketTimeoutException("connect timed out"));
                throw failure;
            }
            return COMMIT;
        }

        int attempts() {
            return attempts;
        }
    }
}
