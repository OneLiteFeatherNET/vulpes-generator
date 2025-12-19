package net.theevilreaper.vulpes.generator.controller.git;

import io.micronaut.http.HttpResponse;
import net.theevilreaper.vulpes.generator.domain.client.GithubService;
import net.theevilreaper.vulpes.generator.domain.release.GitReleaseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GitReleaseControllerUnitTest {


    private GithubService service;
    private GitReleaseController controller;

    @BeforeEach
    void setUp() {
        service = mock(GithubService.class);
        controller = new GitReleaseController(service);
    }

    @Test
    void returnsLatestVersion() {
        GitReleaseDTO dto = new GitReleaseDTO("v1.2.3", "", OffsetDateTime.now());
        when(service.getLatestVersion()).thenReturn(dto);

        HttpResponse<GitReleaseDTO> response = controller.getBuildInformation();

        assertEquals(200, response.getStatus().getCode());
        assertEquals(dto, response.body());
        verify(service, times(1)).getLatestVersion();
    }
}
