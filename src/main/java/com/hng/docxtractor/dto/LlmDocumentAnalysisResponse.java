package com.hng.docxtractor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmDocumentAnalysisResponse {
    private String documentType;
    private String summary;
    private Entities entities;

    @Data
    @Builder
    public static class Entities {
        private String[] names;
        private String[] dates;
        private String[] amounts;
        private String[] emails;
        private String[] phones;
    }
}
