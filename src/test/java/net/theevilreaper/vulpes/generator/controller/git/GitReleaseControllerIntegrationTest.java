package net.theevilreaper.vulpes.generator.controller.git;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.domain.client.GithubService;
import net.theevilreaper.vulpes.generator.domain.release.GitReleaseDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;

import java.time.OffsetDateTime;

@MicronautTest
class GitReleaseControllerIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    GithubService service;

    @Singleton
    @Replaces(GithubService.class)
    GithubService mockGithubService() {
        return Mockito.mock(GithubService.class);
    }

    @Test
    void getBuildInformationEndpoint() {
        GitReleaseDTO dto = new GitReleaseDTO("v1.2.3", null, OffsetDateTime.now());
        when(service.getLatestVersion()).thenReturn(dto);

        HttpRequest<?> request = HttpRequest.GET("/build/data");
        HttpResponse<GitReleaseDTO> response = client.toBlocking().exchange(request, GitReleaseDTO.class);

        assertEquals(200, response.getStatus().getCode());
        assertEquals(dto, response.body());
    }
}
