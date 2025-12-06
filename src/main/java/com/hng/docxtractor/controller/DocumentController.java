package com.hng.docxtractor.controller;

import com.hng.docxtractor.dto.DocumentAnalysisResponse;
import com.hng.docxtractor.service.DocumentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentAnalysisService documentAnalysisService;

    @Operation(
            summary = "Upload and analyze a document",
            description = "Accepts PDF, DOCX, or image files and extracts text + detects images"
    )
    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentAnalysisResponse> analyzeDocument(
            @Parameter(description = "PDF, DOCX, JPG, PNG")
            @RequestPart("file") MultipartFile file
    ) {
        DocumentAnalysisResponse response = documentAnalysisService.analyze(file);
        return ResponseEntity.ok(response);
    }
}
