package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Requires {@code Authorization: Bearer <vulpes.release.token>} on {@code /releases}. Every other request passes.
 * <p>
 * The presented and the configured token are both hashed with SHA-256 and compared with
 * {@link MessageDigest#isEqual(byte[], byte[])}, so the comparison takes the same time for every token length.
 * A blank configured token refuses every request (fail closed). Neither the token nor the header is logged.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
@Filter({"/releases", "/releases/**"})
public class ReleaseTokenFilter implements HttpServerFilter {

    private static final String BEARER = "Bearer ";
    private static final String UNAUTHORIZED_BODY = "{\"reason\":\"UNAUTHORIZED\","
            + "\"message\":\"a valid bearer token is required\"}";

    /** SHA-256 of the configured token, or {@code null} when no token is configured. */
    private final byte[] expectedDigest;

    /**
     * Creates the filter.
     *
     * @param token the configured release token, blank when releases are disabled
     */
    @Inject
    public ReleaseTokenFilter(@Value("${vulpes.release.token:}") String token) {
        this.expectedDigest = token == null || token.isBlank() ? null : sha256(token);
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (isAuthorized(request.getHeaders().get(HttpHeaders.AUTHORIZATION))) {
            return chain.proceed(request);
        }
        MutableHttpResponse<?> unauthorized = HttpResponse.unauthorized()
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(UNAUTHORIZED_BODY);
        return Publishers.just(unauthorized);
    }

    private boolean isAuthorized(String authorization) {
        if (expectedDigest == null || authorization == null || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return false;
        }
        byte[] presented = sha256(authorization.substring(BEARER.length()));
        return MessageDigest.isEqual(presented, expectedDigest);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }
}
