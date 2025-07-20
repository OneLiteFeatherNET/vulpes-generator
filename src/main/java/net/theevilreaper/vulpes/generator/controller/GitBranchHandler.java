package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.git.GitWorker;
import net.theevilreaper.vulpes.generator.util.BranchFilter;

import java.util.List;

import static net.theevilreaper.vulpes.generator.util.Constants.EMPTY_STRING;

@Controller("/git")
public class GitBranchHandler {

    private final GitWorker gitWorker;

    @Inject
    public GitBranchHandler(GitWorker gitWorker) {
        this.gitWorker = gitWorker;
    }

    @Operation(
            summary = "Get all branches",
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
    public HttpResponse<List<String>> getBranches(@QueryValue(value = "full", defaultValue = "false") boolean full) {
        List<String> gitRefs = gitWorker.getGitRefs();
        List<String> filtered = BranchFilter.filterBranches(gitRefs, string -> !string.contains("/renovate"));

        if (full) {
            return HttpResponse.ok(filtered).contentType(MediaType.APPLICATION_JSON);
        }
        List<String> branches = filtered.stream().map(string -> string.replace("refs/heads/", EMPTY_STRING)).toList();
        return HttpResponse.ok(branches).contentType(MediaType.APPLICATION_JSON);
    }
}
