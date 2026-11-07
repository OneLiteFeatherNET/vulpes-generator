package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.MavenContentConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Reads files from the Maven repositories with the generator's own credentials. A file that does not exist is an empty
 * result; every other answer, and every failure to connect, is a refusal that names the repository. Credentials never
 * appear in a refusal or in a result.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class MavenRepositoryClient {

    /** The name of the releases repository in messages. */
    static final String RELEASES = "releases";
    /** The name of the snapshots repository in messages. */
    static final String SNAPSHOTS = "snapshots";

    private final MavenContentConfiguration configuration;
    private final HttpClient httpClient;

    /**
     * Creates the client.
     *
     * @param configuration the repository URLs and the credentials
     */
    @Inject
    public MavenRepositoryClient(MavenContentConfiguration configuration) {
        this.configuration = configuration;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /**
     * Reads a file of a repository.
     *
     * @param repositoryUrl the URL of the repository, without a trailing slash
     * @param name          the name of the repository in messages, {@link #RELEASES} or {@link #SNAPSHOTS}
     * @param path          the path of the file below the repository
     * @return the bytes of the file, or empty when the repository answers {@code 404}
     * @throws ReleaseRejectedException with {@code REPOSITORY_UNAVAILABLE} when the repository cannot be reached or
     *                                  answers with another status
     */
    public Optional<byte[]> read(String repositoryUrl, String name, String path) {
        String url = repositoryUrl + "/" + path;
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(60));
        String authorization = authorization();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        try {
            HttpResponse<byte[]> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            return switch (response.statusCode()) {
                case 200 -> Optional.of(response.body());
                case 404 -> Optional.empty();
                default -> throw unavailable("the " + name + " repository " + repositoryUrl + " answered "
                        + response.statusCode() + " for " + path);
            };
        } catch (IOException e) {
            throw unavailable("the " + name + " repository " + repositoryUrl + " cannot be reached");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("reading from the " + name + " repository " + repositoryUrl + " was interrupted");
        }
    }

    private String authorization() {
        if (configuration.username() == null || configuration.username().isBlank()) {
            return null;
        }
        String credentials = configuration.username() + ":"
                + (configuration.password() == null ? "" : configuration.password());
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static ReleaseRejectedException unavailable(String message) {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.REPOSITORY_UNAVAILABLE, message);
    }
}
