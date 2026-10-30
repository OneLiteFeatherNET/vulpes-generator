package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;

/**
 * Maps a refused release to its HTTP status:
 * <ul>
 *     <li>{@code 400}: the request is invalid</li>
 *     <li>{@code 409}: the version already exists</li>
 *     <li>{@code 422}: the snapshot is rejected or the project is unknown</li>
 *     <li>{@code 502}: the backend or the Maven repository cannot be used</li>
 * </ul>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Produces
@Singleton
public class ReleaseRejectedExceptionHandler
        implements ExceptionHandler<ReleaseRejectedException, HttpResponse<ReleaseErrorResponse>> {

    @Override
    public HttpResponse<ReleaseErrorResponse> handle(HttpRequest request, ReleaseRejectedException exception) {
        ReleaseRejectedException.Reason reason = exception.getReason();
        return HttpResponse.status(statusOf(reason))
                .body(new ReleaseErrorResponse(reason.name(), exception.getMessage()));
    }

    static HttpStatus statusOf(ReleaseRejectedException.Reason reason) {
        return switch (reason) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case VERSION_EXISTS -> HttpStatus.CONFLICT;
            case REVISION_MISMATCH, UNSUPPORTED_FORMAT, PROJECT_MISMATCH, UNKNOWN_PROJECT -> HttpStatus.UNPROCESSABLE_ENTITY;
            case SOURCE_UNAVAILABLE, REPOSITORY_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
        };
    }
}
