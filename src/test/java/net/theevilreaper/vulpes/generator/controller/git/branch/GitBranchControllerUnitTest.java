package net.theevilreaper.vulpes.generator.controller.git.branch;

import io.micronaut.http.HttpResponse;
import net.theevilreaper.vulpes.generator.controller.git.GitBranchController;
import net.theevilreaper.vulpes.generator.domain.client.GithubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GitBranchControllerUnitTest {

    private GithubService githubService;
    private GitBranchController controller;

    @BeforeEach
    void setUp() {
        githubService = mock(GithubService.class);
        controller = new GitBranchController(githubService);
    }

    @Test
    void returnsFilteredBranches() {
        List<String> branches = List.of("main", "develop", "renovate/update-deps");
        when(githubService.getBranches()).thenReturn(branches);

        HttpResponse<List<String>> response = controller.getBranches();

        assertEquals(List.of("main", "develop"), response.body());
        verify(githubService, times(1)).getBranches();
    }

    @Test
    void returnsEmptyListWhenNoBranches() {
        when(githubService.getBranches()).thenReturn(List.of());
        HttpResponse<List<String>> response = controller.getBranches();
        assertEquals(List.of(), response.body());
    }
}
