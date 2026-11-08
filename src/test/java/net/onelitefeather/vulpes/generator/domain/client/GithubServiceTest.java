package net.onelitefeather.vulpes.generator.domain.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientException;
import net.onelitefeather.vulpes.generator.domain.configuration.GithubConfiguration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.onelitefeather.vulpes.generator.domain.release.GitBranch;
import net.onelitefeather.vulpes.generator.domain.release.GitBranchHead;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    @Mock
    GithubClient client;

    @Mock
    GithubConfiguration config;

    GithubService service;

    private static final String HEAD = "0123456789abcdef0123456789abcdef01234567";

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        when(config.owner()).thenReturn("owner");
        when(config.repo()).thenReturn("repo");

        service = new GithubService(client, config);

        logger = (Logger) LoggerFactory.getLogger(GithubService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void returnsBranchNamesWhenResponseIsSuccessful() {
        // Mocked response
        List<GitBranch> branches = List.of(
                new GitBranch("main", true),
                new GitBranch("develop", false)
        );
        HttpResponse<List<GitBranch>> response = mock(HttpResponse.class);
        when(response.code()).thenReturn(200);
        when(response.body()).thenReturn(branches);

        when(client.branches("owner", "repo", 100, 1, null)).thenReturn(response);

        List<String> result = service.getBranches();
        assertEquals(List.of("main", "develop"), result);
    }

    @Test
    void failsWhenResponseIsNot200() {
        HttpResponse<List<GitBranch>> response = mock(HttpResponse.class);
        when(response.code()).thenReturn(404);

        when(client.branches("owner", "repo", 100, 1, null)).thenReturn(response);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service.getBranches(),
                "a branch list that GitHub does not answer with 200 must not be read as an empty list");

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "a non-200 branch list must be reported as BASE_UNAVAILABLE");
    }

    @Test
    void failedConnectionGivesBaseUnavailable() {
        when(client.branches("owner", "repo", 100, 1, null))
                .thenThrow(new HttpClientException("connection refused"));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service.getBranches());

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "a branch list that cannot be reached must be reported as BASE_UNAVAILABLE");
    }

    @Test
    void configuredPasswordIsSentAsBearerAuthorization() {
        when(config.password()).thenReturn("secret-token");
        HttpResponse<List<GitBranch>> response = mock(HttpResponse.class);
        when(response.code()).thenReturn(200);
        when(response.body()).thenReturn(List.of(new GitBranch("main", true)));
        when(client.branches("owner", "repo", 100, 1, "Bearer secret-token")).thenReturn(response);

        assertEquals(List.of("main"), service.getBranches(),
                "the configured github.password must be sent as the Authorization header of the branch list");
    }

    @Test
    void resolveCommitReturnsTheFullShaOfTheBranchHead() {
        HttpResponse<GitBranchHead> response = mock(HttpResponse.class);
        when(response.code()).thenReturn(200);
        when(response.body()).thenReturn(new GitBranchHead("develop", new GitBranchHead.Commit(HEAD)));
        when(client.branch("owner", "repo", "develop", null)).thenReturn(response);

        assertEquals(HEAD, service.resolveCommit("develop"),
                "the commit must be the full SHA of the branch head as GitHub reports it");
    }

    @Test
    void resolveCommitPassesABranchWithASlashToTheClientUnchanged() {
        HttpResponse<GitBranchHead> response = mock(HttpResponse.class);
        when(response.code()).thenReturn(200);
        when(response.body()).thenReturn(new GitBranchHead("feature/font", new GitBranchHead.Commit(HEAD)));
        when(client.branch("owner", "repo", "feature/font", null)).thenReturn(response);

        assertEquals(HEAD, service.resolveCommit("feature/font"),
                "the client encodes the branch; the service must hand it over as it was named");
    }

    @Test
    void resolveCommitFailsWithBaseUnavailableWhenNotFound() {
        HttpResponse<GitBranchHead> response = mock(HttpResponse.class);
        when(response.code()).thenReturn(404);
        when(client.branch("owner", "repo", "gone", null)).thenReturn(response);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service.resolveCommit("gone"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "a branch that GitHub does not answer with 200 for must be BASE_UNAVAILABLE");
    }

    @Test
    void resolveCommitFailsWithBaseUnavailableWhenGitHubIsUnreachable() {
        when(client.branch("owner", "repo", "develop", null))
                .thenThrow(new HttpClientException("connection refused"));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service.resolveCommit("develop"));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason(),
                "an unreachable GitHub must be reported as BASE_UNAVAILABLE");
    }

    @Test
    void resolveCommitLogsAWarningWithTheCauseOfTheFailure() {
        when(client.branch("owner", "repo", "develop", null))
                .thenThrow(new HttpClientException("connection refused"));

        assertThrows(ReleaseRejectedException.class, () -> service.resolveCommit("develop"));

        assertEquals(1, appender.list.size(), "exactly one warning must be logged for the failed lookup");
        ILoggingEvent event = appender.list.getFirst();
        assertEquals(Level.WARN, event.getLevel(), "the failed lookup must be logged as a WARN");
        assertTrue(event.getFormattedMessage().contains("develop")
                        && event.getFormattedMessage().contains("connection refused"),
                "the warning must name the branch and the cause, was: " + event.getFormattedMessage());
    }
}
