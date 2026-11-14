package net.onelitefeather.vulpes.generator.release;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GET /projects/{projectId}/local-build} against the fake backend and the fake base.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalBuildControllerIntegrationTest implements TestPropertyProvider {

    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort();
    }

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    FakeBackend backend;

    @Inject
    FakeBaseBranches branches;

    @Inject
    FakeBaseVerification verification;

    @BeforeEach
    void resetEnvironment() {
        backend.reset();
        branches.reset();
        verification.reset();
    }

    @Test
    void localBuildAnswers200WithAZipAndTheAttachmentName() throws IOException {
        Outcome outcome = get("/projects/" + PROJECT_ID + "/local-build");

        assertEquals(200, outcome.status(), "a known project must answer 200");
        assertEquals("application/zip", outcome.contentType(), "the media type must be application/zip");
        assertEquals("attachment; filename=\"eldoria-local.zip\"", outcome.contentDisposition(),
                "the download must be an attachment named after the project");
        assertEquals(List.of("vulpes/eldoria/release.json", "vulpes/eldoria/snapshot.json"), entryNames(outcome.body()),
                "the zip must hold exactly the two metadata entries");
    }

    @Test
    void baseBranchQueryParameterSelectsTheBranch() {
        Outcome outcome = get("/projects/" + PROJECT_ID + "/local-build?baseBranch=feature/font");

        assertEquals(200, outcome.status(), "a known branch must answer 200");
        assertEquals("feature/font", verification.inputs().get(0).branch(), "the requested branch must be verified");
    }

    @Test
    void unknownProjectAnswers422() {
        Outcome outcome = get("/projects/" + UNKNOWN_PROJECT + "/local-build");

        assertEquals(422, outcome.status(), "a project unknown to the backend must answer 422");
    }

    @Test
    void compilationFailureAnswers422WithTheDiagnostics() {
        verification.failWith(new ReleaseRejectedException(ReleaseRejectedException.Reason.COMPILATION_FAILED,
                "does not compile", List.of(new ReleaseDiagnostic("Eldoria.java", 12, "cannot find symbol")), false));

        Outcome outcome = get("/projects/" + PROJECT_ID + "/local-build");

        assertEquals(422, outcome.status(), "a snapshot that does not compile must answer 422");
        assertTrue(outcome.text().contains("Eldoria.java") && outcome.text().contains("diagnostics"),
                "the body must list the diagnostics: " + outcome.text());
    }

    @Test
    void unknownBaseBranchAnswers422() {
        Outcome outcome = get("/projects/" + PROJECT_ID + "/local-build?baseBranch=no-such-branch");

        assertEquals(422, outcome.status(), "an unknown branch must answer 422");
    }

    @Test
    void unavailableBackendAnswers502() {
        backend.setDown(true);

        assertEquals(502, get("/projects/" + PROJECT_ID + "/local-build").status(),
                "an unavailable backend must answer 502");
    }

    @Test
    void validLocalBuildAnswersTheContentProblemHeaderWithZero() {
        Outcome outcome = get("/projects/" + PROJECT_ID + "/local-build");

        assertEquals("0", outcome.contentProblems(), "valid content must answer the problem count 0");
    }

    @Test
    void localBuildWithContentProblemsIsDeliveredWithTheirCount() throws IOException {
        backend.registerProject(projectWithMissingSound());

        Outcome outcome = get("/projects/" + PROJECT_ID + "/local-build");

        assertEquals(200, outcome.status(), "a local build with content problems must still be delivered");
        assertEquals("2", outcome.contentProblems(), "the header must count the two missing sound files");
        assertEquals(List.of("vulpes/eldoria/release.json", "vulpes/eldoria/snapshot.json"), entryNames(outcome.body()),
                "the zip must hold the two metadata entries");
    }

    @Test
    void projectIdThatIsNotAUuidAnswers400() {
        assertEquals(400, get("/projects/Eldoria/local-build").status(), "a project id that is not a UUID must answer 400");
    }

    /** The project of the default fixture with a sound that refers to two files without an asset. */
    private static ProjectData projectWithMissingSound() {
        ProjectFixture fixture = ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
        fixture.sounds().add(obj("id", ProjectFixture.id(5), "key", "vhs_static", "uiName", "vhs_static", "subTitle", null));
        fixture.sources(ProjectFixture.id(5)).add(obj("id", ProjectFixture.id(6), "name", "eldoria:vhs_static_1",
                "volume", 1.0, "pitch", 1.0, "weight", 1, "stream", false, "attenuationDistance", 16,
                "preload", false, "type", "FILE"));
        fixture.sources(ProjectFixture.id(5)).add(obj("id", ProjectFixture.id(7), "name", "eldoria:vhs_static_2",
                "volume", 1.0, "pitch", 1.0, "weight", 1, "stream", false, "attenuationDistance", 16,
                "preload", false, "type", "FILE"));
        return fixture.build();
    }

    private Outcome get(String uri) {
        try {
            HttpResponse<byte[]> response = client.toBlocking().exchange(HttpRequest.GET(uri), byte[].class);
            return new Outcome(response.getStatus().getCode(), response.getContentType().map(MediaType::toString).orElse(""),
                    response.header("Content-Disposition"), response.header("Vulpes-Content-Problems"),
                    response.body(), "");
        } catch (HttpClientResponseException e) {
            return new Outcome(e.getStatus().getCode(), "", null, null, null,
                    e.getResponse().getBody(String.class).orElse(""));
        }
    }

    private static List<String> entryNames(byte[] zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private record Outcome(int status, String contentType, String contentDisposition, String contentProblems,
                           byte[] body, String text) {
    }
}
