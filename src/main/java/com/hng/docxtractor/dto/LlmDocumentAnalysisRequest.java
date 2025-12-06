package com.hng.docxtractor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmDocumentAnalysisRequest {
    private String fileName;
    private String extractedText;
    private boolean hasImages;
    private int imageCount;
    private String fileType;
}
