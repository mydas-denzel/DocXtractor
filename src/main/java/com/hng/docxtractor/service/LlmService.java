package com.hng.docxtractor.service;

import com.hng.docxtractor.dto.LlmDocumentAnalysisRequest;
import com.hng.docxtractor.dto.LlmDocumentAnalysisResponse;

public interface LlmService {
    LlmDocumentAnalysisResponse analyzeDocument(LlmDocumentAnalysisRequest request);
}
