package net.onelitefeather.vulpes.generator.controller.generation;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Controller("/vulpes")
public class VulpesGenerationController {

    @Operation(
            summary = "Generate a new Vulpes version",
            operationId = "generateVulpes",
            description = "Generates a new code base for Vulpes based on the provided branch",
            tags = {"generation"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The generation was successful",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Object.class)
            )
    )
    @Get("/generate")
    public HttpResponse<Object> generateProject(@QueryValue(value = "branch") String branch) {
        return HttpResponse.ok();
    }
}

