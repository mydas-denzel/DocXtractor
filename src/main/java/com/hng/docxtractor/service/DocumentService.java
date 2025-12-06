package com.hng.docxtractor.service;

import com.hng.docxtractor.dto.DocumentUploadResponse;
import com.hng.docxtractor.dto.DocumentDetailsDto;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    DocumentUploadResponse uploadDocument(MultipartFile file);
    DocumentUploadResponse analyzeDocument(Long id);
    DocumentDetailsDto getDocument(Long id);
}
