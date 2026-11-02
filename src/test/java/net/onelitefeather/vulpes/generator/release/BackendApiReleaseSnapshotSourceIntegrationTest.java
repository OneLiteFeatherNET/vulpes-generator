package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.id;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.json;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assembles snapshots through the embedded fake backend: paging, the stable read, bearer tokens and the errors of the
 * backend. The backend answers the endpoints of backend 3.3.0 in the shapes of the captured samples.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendApiReleaseSnapshotSourceIntegrationTest implements TestPropertyProvider {

    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String PROJECT_PATH = "project/" + PROJECT_ID;

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort();
    }

    @Inject
    BackendApiReleaseSnapshotSource source;

    @Inject
    FakeBackend backend;

    @Inject
    @Client("/")
    HttpClient client;

    @Value("${vulpes.backend.url}")
    String backendUrl;

    @BeforeEach
    void resetBackend() {
        backend.reset();
    }

    @Test
    void sampleProjectIsAssembledIntoAValidFormatOneSnapshot() {
        backend.registerProject(SampleProject.load().build());

        byte[] snapshot = source.fetch(SampleProject.ID);

        assertTrue(SnapshotSchema.violations(snapshot).isEmpty(),
                "the assembled snapshot must validate against the schema, violations: " + SnapshotSchema.violations(snapshot));
    }

    @Test
    void everyPageOfAListIsCollected() {
        ProjectFixture fixture = ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
        for (int i = 1; i <= 150; i++) {
            fixture.items().add(obj("id", id(i), "key", "item_" + i));
            fixture.components(id(i)).add(obj("id", id(1000 + i), "componentKey", "stelaris:material",
                    "value", "minecraft:book"));
        }
        backend.registerProject(fixture.build());

        JsonNode items = json(new String(source.fetch(PROJECT_ID), StandardCharsets.UTF_8)).get("items");

        assertEquals(150, items.size(), "all 150 items of two pages must be in the snapshot");
    }

    @Test
    void bareArrayIsTakenAsTheWholeList() {
        backend.putBareList(PROJECT_PATH + "/attribute", List.of(
                obj("id", id(2), "uiName", "Freeze", "key", "freeze", "defaultValue", -1.0,
                        "minimumValue", -1.0, "maximumValue", 1.0, "synced", false)));

        JsonNode attributes = json(new String(source.fetch(PROJECT_ID), StandardCharsets.UTF_8)).get("attributes");

        assertEquals(1, attributes.size(), "a bare JSON array must be taken as the whole list");
    }

    @Test
    void unchangedProjectIsReadTwiceAndUsed() {
        source.fetch(PROJECT_ID);

        assertEquals(2, backend.requestCount(PROJECT_PATH),
                "an unchanged project must be read exactly twice, once per run of the stable read");
    }

    @Test
    void projectThatChangesOnceIsReadAgainUntilItSettles() {
        AtomicInteger reads = new AtomicInteger();
        backend.putObject(PROJECT_PATH, () -> {
            int read = reads.incrementAndGet();
            return FakeBackend.project(PROJECT_ID, "eldoria", "Eldoria " + (read <= 3 ? read : "stable"));
        });

        byte[] snapshot = source.fetch(PROJECT_ID);

        assertEquals("Eldoria stable", json(new String(snapshot, StandardCharsets.UTF_8)).get("pack")
                .get("description").get("text").asText(), "the settled state of the project must be released");
        assertEquals(6, backend.requestCount(PROJECT_PATH),
                "a project that changes twice must be read in three attempts of two reads each");
    }

    @Test
    void projectThatKeepsChangingIsRefusedWithContentChangedAfterThreeAttempts() {
        AtomicInteger reads = new AtomicInteger();
        backend.putObject(PROJECT_PATH, () -> FakeBackend.project(PROJECT_ID, "eldoria", "Eldoria " + reads.incrementAndGet()));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.CONTENT_CHANGED, e.getReason(),
                "a project that never settles must be refused as CONTENT_CHANGED");
        assertEquals(6, backend.requestCount(PROJECT_PATH),
                "exactly three attempts of two reads each must be made before the release is refused");
    }

    @Test
    void unknownProjectIsRejectedAsUnknownProject() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(UNKNOWN_PROJECT));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_PROJECT, e.getReason(),
                "the backend answering 404 for the project must be reported as UNKNOWN_PROJECT");
    }

    @Test
    void unavailableBackendIsRejectedAsSourceUnavailable() {
        backend.setDown(true);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "a backend answering 503 must be reported as SOURCE_UNAVAILABLE");
    }

    @Test
    void configuredTokenIsSentWithEveryBackendCall() {
        source.fetch(PROJECT_ID);

        assertFalse(backend.authorizations().isEmpty(), "the backend must have been called");
        for (String authorization : backend.authorizations()) {
            assertEquals("Bearer test-token", authorization,
                    "every backend call must carry the configured token as bearer token");
        }
    }

    @Test
    void blankTokenSendsNoAuthorizationHeader() {
        BackendApiReleaseSnapshotSource withoutToken =
                new BackendApiReleaseSnapshotSource(client, backendUrl, "  ", List.of("minecraft", "olf"));

        withoutToken.fetch(PROJECT_ID);

        assertFalse(backend.authorizations().isEmpty(), "the backend must have been called");
        for (String authorization : backend.authorizations()) {
            assertNull(authorization, "no Authorization header must be sent without a configured token");
        }
    }
}
