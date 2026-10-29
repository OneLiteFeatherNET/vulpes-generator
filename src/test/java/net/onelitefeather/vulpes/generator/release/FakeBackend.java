package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State of the fake backend served by the embedded test server. Each test resets it in its {@code @BeforeEach}.
 *
 * @author TheMeinerLP
 */
@Singleton
public class FakeBackend {

    private final Map<UUID, byte[]> snapshots = new ConcurrentHashMap<>();
    private volatile boolean down;
    private volatile String lastAuthorization;
    private final AtomicInteger requests = new AtomicInteger();

    public void reset() {
        snapshots.clear();
        down = false;
        lastAuthorization = null;
        requests.set(0);
    }

    public void register(UUID projectId, byte[] snapshot) {
        snapshots.put(projectId, snapshot);
    }

    /** Makes the backend answer 503 for every request, as if it were down. */
    public void setDown(boolean down) {
        this.down = down;
    }

    public boolean isDown() {
        return down;
    }

    public byte[] snapshot(UUID projectId) {
        return snapshots.get(projectId);
    }

    public String lastAuthorization() {
        return lastAuthorization;
    }

    void recordAuthorization(String authorization) {
        lastAuthorization = authorization;
        requests.incrementAndGet();
    }

    /** Returns how many snapshot requests reached the backend since the last reset. */
    public int requestCount() {
        return requests.get();
    }
}
