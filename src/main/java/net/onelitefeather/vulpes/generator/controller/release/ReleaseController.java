package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.release.ReleaseRequest;
import net.onelitefeather.vulpes.generator.release.ReleaseResult;
import net.onelitefeather.vulpes.generator.release.ReleaseService;

/**
 * Publishes a frozen project snapshot as a content data JAR. The controller only translates between HTTP and
 * {@link ReleaseService}; rejections are mapped to status codes by {@link ReleaseRejectedExceptionHandler}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@SecurityScheme(
        name = "releaseToken",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        description = "The shared release token from vulpes.release.token. Without a configured token every release request is refused."
)
@Controller("/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    /**
     * Creates the controller.
     *
     * @param releaseService the service that performs the release
     */
    @Inject
    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    /**
     * Publishes a release.
     *
     * @param request the release request
     * @return {@code 201} with the coordinates and the revision of the published release
     */
    @Operation(
            summary = "Publish a content release",
            operationId = "publishRelease",
            description = "Fetches the frozen snapshot of the project from the backend, verifies it against the revision, "
                    + "builds a reproducible content data JAR and publishes it to Reposilite. Versions X.Y.Z go to the "
                    + "releases repository, X.Y.Z-dev.N to the snapshots repository. An existing version is never overwritten.",
            tags = {"release"},
            security = @SecurityRequirement(name = "releaseToken")
    )
    @ApiResponse(
            responseCode = "201",
            description = "The release was published.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "The bearer token is missing, uses another scheme or is wrong, or no release token is configured. "
                    + "Nothing was fetched or published.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            ),
            headers = @Header(
                    name = "WWW-Authenticate",
                    description = "Always Bearer",
                    schema = @Schema(type = "string", example = "Bearer")
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The request is invalid. Nothing was fetched or published.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "The version already exists in the target repository. Nothing was uploaded.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "The snapshot was rejected: revision mismatch, unsupported format, foreign project or unknown "
                    + "project. Nothing was uploaded.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "502",
            description = "The backend or the Maven repository cannot be reached or refused the request. Nothing was published.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<ReleaseResponse> release(@Body ReleaseRequest request) {
        ReleaseResult result = releaseService.release(request);
        return HttpResponse.created(new ReleaseResponse(result.coordinates().toString(), result.revision()));
    }
}
