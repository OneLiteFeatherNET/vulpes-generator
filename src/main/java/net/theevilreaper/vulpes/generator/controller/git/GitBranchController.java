package net.theevilreaper.vulpes.generator.controller.git;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.domain.client.GithubService;
import net.theevilreaper.vulpes.generator.util.BranchFilter;

import java.util.List;

@Controller("/git")
public class GitBranchController {

    private final GithubService githubService;

    @Inject
    public GitBranchController(GithubService githubService) {
        this.githubService = githubService;
    }

    @Operation(
            summary = "Get all branches",
            operationId = "getBranches",
            description = "Returns a list of all branches in the git repository, excluding renovate branches.",
            tags = {"Branches"}
    )
    @ApiResponse(
            responseCode = "500",
            description = "An error occurred while retrieving branches",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class)
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Branches retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = List.class)
            )
    )
    @Get(value = "/branches", produces = "application/json")
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<List<String>> getBranches() {
        List<String> gitRefs = this.githubService.getBranches();

        if (gitRefs.isEmpty()) {
            return HttpResponse.ok(gitRefs);
        }

        List<String> filtered = BranchFilter.filterBranches(gitRefs, string -> !string.contains("/renovate"));
        return HttpResponse.ok(filtered);
    }
}
