package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.domain.build.BuildInformationDTO;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Controller("/build")
public class BuildInformationHandler {


    @Inject
    public BuildInformationHandler() {
    }

    @Operation(
            summary = "Get build information",
            description = "Returns the latest build information including version and creation date.",
            tags = {"build"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "Build information retrieved successfully.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BuildInformationDTO.class)
            )
    )
    @Get(value = "/data", produces = MediaType.APPLICATION_JSON)
    public @NotNull HttpResponse<BuildInformationDTO> getBuildInformation() {
        //TODO: Reimplement it with Github integration
        Map<String, String> data = new HashMap<>();
        data.put("version", "1.0.0");
        data.put("created", System.currentTimeMillis() + "");
        return HttpResponse.ok(new BuildInformationDTO(data));
    }
}
