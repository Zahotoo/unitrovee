package com.unitrovee.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * local-disk implementation of StorageService (MVP)
 */
@Service
@Profile("local")
public class LocalStorageService implements StorageService {

    private final Path root;            // directory where files are written
    private final String baseUrl;       // prefix used to build public url

    public LocalStorageService(
            @Value("${storage.local.directory:uploads}") String directory,
            @Value("${storage.local.base-url:http://localhost:8080/files}") String baseUrl
    ) {
        this.root = Path.of(directory);
        this.baseUrl = baseUrl;
    }

    @Override
    public String store(MultipartFile file) {
        try {
            Files.createDirectories(root);      // ensure the target folder exists
            // generate our own random key: only reuse the extension.
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String key = UUID.randomUUID() + (ext != null ? "." + ext : "");
            Files.copy(file.getInputStream(), root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(root.resolve(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + key, e);
        }
    }

    @Override
    public String getUrl(String key) {
        return baseUrl + "/" + key;
    }
}
