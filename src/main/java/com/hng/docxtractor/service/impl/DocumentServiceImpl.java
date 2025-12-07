package com.hng.docxtractor.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.docxtractor.dto.*;
import com.hng.docxtractor.entity.Document;
import com.hng.docxtractor.enums.DocumentStatus;
import com.hng.docxtractor.exception.ApiException;
import com.hng.docxtractor.repo.DocumentRepository;
import com.hng.docxtractor.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements com.hng.docxtractor.service.DocumentService {

    private final StorageService storageService;
    private final TextExtractionService textExtractionService;
    private final LlmService llmService;
    private final DocumentRepository docRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.upload.max-bytes}")
    private long maxBytes;

    @Value("${storage.bucket}")
    private String storageBucket;

    @Override
    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        try {
            if (file == null || file.isEmpty())
                throw new ApiException("No file provided");

            // Validate MIME type / extension
            String contentType = file.getContentType();
            String extension = FilenameUtils.getExtension(
                    file.getOriginalFilename() == null ? "" : file.getOriginalFilename()
            ).toLowerCase();

            Set<String> allowedMime = Set.of(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword"
            );

            Set<String> allowedExt = Set.of("pdf", "docx", "doc");

            if (!allowedMime.contains(contentType) && !allowedExt.contains(extension))
                throw new ApiException("Unsupported file type. Only PDF/DOC/DOCX allowed.");

            if (file.getSize() > maxBytes)
                throw new ApiException("File exceeds max size of " + maxBytes + " bytes");

            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();

            // -------- UUID primary key but ALSO used for filename --------
            UUID id = UUID.randomUUID(); // Will become PK

            // Stored filename → UUID.ext
            String storedName = id + "." + extension;

            // Upload actual file
            String storagePath = storageService.upload(storageBucket, storedName, file);

            // Extract text
            TextExtractionService.TextExtractionResult extraction = textExtractionService.extractText(file);

            // Persist
            Document doc = Document.builder()
                    .id(id)                               // PK
                    .originalFileName(original)
                    .storagePath(storagePath)
                    .contentType(contentType)
                    .sizeBytes(file.getSize())
                    .extractedText(extraction.text)
                    .containsImages(extraction.containsImages)
                    .imageCount(extraction.imageCount)
                    .analyzed(false)
                    .status(DocumentStatus.PENDING)       // <-- ADD THIS
                    //.summary(null)                        // <-- optional, but safe
                    .build();


            docRepo.save(doc);

            return DocumentUploadResponse.builder()
                    .id(doc.getId())                      // UUID
                    .fileName(doc.getOriginalFileName())
                    .contentType(doc.getContentType())
                    .sizeBytes(doc.getSizeBytes())
                    .containsImages(doc.isContainsImages())
                    .imageCount(doc.getImageCount())
                    .message("Uploaded and text extracted; call /documents/{id}/analyze to run LLM")
                    .build();

        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException("Upload failed: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional
    public DocumentUploadResponse analyzeDocument(UUID id) {
        Document doc = docRepo.findById(id)
                .orElseThrow(() -> new ApiException("Document not found: " + id));

        // --- Helper inline check ---
        boolean invalidAnalysis = doc.getSummary() == null || doc.getSummary().trim().isEmpty();

        switch (doc.getStatus()) {

            case PROCESSING:
                return DocumentUploadResponse.builder()
                        .id(doc.getId())
                        .fileName(doc.getOriginalFileName())
                        .message("Your document is still being analyzed. Please be patient.")
                        .build();

            case COMPLETED:
                // If summary/metadata missing → treat as failed
                if (invalidAnalysis) {
                    doc.setStatus(DocumentStatus.FAILED);
                    docRepo.save(doc);
                    return DocumentUploadResponse.builder()
                            .id(doc.getId())
                            .fileName(doc.getOriginalFileName())
                            .message("Previous analysis failed. Please retry.")
                            .build();
                }

                return DocumentUploadResponse.builder()
                        .id(doc.getId())
                        .fileName(doc.getOriginalFileName())
                        .contentType(doc.getContentType())
                        .sizeBytes(doc.getSizeBytes())
                        .message("Analysis already completed.")
                        .build();

            case FAILED:
            case PENDING:
                runAnalysisAsync(doc); // starts async job
                return DocumentUploadResponse.builder()
                        .id(doc.getId())
                        .fileName(doc.getOriginalFileName())
                        .message("Analysis started. Check again later.")
                        .build();

            default:
                throw new ApiException("Unknown status");
        }
    }


    @Override
    public DocumentDetailsDto getDocument(UUID id) {
        Document d = docRepo.findById(id)
                .orElseThrow(() -> new ApiException("Document not found: " + id));

        return DocumentDetailsDto.builder()
                .id(d.getId())
                .fileName(d.getOriginalFileName())
                .contentType(d.getContentType())
                .sizeBytes(d.getSizeBytes())
                .storagePath(d.getStoragePath())
                .extractedText(d.getExtractedText())
                .containsImages(d.isContainsImages())
                .imageCount(d.getImageCount())
                .analyzed(d.isAnalyzed())
                .documentType(d.getDocumentType())
                .summary(d.getSummary())
                .metadataJson(d.getMetadataJson())
                .build();
    }

    @Async
    public void runAnalysisAsync(Document doc) {
        try {
            doc.setStatus(DocumentStatus.PROCESSING);
            docRepo.save(doc);

            // Call your existing LLM service
            LlmService.LlmResult res = llmService.analyze(
                    doc.getOriginalFileName(),
                    doc.getContentType(),
                    doc.getExtractedText(),
                    doc.isContainsImages(),
                    doc.getImageCount()
            );

            doc.setDocumentType(res.documentType);
            doc.setSummary(res.summary);
            doc.setMetadataJson(res.entitiesJson);
            doc.setAnalyzed(true);
            doc.setStatus(DocumentStatus.COMPLETED);

        } catch (Exception e) {
            doc.setStatus(DocumentStatus.FAILED);
        }

        docRepo.save(doc);
    }

}
