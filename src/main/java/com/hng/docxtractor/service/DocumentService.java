package com.hng.docxtractor.service;

import com.hng.docxtractor.dto.DocumentUploadResponse;
import com.hng.docxtractor.dto.DocumentDetailsDto;
import com.hng.docxtractor.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentService {
    DocumentUploadResponse uploadDocument(MultipartFile file);
    DocumentUploadResponse analyzeDocument(UUID id);
    DocumentDetailsDto getDocument(UUID id);

    void runAnalysisAsync(Document doc);
}
