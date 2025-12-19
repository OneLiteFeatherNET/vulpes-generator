package net.theevilreaper.vulpes.generator.controller.git.branch;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.controller.git.GitBranchController;
import net.theevilreaper.vulpes.generator.domain.client.GithubService;
import net.theevilreaper.vulpes.generator.domain.configuration.BranchFilterConfiguration;
import org.junit.jupiter.api.Test;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Singleton;
import org.mockito.Mockito;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@MicronautTest
class GitBranchControllerIntegrationTest {

    @Inject
    GitBranchController controller;

    @Inject
    GithubService githubService;

    @Inject
    BranchFilterConfiguration branchFilterConfiguration;

    @Inject
    @Client("/")
    HttpClient client;

    @Singleton
    @Replaces(GithubService.class)
    GithubService mockGithubService() {
        return Mockito.mock(GithubService.class);
    }

    @Test
    void controllerFiltersBranchesCorrectly () {
        List<String> branches = List.of(
                "main",
                "develop",
                "renovate/update-deps"
        );
        when(githubService.getBranches()).thenReturn(branches);

        HttpResponse<List<String>> response = controller.getBranches();
        assertEquals(200, response.getStatus().getCode());
        assertEquals(List.of("main", "develop"), response.body());
    }

    @Test
    void filtersBranchesViaHttp() {
        var response = client.toBlocking()
                .retrieve(HttpRequest.GET("git/branches"), Argument.listOf(String.class));

        assertEquals(List.of("main", "develop"), response);
    }
}
