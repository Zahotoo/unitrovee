package com.unitrovee.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private static final String BASE_URL = "http://localhost:8080/files";

    @Test
    void store_writesFileToDiskAndReturnsRetrievableKeyAndUrl() {
        // use the temp dir as the storage root
        LocalStorageService storage = new LocalStorageService(tempDir.toString(), BASE_URL);
        MultipartFile file = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-byte".getBytes()
        );
        String key = storage.store(file);

        assertThat(key).endsWith(".png");                                   // extension preserved
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();            // bytes actually landed on disk
        assertThat(storage.getUrl(key)).isEqualTo(BASE_URL + "/" + key);
    }

    @Test
    void delete_removesTheStoredFile() {
        LocalStorageService storage = new LocalStorageService(tempDir.toString(), BASE_URL);
        MultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", "x".getBytes());
        String key = storage.store(file);
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();

        storage.delete(key);

        assertThat(Files.exists(tempDir.resolve(key))).isFalse();       // it should be gone after deletion

    }
}
