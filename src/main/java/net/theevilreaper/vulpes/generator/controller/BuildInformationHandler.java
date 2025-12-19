package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.domain.client.GithubReleaseService;
import net.theevilreaper.vulpes.generator.domain.release.GitReleaseDTO;
import org.jetbrains.annotations.NotNull;

@Controller("/build")
public class BuildInformationHandler {

    private final GithubReleaseService releaseService;

    /**
     * Creates a new instance of the {@link BuildInformationHandler}
     *
     * @param releaseService the {@link GithubReleaseService} to use for retrieving
     *                       the latest release information.
     */
    @Inject
    public BuildInformationHandler(GithubReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    /**
     * Returns the latest build information with a specific level of detail.
     *
     * @return the latest build information
     */
    @Operation(
            summary = "Get build information",
            operationId = "getBuildData",
            description = "Returns the latest build information including version and creation date.",
            tags = {"build"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "Build information retrieved successfully.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = GitReleaseDTO.class)
            )
    )
    @Get(value = "/data", produces = MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    public @NotNull HttpResponse<GitReleaseDTO> getBuildInformation() {
        GitReleaseDTO latestVersion = this.releaseService.getLatestVersion();
        return HttpResponse.ok(latestVersion);
    }
}
