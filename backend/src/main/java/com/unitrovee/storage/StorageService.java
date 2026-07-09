package com.unitrovee.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Contract for file-storage (item images)
 */
public interface StorageService {

    // store a file and return its unique storage key
    String store(MultipartFile file);

    // delete the file identified by the given key
    void delete(String key);

    // build the public url for a stored key
    String getUrl(String key);
}
