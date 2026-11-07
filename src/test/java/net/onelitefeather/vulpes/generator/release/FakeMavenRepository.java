package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory store of the fake Maven repository. Each test resets it in its {@code @BeforeEach}.
 *
 * @author TheMeinerLP
 */
@Singleton
public class FakeMavenRepository {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final Map<String, Integer> failures = new ConcurrentHashMap<>();
    private final AtomicInteger requests = new AtomicInteger();
    private volatile String lastAuthorization;

    public void reset() {
        files.clear();
        failures.clear();
        requests.set(0);
        lastAuthorization = null;
    }

    /** Makes every request below the path prefix answer with the status, as a broken repository does. */
    public void failUnder(String pathPrefix, int status) {
        failures.put(pathPrefix, status);
    }

    /** The status a request for the path must answer with instead of the stored file, if one was set. */
    Optional<Integer> failureFor(String path) {
        return failures.entrySet().stream()
                .filter(failure -> path.startsWith(failure.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /** The number of requests the repository has received since the last reset. */
    public int requestCount() {
        return requests.get();
    }

    void countRequest() {
        requests.incrementAndGet();
    }

    public void store(String path, byte[] content) {
        files.put(path, content);
    }

    public boolean contains(String path) {
        return files.containsKey(path);
    }

    public byte[] get(String path) {
        return files.get(path);
    }

    public Set<String> keys() {
        return files.keySet();
    }

    public String lastAuthorization() {
        return lastAuthorization;
    }

    void recordAuthorization(String authorization) {
        lastAuthorization = authorization;
    }
}
