package net.onelitefeather.vulpes.generator.controller.generation;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.onelitefeather.vulpes.generator.domain.generation.GenerationResponse;

@Controller("/pack")
public class PackGenerationController {

    @Operation(
            summary = "Generate a new ResourcePack version",
            operationId = "generateResourcePack",
            description = "Generates a new ResourcePack code base based on the provided branch",
            tags = {"generation"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "The generation was successful",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GenerationResponse.class)
            )
    )
    @Get("/generate")
    public HttpResponse<GenerationResponse> generateResourcePack() {
        return HttpResponse.ok();
    }

}
