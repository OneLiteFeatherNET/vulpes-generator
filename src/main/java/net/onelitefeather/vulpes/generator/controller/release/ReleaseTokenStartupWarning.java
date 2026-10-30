package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Warns once at startup when {@code vulpes.release.token} is blank. In that state {@code POST /releases}
 * refuses every request. The token itself is never logged.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class ReleaseTokenStartupWarning {

    private static final Logger LOG = LoggerFactory.getLogger(ReleaseTokenStartupWarning.class);

    private final String token;

    /**
     * Creates the warning.
     *
     * @param token the configured release token, blank when releases are disabled
     */
    @Inject
    public ReleaseTokenStartupWarning(@Value("${vulpes.release.token:}") String token) {
        this.token = token;
    }

    /**
     * Logs the warning when a startup happened without a release token.
     *
     * @param event the startup event
     */
    @EventListener
    public void onStartup(StartupEvent event) {
        warnIfDisabled();
    }

    /** Logs one WARN line when the release token is blank. */
    public void warnIfDisabled() {
        if (token == null || token.isBlank()) {
            LOG.warn("Content releases are disabled: vulpes.release.token is not set, so POST /releases answers 401 "
                    + "until the token is configured.");
        }
    }
}
