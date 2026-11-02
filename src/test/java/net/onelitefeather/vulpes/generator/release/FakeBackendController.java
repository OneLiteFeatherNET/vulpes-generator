package net.onelitefeather.vulpes.generator.release;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;

/**
 * The backend REST API, served by the embedded test server under {@code /fake-backend}. The answers come from
 * {@link FakeBackend}.
 *
 * @author TheMeinerLP
 */
@Controller("/fake-backend")
public class FakeBackendController {

    private final FakeBackend backend;

    public FakeBackendController(FakeBackend backend) {
        this.backend = backend;
    }

    @Get("/{+path}")
    public HttpResponse<String> get(@PathVariable String path,
                                    @Nullable @Header("Authorization") String authorization,
                                    @Nullable @QueryValue Integer size,
                                    @Nullable @QueryValue Integer page) {
        FakeBackend.FakeReply reply = backend.serve(path, authorization, size, page);
        if (reply.status() == 200) {
            return HttpResponse.ok(reply.body()).contentType(MediaType.APPLICATION_JSON_TYPE);
        }
        return HttpResponse.status(HttpStatus.valueOf(reply.status()));
    }
}
