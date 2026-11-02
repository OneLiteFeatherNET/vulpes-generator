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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE;

/**
 * Reads the REST endpoints of the Vulpes backend. Every request carries the bearer token when one is configured.
 * List endpoints are read page by page until {@code totalSize} entries are collected; a bare JSON array is taken whole.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
final class BackendApiClient {

    /** The page size of every list request. */
    static final int PAGE_SIZE = 100;

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

    /**
     * Reads every entry of a list endpoint.
     *
     * @param path the path below the base URL, without query
     * @return all entries in the order the backend returned them
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#SOURCE_UNAVAILABLE} when a page is
     *                                  missing, malformed or the backend stops before {@code totalSize}
     */
    List<JsonNode> list(String path) {
        List<JsonNode> entries = new ArrayList<>();
        for (int page = 0; ; page++) {
            String pagePath = path + "?size=" + PAGE_SIZE + "&page=" + page;
            JsonNode body = find(pagePath).orElseThrow(() -> unavailable(path + " does not exist"));
            if (body.isArray()) {
                body.forEach(entries::add);
                return entries;
            }
            JsonNode content = body.get("content");
            JsonNode totalSize = body.get("totalSize");
            if (content == null || !content.isArray() || totalSize == null || !totalSize.isNumber()) {
                throw unavailable(path + " answered no page");
            }
            content.forEach(entries::add);
            if (entries.size() >= totalSize.asLong()) {
                return entries;
            }
            if (content.isEmpty()) {
                throw unavailable(path + " ended at " + entries.size() + " of " + totalSize.asLong() + " entries");
            }
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
