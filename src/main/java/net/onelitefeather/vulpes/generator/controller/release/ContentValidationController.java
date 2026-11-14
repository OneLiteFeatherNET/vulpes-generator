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
import net.onelitefeather.vulpes.generator.release.ContentValidationService;

import java.util.UUID;

/**
 * Lets the editor check the content of a project before a release. The check reads the content, reports every
 * problem and builds, compiles and publishes nothing.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Controller("/projects/{projectId}")
public class ContentValidationController {

    private final ContentValidationService validation;

    /**
     * Creates the controller.
     *
     * @param validation checks the content of a project
     */
    @Inject
    public ContentValidationController(ContentValidationService validation) {
        this.validation = validation;
    }

    /**
     * Returns the content problems of the project.
     *
     * @param projectId the id of the project in the backend
     * @return {@code 200} with every problem; the list is empty when the content is valid
     */
    @Operation(
            summary = "List the content problems of a project",
            operationId = "getContentValidation",
            description = "Checks the current content of the project: every reference to a font texture, a sound file, an item "
                    + "model or a texture must resolve to an asset, keys and asset paths must be unique and valid resource "
                    + "locations, and the glyph rows of a bitmap font must have one width with no character used twice under "
                    + "one font key. Every problem is listed, each naming its content entry. Nothing is built, compiled or "
                    + "published. The baseBranch parameter is accepted for symmetry with the local build and does not change "
                    + "the answer, because only the content is checked.",
            tags = {"release"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The content problems; the list is empty when the content is valid.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ContentValidationResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "The project id is not a UUID.")
    @ApiResponse(
            responseCode = "422",
            description = "The project is unknown, or its content cannot be assembled into a snapshot.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "502",
            description = "The backend cannot be reached or answered without a usable content export.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ReleaseErrorResponse.class)
            )
    )
    @Get("/validation{?baseBranch}")
    @ExecuteOn(TaskExecutors.BLOCKING)
    public ContentValidationResponse validation(@PathVariable UUID projectId) {
        return new ContentValidationResponse(validation.validate(projectId));
    }
}
