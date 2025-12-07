package com.hng.docxtractor.dto;
import lombok.*;

import java.util.UUID;

@Data @Builder
public class DocumentDetailsDto {
    private UUID id;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String storagePath;
    private String extractedText;
    private boolean containsImages;
    private int imageCount;
    private boolean analyzed;
    private String documentType;
    private String summary;
    private String metadataJson;
}
