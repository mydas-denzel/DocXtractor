package com.hng.docxtractor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentAnalysisResponse {

    private String fileName;
    private String fileType;
    private String contentType; // IMAGE_BASED, TEXT_BASED, MIXED_CONTENT

    private String textExtracted;
    private boolean containsImages;
    private int imageCount;

    private Summary summary;

    @Data
    @Builder
    public static class Summary {
        private boolean isBlank;
        private boolean hasText;
        private boolean hasImages;
    }
}
