package net.onelitefeather.vulpes.generator.release.base;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock that moves only when the test moves it, so that the order of cache uses is fixed.
 *
 * @author TheMeinerLP
 */
final class AdvancingClock extends Clock {

    private Instant now = Instant.parse("2026-11-07T12:00:00Z");

    /**
     * Moves the clock forward.
     *
     * @param amount the distance to move
     */
    void advance(Duration amount) {
        now = now.plus(amount);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
