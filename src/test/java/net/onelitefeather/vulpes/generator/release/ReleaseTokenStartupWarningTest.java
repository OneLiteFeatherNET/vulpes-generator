package net.onelitefeather.vulpes.generator.release;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.onelitefeather.vulpes.generator.controller.release.ReleaseTokenStartupWarning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The startup warning of a disabled release endpoint, asserted through a captured appender.
 *
 * @author TheMeinerLP
 */
class ReleaseTokenStartupWarningTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(ReleaseTokenStartupWarning.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void blankTokenLogsOneWarningThatReleasesAreDisabled() {
        new ReleaseTokenStartupWarning("").warnIfDisabled();

        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size(), "exactly one startup warning must be logged");
        assertEquals(Level.WARN, events.get(0).getLevel(), "the startup message must be a WARN");
        assertTrue(events.get(0).getFormattedMessage().contains("vulpes.release.token"),
                "the warning must name the setting, was: " + events.get(0).getFormattedMessage());
    }

    @Test
    void configuredTokenLogsNothing() {
        new ReleaseTokenStartupWarning("release-secret-value").warnIfDisabled();

        assertTrue(appender.list.isEmpty(), "a configured token must not produce a warning");
    }

    @Test
    void warningNeverContainsTheToken() {
        new ReleaseTokenStartupWarning("").warnIfDisabled();

        for (ILoggingEvent event : appender.list) {
            assertFalse(event.getFormattedMessage().contains("release-secret-value"),
                    "the token must never be logged");
        }
    }
}
