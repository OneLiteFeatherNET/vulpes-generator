package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The first {@link ReleaseCompletionListener}: it only logs the completed release.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class LoggingReleaseCompletionListener implements ReleaseCompletionListener {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingReleaseCompletionListener.class);

    @Override
    public void completed(ContentCoordinates coordinates, String revision) {
        LOG.info("Published content release {} from revision {}", coordinates, revision);
    }
}
