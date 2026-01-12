package net.onelitefeather.vulpes.generator.controller.download;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.server.types.files.SystemFile;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.domain.client.GithubService;
import net.onelitefeather.vulpes.generator.domain.generation.GenerationResponse;
import net.onelitefeather.vulpes.generator.domain.generation.VulpesGenerationService;
import net.onelitefeather.vulpes.generator.domain.generation.exception.GenerationException;
import net.onelitefeather.vulpes.generator.util.FileHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Controller("/download")
@ExecuteOn(TaskExecutors.BLOCKING)
public class VulpesDownloadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VulpesDownloadController.class);

    private final VulpesGenerationService generationService;
    private final GithubService githubService;

    @Inject
    public VulpesDownloadController(
            VulpesGenerationService generationService,
            GithubService githubService
    ) {
        this.generationService = generationService;
        this.githubService = githubService;
    }

    @Operation(
            summary = "Download a generated Vulpes code base",
            operationId = "download",
            description = "Downloads a zip file containing the generated Vulpes code base from the specified branch.",
            tags = {"download"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The download was successful",
            content = @Content(
                    mediaType = "application/octet-stream",
                    schema = @Schema(implementation = File.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid branch or no branches available",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GenerationResponse.GenerationErrorResponseDTO.class)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "An error occurred during generation",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GenerationResponse.GenerationErrorResponseDTO.class)
            )
    )
    @Get()
    public @NotNull HttpResponse<Object> download(
            @QueryValue(value = "branch", defaultValue = "develop") String branch
    ) {
        List<String> activeBranches = this.githubService.getBranches();

        if (activeBranches.isEmpty()) {
            return HttpResponse.badRequest(
                    new GenerationResponse.GenerationErrorResponseDTO("No branches available")
            );
        }

        if (!activeBranches.contains(branch)) {
            return HttpResponse.badRequest(
                    new GenerationResponse.GenerationErrorResponseDTO(
                            "Branch '" + branch + "' does not exist. Available branches: " + String.join(", ", activeBranches)
                    )
            );
        }

        try {
            Path generatedProject = this.generationService.getVulpesGeneration(branch);
            Path zipFile = generatedProject.getParent().resolve("vulpes-" + branch + ".zip");
            FileHelper.zipFile(generatedProject, zipFile);

            SystemFile systemFile = new SystemFile(zipFile.toFile()).attach("vulpes-" + branch + ".zip");
            return HttpResponse.ok(systemFile);
        } catch (GenerationException exception) {
            LOGGER.error("Generation failed for branch: {}", branch, exception);
            return HttpResponse.serverError(
                    new GenerationResponse.GenerationErrorResponseDTO(
                            "Generation failed: " + exception.getMessage()
                    )
            );
        } catch (Exception exception) {
            LOGGER.error("Failed to create zip file for branch: {}", branch, exception);
            return HttpResponse.serverError(
                    new GenerationResponse.GenerationErrorResponseDTO(
                            "Failed to create zip file: " + exception.getMessage()
                    )
            );
        }
    }
}
