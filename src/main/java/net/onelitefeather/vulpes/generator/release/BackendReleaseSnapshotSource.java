package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;

import java.net.URI;
import java.util.UUID;

/**
 * Fetches the frozen snapshot of a project from the Vulpes backend with {@code GET /project/{projectId}/snapshot}.
 * The body is taken as raw bytes and is never decoded, so the revision check sees exactly what the backend sent.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BackendReleaseSnapshotSource implements ReleaseSnapshotSource {

    private final HttpClient client;
    private final String backendUrl;
    private final String token;

    /**
     * Creates the source.
     *
     * @param client     the HTTP client of the {@code vulpes-backend} service, which has its own body size limit
     *                   ({@code micronaut.http.services.vulpes-backend.max-content-length}); requests use absolute URIs
     * @param backendUrl the base URL of the backend, for example {@code http://vulpes-backend:8080}
     * @param token      the bearer token for the backend, sent only when it is not blank
     */
    public BackendReleaseSnapshotSource(@Client(id = "vulpes-backend") HttpClient client,
                                        @Value("${vulpes.backend.url}") String backendUrl,
                                        @Value("${vulpes.backend.token:}") String token) {
        this.client = client;
        this.backendUrl = backendUrl;
        this.token = token;
    }

    @Override
    public byte[] fetch(UUID projectId) {
        MutableHttpRequest<?> request = HttpRequest.GET(URI.create(backendUrl + "/project/" + projectId + "/snapshot"));
        if (!token.isBlank()) {
            request.bearerAuth(token);
        }
        try {
            HttpResponse<byte[]> response = client.toBlocking().exchange(request, byte[].class);
            byte[] body = response.body();
            if (body == null) {
                throw new ReleaseRejectedException(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE,
                        "backend answered without a snapshot for project " + projectId);
            }
            return body;
        } catch (HttpClientResponseException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw new ReleaseRejectedException(ReleaseRejectedException.Reason.UNKNOWN_PROJECT,
                        "backend does not know project " + projectId);
            }
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE,
                    "backend answered " + e.getStatus() + " for project " + projectId);
        } catch (HttpClientException e) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE,
                    "backend cannot be reached: " + e.getMessage());
        }
    }
}
