package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.release.ReleaseClockFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Replaces the system clock of every test context with {@link ReleaseTestData#CREATED_AT}, so that the
 * {@code createdAt} of releases started from the editor is repeatable.
 *
 * @author TheMeinerLP
 */
@Factory
class FixedClockFactory {

    @Singleton
    @Replaces(factory = ReleaseClockFactory.class)
    Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);
    }
}
