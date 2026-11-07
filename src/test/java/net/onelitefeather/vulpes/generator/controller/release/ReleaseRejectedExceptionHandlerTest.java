package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import net.onelitefeather.vulpes.generator.release.ReleaseDiagnostic;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the HTTP status of every refused release reason.
 *
 * @author TheMeinerLP
 */
class ReleaseRejectedExceptionHandlerTest {

    @Test
    void versionNotPublishedAnswers404() {
        assertEquals(HttpStatus.NOT_FOUND, ReleaseRejectedExceptionHandler.statusOf(Reason.VERSION_NOT_PUBLISHED),
                "a version that is not published must answer 404");
    }

    @Test
    void corruptPublishedJarAnswers502() {
        assertEquals(HttpStatus.BAD_GATEWAY, ReleaseRejectedExceptionHandler.statusOf(Reason.PUBLISHED_JAR_CORRUPT),
                "a corrupt published JAR must answer 502");
    }

    @Test
    void unknownBaseBranchAnswers422() {
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY,
                ReleaseRejectedExceptionHandler.statusOf(Reason.UNKNOWN_BASE_BRANCH),
                "an unknown base branch must answer 422");
    }

    @Test
    void baseUnavailableAnswers502() {
        assertEquals(HttpStatus.BAD_GATEWAY, ReleaseRejectedExceptionHandler.statusOf(Reason.BASE_UNAVAILABLE),
                "an unreachable vulpes-base must answer 502");
    }

    @Test
    void compilationFailedAnswers422() {
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ReleaseRejectedExceptionHandler.statusOf(Reason.COMPILATION_FAILED),
                "a compile failure must answer 422");
    }

    @Test
    void generationRefusedAnswers422() {
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ReleaseRejectedExceptionHandler.statusOf(Reason.GENERATION_REFUSED),
                "a refused snapshot must answer 422");
    }

    @Test
    void compilationRefusalCarriesItsDiagnosticsAndTruncation() {
        ReleaseDiagnostic diagnostic = new ReleaseDiagnostic("items/Sword.java", 12, "cannot find symbol");
        ReleaseRejectedException refusal = new ReleaseRejectedException(Reason.COMPILATION_FAILED, "1 error",
                List.of(diagnostic), true);

        HttpResponse<ReleaseErrorResponse> response = new ReleaseRejectedExceptionHandler()
                .handle((HttpRequest<?>) null, refusal);

        assertEquals(List.of(diagnostic), response.body().diagnostics(),
                "the body must list the compiler errors of the refusal");
        assertEquals(Boolean.TRUE, response.body().truncated(), "the body must state that the list was cut");
    }

    @Test
    void refusalWithoutDiagnosticsLeavesThemUnset() {
        ReleaseRejectedException refusal = new ReleaseRejectedException(Reason.VERSION_EXISTS, "exists");

        HttpResponse<ReleaseErrorResponse> response = new ReleaseRejectedExceptionHandler()
                .handle((HttpRequest<?>) null, refusal);

        assertNull(response.body().diagnostics(), "diagnostics must be unset when the refusal has none");
        assertNull(response.body().truncated(), "truncated must be unset when the list was not cut");
    }
}
