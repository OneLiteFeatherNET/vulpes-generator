package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.http.HttpStatus;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the HTTP status of every refused release reason.
 *
 * @author TheMeinerLP
 */
class ReleaseRejectedExceptionHandlerTest {

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
    void corruptPublishedJarAnswers502() {
        assertEquals(HttpStatus.BAD_GATEWAY, ReleaseRejectedExceptionHandler.statusOf(Reason.PUBLISHED_JAR_CORRUPT),
                "a corrupt published JAR must answer 502");
    }
}
