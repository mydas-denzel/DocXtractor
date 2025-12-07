package com.hng.docxtractor.dto;
import lombok.*;

import java.util.UUID;

@Data @Builder
public class DocumentUploadResponse {
    private UUID id;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private boolean containsImages;
    private int imageCount;
    private String message;
}
