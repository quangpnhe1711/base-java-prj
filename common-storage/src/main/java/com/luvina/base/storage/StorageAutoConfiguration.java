package com.luvina.base.storage;

import java.net.URI;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.luvina.base.core.i18n.MessageUtil;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Wires the S3 client, the presigner and {@link StorageService}.
 *
 * <p>Active only when {@code app.storage.bucket} is set, so a service that does
 * not store files pays nothing for having the module on the classpath.
 *
 * <p>Credentials fall back to the AWS default provider chain when none are
 * configured, which is what lets a production deployment use an instance role
 * instead of static keys.
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(prefix = "app.storage", name = "bucket")
public class StorageAutoConfiguration {

    /**
     * Builds the synchronous S3 client.
     *
     * @param properties storage settings
     * @return configured client
     */
    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client(StorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .serviceConfiguration(pathStyle(properties));
        if (properties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }

    /**
     * Builds the presigner used for time-limited download URLs.
     *
     * @param properties storage settings
     * @return configured presigner
     */
    @Bean
    @ConditionalOnMissingBean
    public S3Presigner s3Presigner(StorageProperties properties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .serviceConfiguration(pathStyle(properties));
        if (properties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }

    /**
     * Builds the storage facade.
     *
     * @param s3Client    S3 client
     * @param s3Presigner S3 presigner
     * @param properties  storage settings
     * @param messageUtil message resolver used for error messages
     * @return storage service
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageService storageService(S3Client s3Client, S3Presigner s3Presigner,
                                         StorageProperties properties, MessageUtil messageUtil) {
        return new StorageService(s3Client, s3Presigner, properties, messageUtil);
    }

    private S3Configuration pathStyle(StorageProperties properties) {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .build();
    }

    private AwsCredentialsProvider credentialsProvider(StorageProperties properties) {
        if (properties.hasStaticCredentials()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
