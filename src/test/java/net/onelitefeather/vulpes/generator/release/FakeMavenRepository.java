package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of the fake Maven repository. Each test resets it in its {@code @BeforeEach}.
 *
 * @author TheMeinerLP
 */
@Singleton
public class FakeMavenRepository {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private volatile String lastAuthorization;

    public void reset() {
        files.clear();
        lastAuthorization = null;
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
