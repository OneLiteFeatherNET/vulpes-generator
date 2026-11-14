package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the parts of the S3 pack storage that need no network: the public URL of an object and the configuration
 * checks. The client is built but never called.
 *
 * @author TheMeinerLP
 */
class S3PackStorageTest {

    @Test
    void publicUrlIsThePublicBaseFollowedBySlashAndKey() {
        S3PackStorage storage = new S3PackStorage("http://ceph.test:7480", "cygnus-packs", "us-east-1",
                "https://packs.test/", "", "");

        assertEquals("https://packs.test/vulpes/cygnus/abc.zip", storage.url("vulpes/cygnus/abc.zip"),
                "the URL must be the public base URL, one slash and the key, without a doubled slash");
    }

    @Test
    void configuredBucketWithoutPublicUrlIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> new S3PackStorage("http://ceph.test:7480", "cygnus-packs", "us-east-1", "", "", ""),
                "a bucket without a public URL would write URLs that no client can open");
    }
}
