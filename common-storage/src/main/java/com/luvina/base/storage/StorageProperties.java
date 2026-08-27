package com.luvina.base.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Object storage settings.
 *
 * <p>There is a single backend, the S3 API. MinIO speaks it in development and
 * AWS S3 speaks it in production, so both environments run the same code path.
 * Only {@link #endpoint} differs.
 *
 * <p>Credentials must come from the environment, never from a committed file.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * Full endpoint URI of an S3-compatible service, for example
     * {@code http://localhost:9000} for MinIO. Leave empty to use the real AWS
     * endpoint for {@link #region}.
     *
     * <p>Give the complete URI including the scheme. MinIO in a compose network
     * is usually plain HTTP, so a hardcoded {@code https://} prefix breaks it.
     */
    private String endpoint = "";

    /** AWS region. Any value works for MinIO, but it must not be empty. */
    @NotBlank
    private String region = "us-east-1";

    /** Bucket that holds every object written by this service. */
    @NotBlank
    private String bucket;

    /** Access key. Supply through the environment. */
    private String accessKey = "";

    /** Secret key. Supply through the environment. */
    private String secretKey = "";

    /**
     * Whether to address the bucket as a path segment rather than a subdomain.
     * MinIO needs {@code true}; AWS accepts it too.
     */
    private boolean pathStyleAccess = true;

    /** Lifetime of generated presigned download URLs. */
    private Duration presignedUrlTtl = Duration.ofHours(1);

    /**
     * Tells whether an explicit endpoint was configured.
     *
     * @return true when the endpoint must override the AWS default
     */
    public boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    /**
     * Tells whether static credentials were configured.
     *
     * @return true when access key and secret key are both present
     */
    public boolean hasStaticCredentials() {
        return !accessKey.isBlank() && !secretKey.isBlank();
    }
}
