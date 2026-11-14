package net.onelitefeather.vulpes.generator.controller.release;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import net.onelitefeather.vulpes.generator.release.ReleaseDiagnostic;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the HTTP status of every refused release reason, and the log line of every refusal.
 *
 * @author TheMeinerLP
 */
class ReleaseRejectedExceptionHandlerTest {

    private static final UUID PROJECT_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-8c0a7b6c3d11");

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
    void invalidContentAnswers422() {
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ReleaseRejectedExceptionHandler.statusOf(Reason.INVALID_CONTENT),
                "content with problems must answer 422");
    }

    @Test
    void invalidContentAnswerListsEveryProblemAsADiagnostic() {
        ReleaseDiagnostic problem = new ReleaseDiagnostic(null, 0, "sound eldoria:vhs_static: refers to eldoria:vhs_static_1");
        ReleaseRejectedException refusal = new ReleaseRejectedException(Reason.INVALID_CONTENT, "1 problem",
                List.of(problem), false);

        HttpResponse<ReleaseErrorResponse> response = new ReleaseRejectedExceptionHandler()
                .handle((HttpRequest<?>) null, refusal);

        assertEquals(List.of(problem), response.body().diagnostics(), "every problem must be listed as a diagnostic");
    }

    @Test
    void refusalLogsOneWarnLineWithProjectReasonAndTheFirstTenDiagnostics() {
        ReleaseRejectedException refusal = new ReleaseRejectedException(Reason.INVALID_CONTENT, "12 problems",
                diagnostics(12), false).forProject(PROJECT_ID);
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            new ReleaseRejectedExceptionHandler().handle(HttpRequest.GET("/releases"), refusal);
        } finally {
            detachAppender(appender);
        }

        assertEquals(1, appender.list.size(), "one refusal must log exactly one line: " + appender.list);
        ILoggingEvent event = appender.list.get(0);
        String line = event.getFormattedMessage();
        assertEquals(Level.WARN, event.getLevel(), "a refusal must be logged at WARN");
        assertTrue(line.contains(PROJECT_ID.toString()), "the line must name the project: " + line);
        assertTrue(line.contains("INVALID_CONTENT"), "the line must name the reason: " + line);
        assertTrue(line.contains("problem 9"), "the tenth diagnostic must be logged: " + line);
        assertFalse(line.contains("problem 10"), "no diagnostic after the tenth may be logged: " + line);
    }

    @Test
    void refusalTakesTheProjectFromThePathWhenNoProjectIsRecorded() {
        ReleaseRejectedException refusal = new ReleaseRejectedException(Reason.UNKNOWN_PROJECT, "unknown");
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            new ReleaseRejectedExceptionHandler().handle(HttpRequest.GET("/projects/" + PROJECT_ID + "/local-build"), refusal);
        } finally {
            detachAppender(appender);
        }

        assertTrue(appender.list.get(0).getFormattedMessage().contains(PROJECT_ID.toString()),
                "the project of the request path must be logged: " + appender.list);
    }

    @Test
    void refusalLogLineDoesNotContainTheRejectionMessage() {
        ReleaseRejectedException refusal = new ReleaseRejectedException(Reason.SOURCE_UNAVAILABLE,
                "backend: answered with secret-token-value").forProject(PROJECT_ID);
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            new ReleaseRejectedExceptionHandler().handle(HttpRequest.GET("/releases"), refusal);
        } finally {
            detachAppender(appender);
        }

        assertFalse(appender.list.get(0).getFormattedMessage().contains("secret-token-value"),
                "the log must never carry the message of a refusal, which may hold credentials: " + appender.list);
    }

    private static List<ReleaseDiagnostic> diagnostics(int count) {
        List<ReleaseDiagnostic> diagnostics = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            diagnostics.add(new ReleaseDiagnostic(null, 0, "problem " + i));
        }
        return diagnostics;
    }

    private static ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(ReleaseRejectedExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(ReleaseRejectedExceptionHandler.class)).detachAppender(appender);
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
