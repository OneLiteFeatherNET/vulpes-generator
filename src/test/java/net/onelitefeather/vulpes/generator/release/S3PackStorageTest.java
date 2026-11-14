package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the parts of the S3 pack storage that need no network: the public URL of an object, the configuration
 * checks and the upload request, with and without a canned ACL. The client is built but never called.
 *
 * @author TheMeinerLP
 */
class S3PackStorageTest {

    @Test
    void publicUrlIsThePublicBaseFollowedBySlashAndKey() {
        S3PackStorage storage = new S3PackStorage("http://ceph.test:7480", "cygnus-packs", "us-east-1",
                "https://packs.test/", "", "", "");

        assertEquals("https://packs.test/vulpes/cygnus/abc.zip", storage.url("vulpes/cygnus/abc.zip"),
                "the URL must be the public base URL, one slash and the key, without a doubled slash");
    }

    @Test
    void configuredBucketWithoutPublicUrlIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> new S3PackStorage("http://ceph.test:7480", "cygnus-packs", "us-east-1", "", "", "", ""),
                "a bucket without a public URL would write URLs that no client can open");
    }

    @Test
    void uploadRequestCarriesNoAclWhenTheAclIsBlank() {
        PutObjectRequest request = S3PackStorage.putRequest("cygnus-packs", "vulpes/cygnus/abc.zip", "");

        assertNull(request.acl(), "a blank ACL setting must send no canned ACL header");
        assertEquals("cygnus-packs", request.bucket(), "the upload must go to the configured bucket");
        assertEquals("vulpes/cygnus/abc.zip", request.key(), "the upload must use the content key");
    }

    @Test
    void uploadRequestCarriesTheConfiguredCannedAcl() {
        PutObjectRequest request = S3PackStorage.putRequest("cygnus-packs", "vulpes/cygnus/abc.zip", "public-read");

        assertEquals(ObjectCannedACL.PUBLIC_READ, request.acl(),
                "the configured canned ACL must be sent on the upload");
    }
}
