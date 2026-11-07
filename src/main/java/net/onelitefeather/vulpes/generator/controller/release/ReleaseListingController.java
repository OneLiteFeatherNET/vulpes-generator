package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.release.PublishedReleaseService;
import net.onelitefeather.vulpes.generator.release.PublishedVersions;

import java.util.UUID;

/**
 * Lists the versions of a project that are published in the releases and the snapshots repositories. The generator
 * reads the repositories with its own credentials, so the editor needs none.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Controller("/projects/{projectId}/releases")
public class ReleaseListingController {

    private final PublishedReleaseService publishedReleases;

    /**
     * Creates the controller.
     *
     * @param publishedReleases lists the published versions
     */
    @Inject
    public ReleaseListingController(PublishedReleaseService publishedReleases) {
        this.publishedReleases = publishedReleases;
    }

    /**
     * Returns the published versions of the project.
     *
     * @param projectId the id of the project in the backend
     * @return {@code 200} with the versions and the latest version of each channel
     */
    @Operation(
            summary = "List the published versions of a project",
            operationId = "listPublishedReleases",
            description = "Reads maven-metadata.xml of the releases and the snapshots repository for the project key of the "
                    + "project. Each snapshot version appears once. A repository that cannot be read is an error; no partial list is returned.",
            tags = {"release"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The published versions; empty when the project was never published.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PublishedVersions.class))
    )
    @ApiResponse(responseCode = "400", description = "The project id is not a UUID.")
    @ApiResponse(responseCode = "422", description = "The project is unknown or its key is not a valid artifact id.")
    @ApiResponse(responseCode = "502", description = "The backend or a Maven repository cannot be read.")
    @Get
    @ExecuteOn(TaskExecutors.BLOCKING)
    public PublishedVersions list(@PathVariable UUID projectId) {
        return publishedReleases.list(projectId);
    }
}
