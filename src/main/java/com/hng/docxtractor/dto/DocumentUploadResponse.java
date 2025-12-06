package com.hng.docxtractor.dto;
import lombok.*;

@Data @Builder
public class DocumentUploadResponse {
    private Long id;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private boolean containsImages;
    private int imageCount;
    private String message;
}
