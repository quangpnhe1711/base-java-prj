package com.luvina.base.storage;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.luvina.base.core.i18n.MessageUtil;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Tests the part of the storage service that is our own logic rather than the
 * AWS SDK: how a storage key is derived from the caller input.
 *
 * <p>Key generation is worth pinning down because it is a security boundary. A
 * caller-supplied file name must not be able to steer where the object lands or
 * to collide with an existing object.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StorageService")
class StorageServiceTest {

    private static final String BUCKET = "test-bucket";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private MessageUtil messageUtil;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setBucket(BUCKET);
        storageService = new StorageService(s3Client, s3Presigner, properties, messageUtil);
    }

    @Test
    @DisplayName("generates a random key that keeps only the extension")
    void generatesRandomKey() {
        String key = upload("report.XLSX", "exports");

        assertThat(key).startsWith("exports/").endsWith(".xlsx");
        // The original name must not survive into the key.
        assertThat(key).doesNotContain("report");
        assertThat(capturedRequest().bucket()).isEqualTo(BUCKET);
    }

    @Test
    @DisplayName("gives two uploads of the same name different keys")
    void avoidsCollisions() {
        assertThat(upload("same.pdf", "docs")).isNotEqualTo(upload("same.pdf", "docs"));
    }

    @Test
    @DisplayName("strips a path out of the caller supplied name")
    void ignoresPathInFileName() {
        String key = upload("../../etc/passwd", "docs");

        assertThat(key).startsWith("docs/");
        assertThat(key).doesNotContain("..").doesNotContain("passwd");
    }

    @Test
    @DisplayName("normalises stray slashes around the folder")
    void normalisesFolder() {
        assertThat(upload("a.png", "/images/")).startsWith("images/").doesNotContain("//");
    }

    @Test
    @DisplayName("drops an implausible extension instead of trusting it")
    void dropsImplausibleExtension() {
        assertThat(upload("archive.thisisnotanextension", "docs")).doesNotContain(".thisisnot");
    }

    private String upload(String fileName, String folder) {
        MockMultipartFile file = new MockMultipartFile(
                "file", fileName, "application/octet-stream", "content".getBytes(StandardCharsets.UTF_8));
        return storageService.upload(file, folder);
    }

    private PutObjectRequest capturedRequest() {
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        return captor.getValue();
    }
}
