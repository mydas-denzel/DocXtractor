package com.hng.docxtractor.service;

import com.hng.docxtractor.service.LlmService.LlmResult;

public interface LlmService {
    LlmResult analyze(String fileName, String fileType, String extractedText, boolean hasImages, int imageCount);

    class LlmResult {
        public final String documentType;
        public final String summary;
        public final String entitiesJson; // JSON string of structured entities
        public LlmResult(String documentType, String summary, String entitiesJson) {
            this.documentType = documentType;
            this.summary = summary;
            this.entitiesJson = entitiesJson;
        }
    }
}
