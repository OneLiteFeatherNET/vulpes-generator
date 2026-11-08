package net.onelitefeather.vulpes.generator.domain.client;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.release.GitBranchHead;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks on the wire that {@link GithubService#resolveCommit(String)} asks the GitHub branch endpoint for the right
 * branch, also when the branch name contains a slash, and that refusals of GitHub become BASE_UNAVAILABLE. The GitHub
 * API is replaced by an embedded fake on localhost, so no real network is used.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GithubServiceBranchHeadTest implements TestPropertyProvider {

    private static final String HEAD = "0123456789abcdef0123456789abcdef01234567";

    @Override
    public Map<String, String> getProperties() {
        int port = freePort();
        return Map.of(
                "micronaut.server.port", String.valueOf(port),
                "micronaut.http.services.github.url", "http://localhost:" + port);
    }

    @Inject
    GithubService service;

    @Inject
    ReceivedBranch received;

    @BeforeEach
    void reset() {
        received.reset();
    }

    @Test
    void branchWithASlashReachesTheEndpointAsOneBranchName() {
        assertEquals(HEAD, service.resolveCommit("feature/font"),
                "the head of a branch with a slash must be resolved");
        assertEquals("feature/font", received.get(),
                "the endpoint must receive the whole branch name, slash included");
    }

    @Test
    void plainBranchIsResolvedToItsHead() {
        assertEquals(HEAD, service.resolveCommit("develop"), "the head of develop must be returned in full");
        assertEquals("develop", received.get(), "the endpoint must receive the branch name");
    }

    @Test
    void notFoundGivesBaseUnavailable() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service.resolveCommit("missing"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "a 404 of the branch endpoint must be reported as BASE_UNAVAILABLE");
    }

    @Test
    void serverErrorGivesBaseUnavailable() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service.resolveCommit("broken"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "a 5xx of the branch endpoint must be reported as BASE_UNAVAILABLE");
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("no free local port", e);
        }
    }

    /** Remembers the branch of the last branch-head request the fake GitHub endpoint received. */
    @Singleton
    @Requires(env = Environment.TEST)
    static class ReceivedBranch {

        private final AtomicReference<String> value = new AtomicReference<>();

        void set(String branch) {
            value.set(branch);
        }

        String get() {
            return value.get();
        }

        void reset() {
            value.set(null);
        }
    }

    /** Stands in for the GitHub branch-head endpoint: {@code broken} answers 503, {@code missing} 404. */
    @Controller("/repos")
    @Requires(env = Environment.TEST)
    static class FakeGithubBranchHeadController {

        private final ReceivedBranch received;

        FakeGithubBranchHeadController(ReceivedBranch received) {
            this.received = received;
        }

        @Get("/{owner}/{repo}/branches/{branch}")
        HttpResponse<GitBranchHead> branch(@PathVariable String owner, @PathVariable String repo,
                                           @PathVariable String branch) {
            received.set(branch);
            return switch (branch) {
                case "missing" -> HttpResponse.notFound();
                case "broken" -> HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE);
                default -> HttpResponse.ok(new GitBranchHead(branch, new GitBranchHead.Commit(HEAD)));
            };
        }
    }
}
