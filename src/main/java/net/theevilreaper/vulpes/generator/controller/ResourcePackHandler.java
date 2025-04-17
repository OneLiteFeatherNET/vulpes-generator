package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.generator.domain.pack.PackRequest;
import net.theevilreaper.vulpes.generator.domain.pack.PackResultDTO;
import net.theevilreaper.vulpes.generator.generation.ResourcePackService;

@Controller("/resourcepack")
public class ResourcePackHandler {

    private final ResourcePackService resourcePackService;

    @Inject
    public ResourcePackHandler(ResourcePackService resourcePackService) {
        this.resourcePackService = resourcePackService;
    }

    @Get(value = "/", produces = "application/json")
    public HttpResponse<PackResultDTO> getResourcePack(
            @Body PackRequest request
    ) {
        return HttpResponse.ok();
    }
}
