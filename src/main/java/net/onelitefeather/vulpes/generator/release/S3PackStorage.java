package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.PACK_STORAGE_UNAVAILABLE;

/**
 * The pack bucket on an S3-compatible service, for example Ceph RGW. The client uses path-style addressing. Objects
 * are written without an ACL: the bucket policy makes them publicly readable. The bean exists only when the endpoint
 * and the bucket are configured.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.0.0
 */
@Singleton
@Requires(property = "vulpes.pack.storage.endpoint", notEquals = "")
@Requires(property = "vulpes.pack.storage.bucket", notEquals = "")
public class S3PackStorage implements PackStorage {

    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client client;
    private final String bucket;
    private final String publicBase;

    /**
     * Creates the storage. The credentials are the configured key pair, or the default AWS credential chain when the
     * pair is blank.
     *
     * @param endpoint  the S3 endpoint, for example {@code http://ceph-rgw:7480}
     * @param bucket    the name of the bucket
     * @param region    the region sent to the endpoint
     * @param publicUrl the public base URL of the objects, without the key
     * @param accessKey the access key, may be blank
     * @param secretKey the secret key, may be blank
     * @throws IllegalStateException when the public URL is blank
     */
    @Inject
    public S3PackStorage(@Value("${vulpes.pack.storage.endpoint:}") String endpoint,
                         @Value("${vulpes.pack.storage.bucket:}") String bucket,
                         @Value("${vulpes.pack.storage.region:us-east-1}") String region,
                         @Value("${vulpes.pack.storage.public-url:}") String publicUrl,
                         @Value("${vulpes.pack.storage.access-key:}") String accessKey,
                         @Value("${vulpes.pack.storage.secret-key:}") String secretKey) {
        if (publicUrl == null || publicUrl.isBlank()) {
            throw new IllegalStateException("vulpes.pack.storage.public-url must be set when the pack bucket is configured");
        }
        this.bucket = bucket;
        this.publicBase = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        AwsCredentialsProvider credentials = accessKey.isBlank() || secretKey.isBlank()
                ? DefaultCredentialsProvider.builder().build()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        this.client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override
    public String url(String key) {
        return publicBase + "/" + key;
    }

    @Override
    public boolean exists(String key) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw unavailable(key, e);
        } catch (SdkException e) {
            throw unavailable(key, e);
        }
    }

    @Override
    public void put(String key, byte[] bytes) {
        try {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/zip")
                            .cacheControl(CACHE_CONTROL)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (SdkException e) {
            throw unavailable(key, e);
        }
    }

    private static ReleaseRejectedException unavailable(String key, SdkException e) {
        return new ReleaseRejectedException(PACK_STORAGE_UNAVAILABLE,
                "the pack bucket cannot be used for " + key + ": " + e.getMessage());
    }
}
