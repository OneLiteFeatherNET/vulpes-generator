package net.onelitefeather.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.types.files.SystemFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.dto.GenerateCodeRequest;
import net.onelitefeather.vulpes.generator.domain.GenerateCodeResponse;
import net.onelitefeather.vulpes.generator.service.FileService;
import net.onelitefeather.vulpes.generator.service.GeneratorService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Controller for downloading generated code.
 */
@Controller("/download")
@Tag(name = "Download", description = "Operations for downloading generated code")
public class DownloadController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    private final GeneratorService generatorService;
    private final FileService fileService;

    /**
     * Constructor with dependencies.
     *
     * @param generatorService the generator service
     * @param fileService the file service
     */
    @Inject
    public DownloadController(GeneratorService generatorService, FileService fileService) {
        this.generatorService = generatorService;
        this.fileService = fileService;
    }

    /**
     * Generates code and returns it as a ZIP file.
     *
     * @param request the code generation request
     * @return a ZIP file containing the generated code
     */
    @Post("/generate")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(
            summary = "Generate and download code",
            description = "Generates code based on the specified Git reference and returns it as a ZIP file"
    )
    @ApiResponse(
            responseCode = "200",
            description = "ZIP file containing the generated code",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
    )
    @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = GenerateCodeResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = GenerateCodeResponse.class)
            )
    )
    public @NotNull HttpResponse<?> generateAndDownload(@Body @NotNull GenerateCodeRequest request) {
        try {
            String reference = request.reference();
            if (reference == null || reference.isBlank()) {
                return HttpResponse.badRequest(GenerateCodeResponse.GenerateCodeErrorDTO.error("Reference cannot be empty"));
            }

            logger.info("Generating code for reference: {} (type: {})", reference, request.referenceType());
            Path zipPath = generatorService.generateProjectAndCreateZip(reference, request.referenceType());

            if (zipPath == null) {
                return HttpResponse.serverError(GenerateCodeResponse.GenerateCodeErrorDTO.error("Failed to generate code"));
            }

            // SystemFile requires a File object, so we need to convert the Path to a File
            SystemFile systemFile = new SystemFile(zipPath.toFile()).attach("generated_code.zip");

            // Schedule the ZIP file for deletion after it's been sent
            zipPath.toFile().deleteOnExit();

            return HttpResponse.ok(systemFile);
        } catch (Exception e) {
            logger.error("Error generating code: {}", e.getMessage(), e);
            return HttpResponse.serverError(GenerateCodeResponse.GenerateCodeErrorDTO.error("Error generating code: " + e.getMessage()));
        }
    }
}
