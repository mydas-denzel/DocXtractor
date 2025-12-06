package com.hng.docxtractor.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * Upload file and return storage path (bucket/object) or URL.
     */
    String upload(String bucket, String objectName, MultipartFile file) throws Exception;
}
