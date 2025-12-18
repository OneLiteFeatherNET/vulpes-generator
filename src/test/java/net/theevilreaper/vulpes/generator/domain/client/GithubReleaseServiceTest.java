package net.theevilreaper.vulpes.generator.domain.client;

import io.micronaut.http.client.exceptions.HttpClientResponseException;
import net.theevilreaper.vulpes.generator.domain.configuration.GithubConfiguration;
import net.theevilreaper.vulpes.generator.domain.release.GitRelease;
import net.theevilreaper.vulpes.generator.domain.release.GitReleaseDTO;
import net.theevilreaper.vulpes.generator.domain.release.GitTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubReleaseServiceTest {

    @Mock
    GithubBuildClient client;

    @Mock
    GithubConfiguration config;

    GithubReleaseService service;

    @BeforeEach
    void setUp() {
        when(config.owner()).thenReturn("owner");
        when(config.repo()).thenReturn("repo");

        service = new GithubReleaseService(client, config);
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
}
