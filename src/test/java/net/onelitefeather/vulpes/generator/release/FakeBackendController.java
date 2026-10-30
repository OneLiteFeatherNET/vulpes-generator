package net.onelitefeather.vulpes.generator.release;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;

import java.util.UUID;

/**
 * The backend snapshot endpoint, served by the embedded test server under {@code /fake-backend}.
 *
 * @author TheMeinerLP
 */
@Controller("/fake-backend")
public class FakeBackendController {

    private final FakeBackend backend;

    public FakeBackendController(FakeBackend backend) {
        this.backend = backend;
    }

    @Get("/project/{projectId}/snapshot")
    public HttpResponse<byte[]> snapshot(@PathVariable UUID projectId,
                                         @Nullable @Header("Authorization") String authorization) {
        backend.recordAuthorization(authorization);
        if (backend.isDown()) {
            return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE);
        }
        byte[] snapshot = backend.snapshot(projectId);
        if (snapshot == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok(snapshot).contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }
}
