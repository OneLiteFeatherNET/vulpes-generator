package net.onelitefeather.vulpes.generator.release;

import com.sun.net.httpserver.HttpServer;
import io.micronaut.http.client.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the snapshot source against a loopback backend that the test starts on an ephemeral port.
 *
 * @author TheMeinerLP
 */
class BackendReleaseSnapshotSourceTest {

    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final UUID BROKEN_PROJECT = UUID.fromString("9d8f2a10-2b3c-4d5e-8f70-123456789abc");
    private static final byte[] SNAPSHOT = withBinaryTail(ReleaseTestData.snapshot(PROJECT_ID));

    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestedPath = new AtomicReference<>();
    private HttpServer server;
    private HttpClient client;

    @BeforeEach
    void startBackend() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/fake-backend/project/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String path = exchange.getRequestURI().getPath();
            requestedPath.set(path);
            if (path.contains(UNKNOWN_PROJECT.toString())) {
                exchange.sendResponseHeaders(404, -1);
            } else if (path.contains(BROKEN_PROJECT.toString())) {
                exchange.sendResponseHeaders(500, -1);
            } else {
                exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, SNAPSHOT.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(SNAPSHOT);
                }
            }
            exchange.close();
        });
        server.start();
        client = HttpClient.create(new URL(baseUrl()));
    }

    @AfterEach
    void stopBackend() {
        client.close();
        server.stop(0);
    }

    @Test
    void fetchReturnsTheSnapshotBytesUnchanged() {
        byte[] fetched = source("").fetch(PROJECT_ID);

        assertArrayEquals(SNAPSHOT, fetched, "the snapshot must reach the caller byte for byte");
    }

    @Test
    void fetchRequestsTheSnapshotPathOfTheProject() {
        source("").fetch(PROJECT_ID);

        assertEquals("/fake-backend/project/" + PROJECT_ID + "/snapshot", requestedPath.get(),
                "the snapshot of the requested project must be fetched from its snapshot path");
    }

    @Test
    void unknownProjectIsRejectedAsUnknownProject() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> source("").fetch(UNKNOWN_PROJECT));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_PROJECT, e.getReason(),
                "a 404 from the backend must be reported as UNKNOWN_PROJECT");
    }

    @Test
    void backendErrorIsRejectedAsSourceUnavailable() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> source("").fetch(BROKEN_PROJECT));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "a 500 from the backend must be reported as SOURCE_UNAVAILABLE");
    }

    @Test
    void unreachableBackendIsRejectedAsSourceUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            closedPort = probe.getLocalPort();
        }
        HttpClient unreachable = HttpClient.create(new URL("http://localhost:" + closedPort));
        try {
            BackendReleaseSnapshotSource source =
                    new BackendReleaseSnapshotSource(unreachable, "http://localhost:" + closedPort, "");

            ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

            assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                    "a refused connection must be reported as SOURCE_UNAVAILABLE");
        } finally {
            unreachable.close();
        }
    }

    @Test
    void blankTokenSendsNoAuthorizationHeader() {
        source("   ").fetch(PROJECT_ID);

        assertNull(authorization.get(), "no Authorization header must be sent without a configured token");
    }

    @Test
    void configuredTokenSendsBearerHeader() {
        source("secret-token").fetch(PROJECT_ID);

        assertEquals("Bearer secret-token", authorization.get(), "the configured token must be sent as bearer token");
    }

    private BackendReleaseSnapshotSource source(String token) {
        return new BackendReleaseSnapshotSource(client, baseUrl(), token);
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/fake-backend";
    }

    /** Appends a byte that is not valid UTF-8 and a trailing newline, to prove the bytes are not decoded. */
    private static byte[] withBinaryTail(byte[] bytes) {
        byte[] tail = {(byte) 0xFF, '\n'};
        byte[] result = new byte[bytes.length + tail.length];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        System.arraycopy(tail, 0, result, bytes.length, tail.length);
        return result;
    }
}
