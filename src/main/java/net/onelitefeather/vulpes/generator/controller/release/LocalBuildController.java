package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.core.annotation.Nullable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.release.LocalBuild;
import net.onelitefeather.vulpes.generator.release.LocalBuildService;
import net.onelitefeather.vulpes.generator.release.PublishedLocalBuildService;

import java.util.UUID;

/**
 * Serves the local build of a project: a zip with the release and the snapshot of the project, verified against a
 * {@code vulpes-base} branch, which the Vulpes Gradle plugin builds on a developer's machine.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Controller("/projects/{projectId}")
public class LocalBuildController {

    static final MediaType ZIP = MediaType.of("application/zip");

    private final LocalBuildService localBuildService;
    private final PublishedLocalBuildService publishedLocalBuildService;

    /**
     * Creates the controller.
     *
     * @param localBuildService          builds the zip of the snapshot
     * @param publishedLocalBuildService builds the zip of a published version
     */
    @Inject
    public LocalBuildController(LocalBuildService localBuildService,
                                PublishedLocalBuildService publishedLocalBuildService) {
        this.localBuildService = localBuildService;
        this.publishedLocalBuildService = publishedLocalBuildService;
    }

    /**
     * Returns the local build of the project.
     *
     * @param projectId  the id of the project in the backend
     * @param baseBranch the {@code vulpes-base} branch, {@code develop} when omitted
     * @return {@code 200} with the zip as an attachment named {@code <project>-local.zip}
     */
    @Operation(
            summary = "Download the local build of a project",
            operationId = "getLocalBuild",
            description = "Verifies the snapshot of the project against a vulpes-base branch and returns a zip with "
                    + "vulpes/<project>/release.json and vulpes/<project>/snapshot.json. No sources and no git data are included.",
            tags = {"release"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The local build zip.",
            content = @Content(mediaType = "application/zip", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(
            responseCode = "422",
            description = "The project is unknown, the branch is unknown or the sources do not compile; the body lists the diagnostics.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReleaseErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "502",
            description = "The backend or the vulpes-base repository cannot be reached.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReleaseErrorResponse.class))
    )
    @Get("/local-build{?baseBranch}")
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<byte[]> localBuild(@PathVariable UUID projectId,
                                           @Nullable @QueryValue String baseBranch) {
        LocalBuild build = localBuildService.build(projectId, baseBranch);
        return HttpResponse.ok(build.zip())
                .contentType(ZIP)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + build.projectKey() + "-local.zip\"");
    }

    /**
     * Returns the local build of a published version of the project.
     *
     * @param projectId the id of the project in the backend
     * @param version   the published version, {@code X.Y.Z} or {@code X.Y.Z-SNAPSHOT}
     * @return {@code 200} with the zip as an attachment named {@code <project>-<version>.zip}
     */
    @Operation(
            summary = "Download the local build of a published version",
            operationId = "getPublishedLocalBuild",
            description = "Reads the data JAR of the version from its repository and returns the zip of a local build. "
                    + "release.json and snapshot.json are copied byte for byte from the JAR.",
            tags = {"release"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The local build zip.",
            content = @Content(mediaType = "application/zip", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "400", description = "The version is not X.Y.Z or X.Y.Z-SNAPSHOT.")
    @ApiResponse(responseCode = "404", description = "The version is not published in its repository.")
    @ApiResponse(responseCode = "422", description = "The project is unknown or its key is not a valid artifact id.")
    @ApiResponse(
            responseCode = "502",
            description = "A repository cannot be read, or the published JAR does not match its own revision.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReleaseErrorResponse.class))
    )
    @Get("/releases/{version}/local-build")
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<byte[]> publishedLocalBuild(@PathVariable UUID projectId, @PathVariable String version) {
        LocalBuild build = publishedLocalBuildService.build(projectId, version);
        return HttpResponse.ok(build.zip())
                .contentType(ZIP)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + build.projectKey() + "-" + version + ".zip\"");
    }
}
