package net.onelitefeather.vulpes.generator.release;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 * The configuration of the release integration tests. Every test class asks for a free local port, so that
 * the embedded servers of different application contexts never share a port. The fake backend and the fake
 * Maven repository are controllers of that embedded server, and the URLs point at it.
 *
 * @author TheMeinerLP
 */
final class ReleaseTestProperties {

    private ReleaseTestProperties() {
    }

    /**
     * Returns the properties for one application context, on a port that was free when this method ran.
     *
     * @return the test properties
     */
    static Map<String, String> onFreePort() {
        int port = freePort();
        String base = "http://localhost:" + port;
        return Map.of(
                "micronaut.server.port", String.valueOf(port),
                "vulpes.backend.url", base + "/fake-backend",
                "vulpes.backend.token", "test-token",
                "vulpes.content.maven.username", "test-user",
                "vulpes.content.maven.password", "test-secret",
                "vulpes.content.maven.releases-url", base + "/fake-maven/repo/onelitefeather-releases",
                "vulpes.content.maven.snapshots-url", base + "/fake-maven/repo/onelitefeather-snapshots");
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("no free local port for the release tests", e);
        }
    }
}
