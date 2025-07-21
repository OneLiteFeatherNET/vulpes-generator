package net.onelitefeather.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import net.onelitefeather.vulpes.generator.dto.GenerateCodeRequest;
import net.onelitefeather.vulpes.generator.domain.GenerateCodeResponse;
import net.onelitefeather.vulpes.generator.service.GeneratorService;

@Controller("/deploy")
public class DeployController {

    private final GeneratorService generatorService;

    @Inject
    public DeployController(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @Operation(
            summary = "Generate and deploy code",
            description = "Generates code based on the specified Git reference and deploys it",
            tags = {"Deploy"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "Code generated and deployed successfully",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GenerateCodeResponse.GenerateCodeSuccessDTO.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Bad request - invalid input",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GenerateCodeResponse.GenerateCodeErrorDTO.class)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GenerateCodeResponse.GenerateCodeErrorDTO.class)
            )
    )
    @Post("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<GenerateCodeResponse> generateAndDeploy(@Valid @Body GenerateCodeRequest request) {
        String reference = request.reference();
        if (reference == null || reference.isBlank()) {
            return HttpResponse.badRequest(GenerateCodeResponse.GenerateCodeErrorDTO.error("Reference cannot be empty"));
        }

        generatorService.generateProject(reference, request.referenceType());

        return HttpResponse.ok(GenerateCodeResponse.GenerateCodeSuccessDTO.success("Code generated and deployed successfully"));
    }
}
