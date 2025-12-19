package net.theevilreaper.vulpes.generator.controller.git;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.domain.client.GithubService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.HttpResponse;

import jakarta.inject.Singleton;
import org.mockito.Mockito;

import java.util.List;

@MicronautTest
class GitBranchControllerIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    GithubService githubService;

    @Singleton
    @Replaces(GithubService.class)
    GithubService mockGithubService() {
        return Mockito.mock(GithubService.class);
    }

    @Test
    void controllerFiltersBranchesCorrectly() {
        GithubService mockService = Mockito.mock(GithubService.class);
        GitBranchController controller = new GitBranchController(mockService);

        List<String> branches = List.of("main", "develop", "renovate/update-deps");
        when(mockService.getBranches()).thenReturn(branches);

        HttpResponse<List<String>> response = controller.getBranches();
        assertEquals(200, response.getStatus().getCode());
        assertEquals(List.of("main", "develop"), response.body());
    }
}
