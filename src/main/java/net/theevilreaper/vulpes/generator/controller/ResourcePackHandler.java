package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/resourcepack")
public class ResourcePackHandler {

    @Get(value = "/", produces = "application/json")
    public HttpResponse<Object> getResourcePack() {
        return HttpResponse.ok();
    }
}
