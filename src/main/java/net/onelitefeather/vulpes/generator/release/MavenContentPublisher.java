package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.MavenContentConfiguration;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Publishes content data JARs to the Reposilite repositories with Maven Resolver.
 * <p>
 * {@code X.Y.Z} versions go to the releases repository, {@code X.Y.Z-dev.N} versions to the snapshots
 * repository. Every upload is one deploy request with the JAR, its POM and its {@code .module} file;
 * Maven Resolver writes the checksums and the {@code maven-metadata.xml}. Existing versions are checked
 * before the upload, so they are never overwritten.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class MavenContentPublisher implements ContentPublisher {

    /** The checksum algorithms written next to every uploaded file. */
    private static final String CHECKSUM_ALGORITHMS = "MD5,SHA-1,SHA-256,SHA-512";
    private static final String LOCAL_REPOSITORY_DIR = "local-repository";

    private final MavenContentConfiguration configuration;
    private final RepositorySystem repositorySystem;
    private final HttpClient httpClient;

    /**
     * Creates the publisher.
     *
     * @param configuration the repository URLs and credentials
     */
    public MavenContentPublisher(MavenContentConfiguration configuration) {
        this.configuration = configuration;
        this.repositorySystem = new RepositorySystemSupplier().get();
        // HTTP/1.1 because Reposilite is plain HTTP/1.1 and the HTTP/2 upgrade attempt is not needed for HEAD.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Returns the repository a version is published to.
     *
     * @param coordinates   the coordinates of the release
     * @param configuration the repository configuration
     * @return the releases URL for {@code X.Y.Z}, the snapshots URL for {@code X.Y.Z-dev.N}
     */
    static String repositoryUrl(ContentCoordinates coordinates, MavenContentConfiguration configuration) {
        return coordinates.isDevVersion() ? configuration.snapshotsUrl() : configuration.releasesUrl();
    }

    @Override
    public boolean exists(ContentCoordinates coordinates) {
        URI pomUri = URI.create(repositoryUrl(coordinates, configuration) + "/" + artifactPath(coordinates)
                + "/" + coordinates.artifactId() + "-" + coordinates.version() + ".pom");
        HttpRequest.Builder request = HttpRequest.newBuilder(pomUri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30));
        String authorization = basicAuthorization();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        try {
            HttpResponse<Void> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
            return switch (response.statusCode()) {
                case 200 -> true;
                case 404 -> false;
                default -> throw unavailable("repository answered " + response.statusCode() + " for " + pomUri);
            };
        } catch (IOException e) {
            throw unavailable("repository " + pomUri + " cannot be reached: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("interrupted while checking " + pomUri);
        }
    }

    @Override
    public void publish(ContentCoordinates coordinates, Path jarFile) {
        String jarFileName = coordinates.artifactId() + "-" + coordinates.version() + ".jar";
        Path dir = jarFile.getParent();
        try {
            byte[] jar = Files.readAllBytes(jarFile);
            Path pomFile = dir.resolve(coordinates.artifactId() + "-" + coordinates.version() + ".pom");
            Path moduleFile = dir.resolve(coordinates.artifactId() + "-" + coordinates.version() + ".module");
            Files.writeString(pomFile, ContentPomWriter.write(coordinates), StandardCharsets.UTF_8);
            Files.writeString(moduleFile, GradleModuleWriter.write(coordinates, jarFileName, jar), StandardCharsets.UTF_8);

            RepositorySystemSession session = newSession(dir.resolve(LOCAL_REPOSITORY_DIR));
            DeployRequest deployRequest = new DeployRequest()
                    .setRepository(remoteRepository(repositoryUrl(coordinates, configuration)))
                    .addArtifact(artifact(coordinates, "jar", jarFile))
                    .addArtifact(artifact(coordinates, "pom", pomFile))
                    .addArtifact(artifact(coordinates, "module", moduleFile));
            repositorySystem.deploy(session, deployRequest);
        } catch (DeploymentException e) {
            throw unavailable("upload of " + coordinates + " failed: " + e.getMessage());
        } catch (IOException e) {
            throw unavailable("cannot prepare the upload of " + coordinates + ": " + e.getMessage());
        }
    }

    /** A session with a local repository inside the build workspace, never the user's ~/.m2. */
    private RepositorySystemSession newSession(Path localRepositoryDir) {
        RepositorySystemSession.SessionBuilder builder = repositorySystem.createSessionBuilder();
        builder.withLocalRepositories(new LocalRepository(localRepositoryDir));
        builder.setConfigProperty("aether.checksums.algorithms", CHECKSUM_ALGORITHMS);
        return builder.build();
    }

    private RemoteRepository remoteRepository(String url) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder("onelitefeather", "default", url);
        Authentication authentication = authentication();
        if (authentication != null) {
            builder.setAuthentication(authentication);
        }
        return builder.build();
    }

    private Authentication authentication() {
        if (configuration.username() == null || configuration.username().isBlank()) {
            return null;
        }
        return new AuthenticationBuilder()
                .addUsername(configuration.username())
                .addPassword(configuration.password() == null ? "" : configuration.password())
                .build();
    }

    private String basicAuthorization() {
        if (configuration.username() == null || configuration.username().isBlank()) {
            return null;
        }
        String credentials = configuration.username() + ":" + (configuration.password() == null ? "" : configuration.password());
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static DefaultArtifact artifact(ContentCoordinates coordinates, String extension, Path file) {
        return new DefaultArtifact(coordinates.groupId(), coordinates.artifactId(), "", extension,
                coordinates.version(), Map.of(), file.toFile());
    }

    private static String artifactPath(ContentCoordinates coordinates) {
        return coordinates.groupId().replace('.', '/') + "/" + coordinates.artifactId() + "/" + coordinates.version();
    }

    private static ReleaseRejectedException unavailable(String message) {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.REPOSITORY_UNAVAILABLE, message);
    }
}
