package com.hng.docxtractor.service;

import com.hng.docxtractor.dto.DocumentAnalysisResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentAnalysisService {
    DocumentAnalysisResponse analyze(MultipartFile file);
}
