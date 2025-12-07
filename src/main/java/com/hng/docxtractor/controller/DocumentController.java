package com.hng.docxtractor.controller;

import com.hng.docxtractor.dto.*;
import com.hng.docxtractor.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload endpoint: POST /upload
     * Accepts multipart/form-data "file"
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        DocumentUploadResponse res = documentService.uploadDocument(file);
        return ResponseEntity.ok(res);
    }

    /**
     * Analyze: POST /documents/{id}/analyze
     */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<DocumentUploadResponse> analyze(@PathVariable("id") Long id) {
        DocumentUploadResponse res = documentService.analyzeDocument(id);
        return ResponseEntity.ok(res);
    }

    /**
     * Get combined document
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailsDto> get(@PathVariable("id") Long id) {
        DocumentDetailsDto dto = documentService.getDocument(id);
        return ResponseEntity.ok(dto);
    }
}
