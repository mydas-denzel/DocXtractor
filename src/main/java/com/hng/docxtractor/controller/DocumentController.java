package com.hng.docxtractor.controller;

import com.hng.docxtractor.dto.*;
import com.hng.docxtractor.entity.Document;
import com.hng.docxtractor.enums.DocumentStatus;
import com.hng.docxtractor.repo.DocumentRepository;
import com.hng.docxtractor.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository repo;

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
    public ResponseEntity<?> analyze(@PathVariable UUID id) {

        Document doc = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (doc.getStatus() == DocumentStatus.PROCESSING) {
            return ResponseEntity.ok(Map.of(
                    "status", "PROCESSING",
                    "message", "Your document is still being analyzed. Please be patient."
            ));
        }

        if (doc.getStatus() == DocumentStatus.COMPLETED) {

            // FIRST TIME CHECKING COMPLETED
            if (!doc.isViewed()) {
                doc.setViewed(true);
                repo.save(doc);

                return ResponseEntity.ok(Map.of(
                        "status", "COMPLETED",
                        "message", "LLM Analysis has been completed, please head to " +
                                "http://localhost:8080/document/" + id
                ));
            }

            // SECOND TIME (AND AFTER)
            return ResponseEntity.ok(Map.of(
                    "status", "ALREADY_COMPLETED",
                    "message", "This document has already been analyzed. No need to analyze again. You can find the summary here: http://localhost:8080/" + id
            ));
        }

        // PENDING or FAILED → Start analysis
        documentService.runAnalysisAsync(doc);

        return ResponseEntity.accepted().body(Map.of(
                "status", "STARTED",
                "message", "Your document analysis has started. You will be notified when complete."
        ));
    }

    /**
     * Get combined document
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailsDto> get(@PathVariable("id") UUID id) {
        DocumentDetailsDto dto = documentService.getDocument(id);
        return ResponseEntity.ok(dto);
    }
}
