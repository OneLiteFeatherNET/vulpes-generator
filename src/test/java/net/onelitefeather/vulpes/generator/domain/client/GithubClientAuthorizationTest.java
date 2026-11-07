package net.onelitefeather.vulpes.generator.domain.client;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.release.GitBranch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks on the wire that {@link GithubClient} sends the Authorization header it is given. The GitHub API is
 * replaced by an embedded fake on localhost, so no real network is used.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GithubClientAuthorizationTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        int port = freePort();
        return Map.of(
                "micronaut.server.port", String.valueOf(port),
                "micronaut.http.services.github.url", "http://localhost:" + port);
    }

    @Inject
    GithubClient client;

    @Inject
    ReceivedAuthorization received;

    @BeforeEach
    void reset() {
        received.reset();
    }

    @Test
    void requestWithoutTokenSendsNoAuthorizationHeader() {
        client.branches("owner", "repo", 100, 1, null);

        assertNull(received.get(), "without a configured token no Authorization header may be sent");
    }

    @Test
    void requestWithTokenSendsItAsAuthorizationHeader() {
        client.branches("owner", "repo", 100, 1, "Bearer secret-token");

        assertEquals("Bearer secret-token", received.get(),
                "the Authorization header must carry the token that the caller passed");
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("no free local port", e);
        }
    }

    /** Remembers the Authorization header of the last branch request the fake GitHub endpoint received. */
    @Singleton
    @Requires(env = Environment.TEST)
    static class ReceivedAuthorization {

        private final AtomicReference<String> value = new AtomicReference<>();

        void set(String authorization) {
            value.set(authorization);
        }

        String get() {
            return value.get();
        }

        void reset() {
            value.set(null);
        }
    }

    /** Stands in for the GitHub branch endpoint. */
    @Controller("/repos")
    @Requires(env = Environment.TEST)
    static class FakeGithubBranchesController {

        private final ReceivedAuthorization received;

        FakeGithubBranchesController(ReceivedAuthorization received) {
            this.received = received;
        }

        @Get("/{owner}/{repo}/branches{?per_page,page}")
        HttpResponse<List<GitBranch>> branches(@PathVariable String owner,
                                               @PathVariable String repo,
                                               @QueryValue(value = "per_page") @Nullable Integer perPage,
                                               @QueryValue @Nullable Integer page,
                                               @Header("Authorization") @Nullable String authorization) {
            received.set(authorization);
            return HttpResponse.ok(List.of(new GitBranch("main", true)));
        }
    }
}
