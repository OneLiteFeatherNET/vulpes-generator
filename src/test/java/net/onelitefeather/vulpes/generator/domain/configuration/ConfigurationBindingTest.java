package net.onelitefeather.vulpes.generator.domain.configuration;

import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks that the release configuration binds with the defaults of the design.
 *
 * @author TheMeinerLP
 */
@MicronautTest
class ConfigurationBindingTest {

    @Inject
    BaseConfiguration base;

    @Inject
    BaseBuildCredentials credentials;

    @Inject
    Environment environment;

    @Test
    void baseRepositoryDefaultsToVulpesBase() {
        assertEquals("OneLiteFeatherNET/vulpes-base", base.repository(),
                "vulpes.base.repository must default to the vulpes-base repository");
    }

    @Test
    void cacheEntriesDefaultToThree() {
        assertEquals(3, base.cacheEntries(), "vulpes.base.cache-entries must default to 3");
    }

    @Test
    void cloneTimeoutDefaultsToTwoMinutes() {
        assertEquals(Duration.ofMinutes(2), base.cloneTimeout(), "vulpes.base.clone-timeout must default to 2m");
    }

    @Test
    void buildTimeoutDefaultsToTenMinutes() {
        assertEquals(Duration.ofMinutes(10), base.buildTimeout(), "vulpes.base.build-timeout must default to 10m");
    }

    @Test
    void cacheDirDefaultsToBlankMeaningTheWorkDirectory() {
        assertEquals("", base.cacheDir(), "vulpes.base.cache-dir must be blank when not configured");
    }

    @Test
    void readOnlyBaseCredentialIsNotConfiguredByDefault() {
        assertFalse(credentials.configured(),
                "vulpes.base.maven.username must default to blank, so that the base build receives no repository variable");
    }

    @Test
    void editorRouteIsDisabledByDefault() {
        assertFalse(environment.getProperty("vulpes.release.editor-route-enabled", Boolean.class).orElse(true),
                "vulpes.release.editor-route-enabled must default to false");
    }
}
