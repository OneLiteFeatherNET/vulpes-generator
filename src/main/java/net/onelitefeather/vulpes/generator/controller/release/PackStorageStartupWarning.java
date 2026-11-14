package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Warns once at startup when the pack bucket is not configured. Releases then list {@code "packs": []} and upload no
 * resource pack. The credentials are never logged.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.0.0
 */
@Singleton
public class PackStorageStartupWarning {

    private static final Logger LOG = LoggerFactory.getLogger(PackStorageStartupWarning.class);

    private final String endpoint;
    private final String bucket;

    /**
     * Creates the warning.
     *
     * @param endpoint the configured S3 endpoint, blank when the bucket is not configured
     * @param bucket   the configured bucket name, blank when the bucket is not configured
     */
    @Inject
    public PackStorageStartupWarning(@Value("${vulpes.pack.storage.endpoint:}") String endpoint,
                                     @Value("${vulpes.pack.storage.bucket:}") String bucket) {
        this.endpoint = endpoint;
        this.bucket = bucket;
    }

    /**
     * Logs the warning when a startup happened without a pack bucket.
     *
     * @param event the startup event
     */
    @EventListener
    public void onStartup(StartupEvent event) {
        warnIfNotConfigured();
    }

    /** Logs one WARN line when the endpoint or the bucket is blank. */
    public void warnIfNotConfigured() {
        if (isBlank(endpoint) || isBlank(bucket)) {
            LOG.warn("Resource packs are not uploaded: vulpes.pack.storage.endpoint or vulpes.pack.storage.bucket is not set, "
                    + "so releases list \"packs\": [] until the bucket is configured.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
