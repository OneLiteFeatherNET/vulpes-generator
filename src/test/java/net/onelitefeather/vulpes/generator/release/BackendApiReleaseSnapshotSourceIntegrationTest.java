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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.id;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.json;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assembles snapshots through the embedded fake backend: the single content read, the golden bytes of the sample
 * project, bearer tokens and the errors of the content export.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendApiReleaseSnapshotSourceIntegrationTest implements TestPropertyProvider {

    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String EXPORT_PATH = "project/" + PROJECT_ID + "/content";

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
    void sampleProjectExportIsAssembledIntoAValidFormatOneSnapshot() {
        registerSampleExport();

        byte[] snapshot = source.fetch(SampleProject.ID);

        assertTrue(SnapshotSchema.violations(snapshot).isEmpty(),
                "the assembled snapshot must validate against the schema, violations: " + SnapshotSchema.violations(snapshot));
    }

    @Test
    void sampleProjectExportEqualsTheGoldenBytes() throws IOException {
        registerSampleExport();

        byte[] snapshot = source.fetch(SampleProject.ID);

        assertArrayEquals(golden("cygnus-snapshot.json"), snapshot,
                "the snapshot of the sample project must stay byte-identical to the golden snapshot");
    }

    @Test
    void releaseReadsTheContentExportExactlyOnce() {
        source.fetch(PROJECT_ID);

        assertEquals(List.of(EXPORT_PATH), backend.requestedPaths(),
                "a snapshot must be assembled from one request to the content export and nothing else");
    }

    @Test
    void exportWithManyItemsIsMappedWithoutPaging() {
        ProjectFixture fixture = ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
        for (int i = 1; i <= 150; i++) {
            fixture.items().add(obj("id", id(i), "key", "item_" + i));
            fixture.components(id(i)).add(obj("id", id(1000 + i), "componentKey", "stelaris:material",
                    "value", "minecraft:book"));
        }
        backend.registerProject(fixture.build());

        JsonNode items = json(new String(source.fetch(PROJECT_ID), StandardCharsets.UTF_8)).get("items");

        assertEquals(150, items.size(), "all 150 items of the export must be in the snapshot");
        assertEquals(1, backend.requestCount(EXPORT_PATH), "the export must be read once, without paging");
    }

    @Test
    void unknownProjectIsRejectedAsUnknownProject() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(UNKNOWN_PROJECT));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_PROJECT, e.getReason(),
                "the export answering 404 must be reported as UNKNOWN_PROJECT");
    }

    @Test
    void exportAnsweringServerErrorIsSourceUnavailableAfterOneRequest() {
        backend.putReply(EXPORT_PATH, new FakeBackend.FakeReply(500, ""));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "an export answering 500 must be reported as SOURCE_UNAVAILABLE");
        assertEquals(1, backend.requestCount(EXPORT_PATH), "the export must not be retried");
    }

    @Test
    void unavailableBackendIsRejectedAsSourceUnavailable() {
        backend.setDown(true);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "a backend answering 503 must be reported as SOURCE_UNAVAILABLE");
    }

    @Test
    void backendThatCannotBeReachedIsRejectedAsSourceUnavailable() {
        BackendApiReleaseSnapshotSource unreachable =
                new BackendApiReleaseSnapshotSource(client, "http://localhost:1", "", List.of("minecraft", "olf"));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> unreachable.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "a backend that refuses the connection must be reported as SOURCE_UNAVAILABLE");
    }

    @Test
    void exportWithoutBodyIsSourceUnavailable() {
        backend.putReply(EXPORT_PATH, new FakeBackend.FakeReply(200, ""));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "an export without a body must be reported as SOURCE_UNAVAILABLE");
    }

    @Test
    void exportWithInvalidJsonIsSourceUnavailable() {
        backend.putReply(EXPORT_PATH, new FakeBackend.FakeReply(200, "{\"project\":"));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "an export with invalid JSON must be reported as SOURCE_UNAVAILABLE");
    }

    @Test
    void exportThatIsNotAnObjectIsSourceUnavailable() {
        backend.putReply(EXPORT_PATH, new FakeBackend.FakeReply(200, "[]"));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, e.getReason(),
                "an export that is not a JSON object must be reported as SOURCE_UNAVAILABLE");
    }

    @Test
    void advancementInExportIsUnsupportedContentNamingIt() {
        ProjectFixture fixture = ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
        fixture.advancements().add(obj("id", id(10), "key", "first_page"));
        backend.registerProject(fixture.build());

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> source.fetch(PROJECT_ID));

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_CONTENT, e.getReason(),
                "an advancement must still be refused as UNSUPPORTED_CONTENT");
        assertTrue(e.getMessage().contains("first_page"), "the message must name the advancement: " + e.getMessage());
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

    /** Registers the project object and the committed content export of the sample project. */
    private void registerSampleExport() {
        JsonNode export = SampleProject.export();
        backend.putObject("project/" + SampleProject.ID, export.get("project"));
        backend.putExport(SampleProject.ID, export);
    }

    static byte[] golden(String name) throws IOException {
        try (InputStream in = BackendApiReleaseSnapshotSourceIntegrationTest.class
                .getResourceAsStream("/release/golden/" + name)) {
            if (in == null) {
                throw new IllegalStateException("golden " + name + " is missing from the test resources");
            }
            return in.readAllBytes();
        }
    }
}
