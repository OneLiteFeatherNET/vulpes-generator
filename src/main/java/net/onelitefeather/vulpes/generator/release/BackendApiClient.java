package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE;

/**
 * Reads the REST endpoints of the Vulpes backend that a release uses: the project object and the content export.
 * Every request carries the bearer token when one is configured.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
final class BackendApiClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient client;
    private final String baseUrl;
    private final String token;

    /**
     * Creates the client.
     *
     * @param client  the HTTP client of the backend service, which has its own body size limit
     * @param baseUrl the base URL of the backend, for example {@code http://vulpes-backend:8080}
     * @param token   the bearer token, sent only when it is not blank
     */
    BackendApiClient(HttpClient client, String baseUrl, String token) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.token = token;
    }

    /**
     * Reads one object.
     *
     * @param path the path below the base URL
     * @return the object, or empty when the backend answers 404
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#SOURCE_UNAVAILABLE} for any other failure
     */
    Optional<JsonNode> find(String path) {
        MutableHttpRequest<?> request = HttpRequest.GET(URI.create(baseUrl + path));
        if (!token.isBlank()) {
            request.bearerAuth(token);
        }
        try {
            HttpResponse<byte[]> response = client.toBlocking().exchange(request, byte[].class);
            return Optional.of(parse(path, response.body()));
        } catch (HttpClientResponseException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw unavailable(path + " answered " + e.getStatus());
        } catch (HttpClientException e) {
            throw unavailable("backend cannot be reached: " + e.getMessage());
        }
    }

    private static JsonNode parse(String path, byte[] body) {
        if (body == null || body.length == 0) {
            throw unavailable(path + " answered without a body");
        }
        try {
            return JSON.readTree(body);
        } catch (IOException e) {
            throw unavailable(path + " answered invalid JSON");
        }
    }

    private static ReleaseRejectedException unavailable(String message) {
        return new ReleaseRejectedException(SOURCE_UNAVAILABLE, "backend: " + message);
    }
}
