package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.time.Clock;

/**
 * Provides the clock that stamps the releases started from the editor. Tests replace it with a fixed clock.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Factory
public class ReleaseClockFactory {

    /**
     * Returns the system clock in UTC.
     *
     * @return the clock
     */
    @Singleton
    public Clock clock() {
        return Clock.systemUTC();
    }
}
