package net.onelitefeather.vulpes.generator.release;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;

/**
 * A Maven repository served by the embedded test server under {@code /fake-maven/repo/}.
 * The repository key is everything after {@code /fake-maven/repo/}, for example
 * {@code onelitefeather-releases/net/onelitefeather/...}.
 * <p>
 * There is no separate HEAD route on purpose: Micronaut answers HEAD with 400 when a GET route with the same
 * template exists, and a GET route answers HEAD requests without a body. The existence check of the publisher
 * therefore reaches this GET handler, and a missing file answers 404 as on a real repository.
 *
 * @author TheMeinerLP
 */
@Controller("/fake-maven")
public class FakeMavenRepositoryController {

    private final FakeMavenRepository repository;

    public FakeMavenRepositoryController(FakeMavenRepository repository) {
        this.repository = repository;
    }

    @Get("/repo/{+path}")
    public HttpResponse<byte[]> get(@PathVariable String path) {
        byte[] content = repository.get(path);
        if (content == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok(content).contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }

    @Put("/repo/{+path}")
    public HttpResponse<Void> put(@PathVariable String path,
                                  @Body byte[] content,
                                  @Nullable @Header("Authorization") String authorization) {
        repository.recordAuthorization(authorization);
        repository.store(path, content);
        return HttpResponse.ok();
    }
}
