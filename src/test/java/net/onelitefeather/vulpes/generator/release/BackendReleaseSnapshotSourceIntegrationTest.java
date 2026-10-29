package net.onelitefeather.vulpes.generator.release;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The snapshot source wired by Micronaut against the embedded fake backend.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendReleaseSnapshotSourceIntegrationTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort();
    }

    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Inject
    ReleaseSnapshotSource source;

    @Inject
    FakeBackend backend;

    @BeforeEach
    void resetBackend() {
        backend.reset();
    }

    @Test
    void fetchReturnsTheSnapshotOfTheEmbeddedBackend() {
        byte[] snapshot = ReleaseTestData.snapshot(PROJECT_ID);
        backend.register(PROJECT_ID, snapshot);

        assertArrayEquals(snapshot, source.fetch(PROJECT_ID), "the stored snapshot must be returned unchanged");
    }

    @Test
    void snapshotLargerThanTheDefaultClientLimitIsFetchedUnchanged() {
        // Micronaut's default client limit is 10 MB; a real project snapshot may be larger.
        byte[] large = new byte[11 * 1024 * 1024];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i * 31 + (i >>> 8));
        }
        backend.register(PROJECT_ID, large);

        assertArrayEquals(large, source.fetch(PROJECT_ID), "an 11 MB snapshot must be fetched byte for byte");
    }

    @Test
    void configuredTokenIsSentAsBearerToEmbeddedBackend() {
        backend.register(PROJECT_ID, ReleaseTestData.snapshot(PROJECT_ID));

        source.fetch(PROJECT_ID);

        assertEquals("Bearer test-token", backend.lastAuthorization(),
                "the token from vulpes.backend.token must be sent as bearer token");
    }

    @Test
    void unknownProjectIsRejectedAsUnknownProject() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(UNKNOWN_PROJECT));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_PROJECT, e.getReason(),
                "the backend answering 404 must be reported as UNKNOWN_PROJECT");
    }

    @Test
    void unavailableBackendIsRejectedAsSourceUnavailable() {
        backend.register(PROJECT_ID, ReleaseTestData.snapshot(PROJECT_ID));
        backend.setDown(true);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "the backend answering 503 must be reported as SOURCE_UNAVAILABLE");
    }
}
