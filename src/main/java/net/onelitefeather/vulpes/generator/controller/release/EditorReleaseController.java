package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.release.EditorReleaseService;
import net.onelitefeather.vulpes.generator.release.ReleaseResult;

/**
 * Starts a content release from the editor. The route exists only when {@code vulpes.release.editor-route-enabled}
 * is {@code true}; otherwise Micronaut answers {@code 404}. It needs no release token, because the editor has no
 * login yet, so it is meant for a generator that only the internal network reaches.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Requires(property = "vulpes.release.editor-route-enabled", value = "true")
@Controller("/generator")
public class EditorReleaseController {

    private final EditorReleaseService editorReleaseService;

    /**
     * Creates the controller.
     *
     * @param editorReleaseService the service that starts the release
     */
    @Inject
    public EditorReleaseController(EditorReleaseService editorReleaseService) {
        this.editorReleaseService = editorReleaseService;
    }

    /**
     * Publishes a release of the project, with the version the editor chose.
     *
     * @param request the project id and the version
     * @return {@code 201} with the coordinates and the revision of the published release
     */
    @Operation(
            summary = "Publish a content release from the editor",
            operationId = "publishEditorRelease",
            description = "Assembles the snapshot of the project from the backend, computes its revision and publishes "
                    + "the content data JAR under the project key. The release is created by stelaris. "
                    + "Only available when vulpes.release.editor-route-enabled is true.",
            tags = {"release"}
    )
    @ApiResponse(
            responseCode = "201",
            description = "The release was published.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseResponse.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "The editor route is disabled.")
    @ApiResponse(
            responseCode = "409",
            description = "The version already exists, or the project changed while the snapshot was assembled.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "The project is unknown or holds content the release cannot express yet.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "502",
            description = "The backend or the Maven repository cannot be reached or refused the request.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @Post(value = "/release", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<ReleaseResponse> release(@Body EditorReleaseRequest request) {
        ReleaseResult result = editorReleaseService.release(request.projectId(), request.version(),
                request.baseBranch());
        return HttpResponse.created(new ReleaseResponse(result.coordinates().toString(), result.revision(),
                result.base(), result.repositoryUrl()));
    }
}
