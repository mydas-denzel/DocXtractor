package com.hng.docxtractor.service.impl;

import com.hng.docxtractor.service.StorageService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {
    private final MinioClient minioClient;

    @Value("${storage.bucket}")
    private String defaultBucket;

    @Override
    public String upload(String bucket, String objectName, MultipartFile file) throws Exception {
        String targetBucket = bucket == null || bucket.isBlank() ? defaultBucket : bucket;

        // create bucket if not exists
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(targetBucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(targetBucket).build());
        }

        try (InputStream is = file.getInputStream()) {
            PutObjectArgs putArgs = PutObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            minioClient.putObject(putArgs);
        }
        // return a predictable storage path
        return String.format("%s/%s", targetBucket, objectName);
    }
}
