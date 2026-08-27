package com.luvina.base.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import com.luvina.base.core.exception.BusinessException;
import com.luvina.base.core.i18n.ErrorCode;
import com.luvina.base.core.i18n.MessageUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Reads and writes objects in the configured bucket.
 *
 * <p>Deliberately a concrete class, not an interface with one implementation.
 * Both environments talk the S3 API, so there is nothing to abstract over. If a
 * second backend ever appears, extract the interface then.
 *
 * <p>Keys are generated as {@code folder/uuid.ext}. Caller-supplied file names
 * are never used as keys: they collide, and they let a caller steer where the
 * object lands.
 */
@Slf4j
@RequiredArgsConstructor
public class StorageService {

    private static final String KEY_SEPARATOR = "/";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;
    private final MessageUtil messageUtil;

    /**
     * Stores an uploaded file under a generated key.
     *
     * @param file   uploaded file
     * @param folder logical folder, for example {@code samples}
     * @return the storage key, to persist alongside the owning record
     */
    public String upload(MultipartFile file, String folder) {
        String key = buildKey(folder, file.getOriginalFilename());
        try (InputStream stream = file.getInputStream()) {
            put(key, stream, file.getSize(), file.getContentType());
        } catch (IOException ex) {
            throw storageFailure("read uploaded file", key, ex);
        }
        return key;
    }

    /**
     * Stores an in-memory payload, such as a generated report.
     *
     * @param content     payload
     * @param folder      logical folder
     * @param fileName    name used only to derive the extension
     * @param contentType MIME type to record on the object
     * @return the storage key
     */
    public String upload(byte[] content, String folder, String fileName, String contentType) {
        String key = buildKey(folder, fileName);
        put(key, RequestBody.fromBytes(content), contentType);
        return key;
    }

    /**
     * Opens an object for reading.
     *
     * <p>The caller owns the stream and must close it.
     *
     * @param key storage key
     * @return the object content
     */
    public InputStream download(String key) {
        try {
            ResponseInputStream<?> response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
            return response;
        } catch (NoSuchKeyException ex) {
            throw new BusinessException(ErrorCode.ERR012,
                    messageUtil.getMessage(ErrorCode.ERR012), HttpStatus.NOT_FOUND);
        } catch (S3Exception ex) {
            throw storageFailure("download", key, ex);
        }
    }

    /**
     * Returns a time-limited URL that lets a browser download the object without
     * proxying the bytes through this service.
     *
     * @param key storage key
     * @return presigned URL valid for the configured lifetime
     */
    public URL presignedDownloadUrl(String key) {
        return presignedDownloadUrl(key, properties.getPresignedUrlTtl());
    }

    /**
     * Returns a time-limited download URL with an explicit lifetime.
     *
     * @param key storage key
     * @param ttl how long the URL stays valid
     * @return presigned URL
     */
    public URL presignedDownloadUrl(String key, Duration ttl) {
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .build())
                .build()).url();
    }

    /**
     * Deletes an object. Deleting a key that does not exist is not an error.
     *
     * @param key storage key
     */
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (S3Exception ex) {
            throw storageFailure("delete", key, ex);
        }
    }

    /**
     * Tells whether an object exists.
     *
     * @param key storage key
     * @return true when the object is present
     */
    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (S3Exception ex) {
            if (ex.statusCode() == HttpStatus.NOT_FOUND.value()) {
                return false;
            }
            throw storageFailure("stat", key, ex);
        }
    }

    private void put(String key, InputStream stream, long length, String contentType) {
        put(key, RequestBody.fromInputStream(stream, length), contentType);
    }

    private void put(String key, RequestBody body, String contentType) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build(), body);
        } catch (S3Exception ex) {
            throw storageFailure("upload", key, ex);
        }
    }

    /**
     * Builds a collision-free key. Only the extension of the original name is
     * kept, and only when it looks like an extension.
     */
    private String buildKey(String folder, String originalName) {
        String normalisedFolder = folder == null || folder.isBlank()
                ? ""
                : folder.replaceAll("^/+|/+$", "") + KEY_SEPARATOR;
        return normalisedFolder + UUID.randomUUID() + extensionOf(originalName);
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,10}") ? "." + extension : "";
    }

    private BusinessException storageFailure(String operation, String key, Exception cause) {
        log.error("Object storage {} failed for key {}", operation, key, cause);
        return new BusinessException(ErrorCode.ERR015,
                messageUtil.getMessage(ErrorCode.ERR015), HttpStatus.SERVICE_UNAVAILABLE);
    }
}
