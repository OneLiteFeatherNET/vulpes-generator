package net.onelitefeather.vulpes.generator.release;

import net.onelitefeather.vulpes.generator.domain.configuration.MavenContentConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the routing of versions to repositories and for the repository being unreachable.
 *
 * @author TheMeinerLP
 */
class MavenContentPublisherTest {

    private static final MavenContentConfiguration CONFIGURATION = new MavenContentConfiguration(
            "user", "secret",
            "https://repo.onelitefeather.dev/onelitefeather-releases",
            "https://repo.onelitefeather.dev/onelitefeather-snapshots");

    @Test
    void releaseVersionIsRoutedToTheReleasesRepository() {
        String url = MavenContentPublisher.repositoryUrl(ContentCoordinates.forRelease("eldoria", "1.2.0"), CONFIGURATION);

        assertEquals("https://repo.onelitefeather.dev/onelitefeather-releases", url,
                "X.Y.Z must be published to the releases repository");
    }

    @Test
    void devVersionIsRoutedToTheSnapshotsRepository() {
        String url = MavenContentPublisher.repositoryUrl(ContentCoordinates.forRelease("eldoria", "1.2.0-dev.3"), CONFIGURATION);

        assertEquals("https://repo.onelitefeather.dev/onelitefeather-snapshots", url,
                "X.Y.Z-dev.N must be published to the snapshots repository");
    }

    @Test
    void unreachableRepositoryIsReportedAsRepositoryUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            closedPort = probe.getLocalPort();
        }
        MavenContentConfiguration unreachable = new MavenContentConfiguration("user", "secret",
                "http://localhost:" + closedPort + "/releases", "http://localhost:" + closedPort + "/snapshots");
        MavenContentPublisher publisher = new MavenContentPublisher(unreachable);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> publisher.exists(ContentCoordinates.forRelease("eldoria", "1.2.0")));

        assertEquals(ReleaseRejectedException.Reason.REPOSITORY_UNAVAILABLE, e.getReason(),
                "a refused connection must be reported as REPOSITORY_UNAVAILABLE");
    }
}
