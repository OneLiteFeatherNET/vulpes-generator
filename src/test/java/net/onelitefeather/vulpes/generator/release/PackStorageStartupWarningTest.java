package net.onelitefeather.vulpes.generator.release;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.onelitefeather.vulpes.generator.controller.release.PackStorageStartupWarning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The startup warning of a missing pack bucket, asserted through a captured appender.
 *
 * @author TheMeinerLP
 */
class PackStorageStartupWarningTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(PackStorageStartupWarning.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void blankEndpointLogsOneWarningNamingTheSettings() {
        new PackStorageStartupWarning("", "cygnus-packs").warnIfNotConfigured();

        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size(), "exactly one startup warning must be logged");
        assertEquals(Level.WARN, events.get(0).getLevel(), "the startup message must be a WARN");
        assertTrue(events.get(0).getFormattedMessage().contains("vulpes.pack.storage"),
                "the warning must name the setting, was: " + events.get(0).getFormattedMessage());
    }

    @Test
    void blankBucketLogsOneWarning() {
        new PackStorageStartupWarning("http://ceph.test", " ").warnIfNotConfigured();

        assertEquals(1, appender.list.size(), "a blank bucket must produce exactly one warning");
    }

    @Test
    void configuredBucketLogsNothing() {
        new PackStorageStartupWarning("http://ceph.test", "cygnus-packs").warnIfNotConfigured();

        assertTrue(appender.list.isEmpty(), "a configured bucket must not produce a warning");
    }
}
