package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.release.ReleaseRequest;
import net.onelitefeather.vulpes.generator.release.ReleaseResult;
import net.onelitefeather.vulpes.generator.release.ReleaseService;

/**
 * Publishes a frozen project snapshot as a content data JAR. The controller only translates between HTTP and
 * {@link ReleaseService}; rejections are mapped to status codes by {@link ReleaseRejectedExceptionHandler}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Controller("/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    /**
     * Creates the controller.
     *
     * @param releaseService the service that performs the release
     */
    @Inject
    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    /**
     * Publishes a release.
     *
     * @param request the release request
     * @return {@code 201} with the coordinates and the revision of the published release
     */
    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<ReleaseResponse> release(@Body ReleaseRequest request) {
        ReleaseResult result = releaseService.release(request);
        return HttpResponse.created(new ReleaseResponse(result.coordinates().toString(), result.revision()));
    }
}
