package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.client.GithubClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The application starts without a GitHub token and makes no outbound request to GitHub while starting. The
 * {@link GithubClient} is replaced by a mock, so any call made during startup is recorded and fails the test.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartupWithoutGithubIntegrationTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort(Map.of("github.password", ""));
    }

    @Inject
    GithubClient githubClient;

    @Singleton
    @Replaces(GithubClient.class)
    GithubClient mockGithubClient() {
        return Mockito.mock(GithubClient.class);
    }

    @Test
    void startupMakesNoRequestToGithub() {
        verifyNoInteractions(githubClient);
    }
}
