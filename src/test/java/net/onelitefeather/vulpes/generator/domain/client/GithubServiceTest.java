package net.onelitefeather.vulpes.generator.domain.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import net.onelitefeather.vulpes.generator.domain.configuration.GithubConfiguration;
import net.onelitefeather.vulpes.generator.domain.release.GitBranch;
import net.onelitefeather.vulpes.generator.domain.release.GitRelease;
import net.onelitefeather.vulpes.generator.domain.release.GitReleaseDTO;
import net.onelitefeather.vulpes.generator.domain.release.GitTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    @Mock
    GithubClient client;

    @Mock
    GithubConfiguration config;

    GithubService service;

    @BeforeEach
    void setUp() {
        when(config.owner()).thenReturn("owner");
        when(config.repo()).thenReturn("repo");

        service = new GithubService(client, config);
    }

    @Test
    void returnsLatestReleaseWhenAvailable() {
        GitRelease release = mock(GitRelease.class);

        when(client.latestRelease("owner", "repo"))
                .thenReturn(release);

        GitReleaseDTO dto = service.getLatestVersion();

        assertEquals(dto, GitReleaseDTO.fromRelease(release));
        verify(client, never()).tags(any(), any());
    }

    @Test
    void fallsBackToTagsWhenReleaseIsNull() {
        when(client.latestRelease("owner", "repo"))
                .thenReturn(null);

        GitTag tag = new GitTag("v1.2.3");
        when(client.tags("owner", "repo"))
                .thenReturn(List.of(tag));

        GitReleaseDTO dto = service.getLatestVersion();
        assertEquals(dto, GitReleaseDTO.fromTag("v1.2.3"));
    }

    @Test
    void fallsBackToTagsWhenReleaseFails() {
        when(client.latestRelease("owner", "repo"))
                .thenThrow(mock(HttpClientResponseException.class));

        GitTag tag = new GitTag("v1.2.3");
        when(client.tags("owner", "repo"))
                .thenReturn(List.of(tag));

        GitReleaseDTO dto = service.getLatestVersion();
        assertEquals(dto, GitReleaseDTO.fromTag("v1.2.3"));
    }

    @Test
    void returnsUnknownWhenNoReleaseAndNoTags() {
        when(client.latestRelease("owner", "repo"))
                .thenThrow(mock(HttpClientResponseException.class));

        when(client.tags("owner", "repo"))
                .thenReturn(List.of());

        GitReleaseDTO dto = service.getLatestVersion();
        assertEquals(dto, GitReleaseDTO.unknown());
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

        when(client.branches("owner", "repo", 100, 1)).thenReturn(response);

        List<String> result = service.getBranches();
        assertEquals(List.of("main", "develop"), result);
    }

    @Test
    void returnsEmptyListWhenResponseIsNot200() {
        HttpResponse<List<GitBranch>> response = mock(HttpResponse.class);
        when(response.code()).thenReturn(404);

        when(client.branches("owner", "repo", 100, 1)).thenReturn(response);

        List<String> result = service.getBranches();

        assertEquals(List.of(), result);
    }
}
