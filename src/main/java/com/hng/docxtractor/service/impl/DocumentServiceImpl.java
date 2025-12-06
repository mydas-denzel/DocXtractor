package com.hng.docxtractor.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.docxtractor.dto.*;
import com.hng.docxtractor.entity.Document;
import com.hng.docxtractor.exception.ApiException;
import com.hng.docxtractor.repository.DocumentRepository;
import com.hng.docxtractor.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
            if (file == null || file.isEmpty()) throw new ApiException("No file provided");
            if (file.getSize() > maxBytes) throw new ApiException("File exceeds max size of " + maxBytes + " bytes");
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();

            // simple filename sanitization
            String base = FilenameUtils.getBaseName(original).replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            String ext = FilenameUtils.getExtension(original);
            String safeName = base + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0,6) + (ext.isBlank()? "" : ("." + ext));

            // store file first
            String storagePath = storageService.upload(storageBucket, safeName, file);

            // extract text + metadata
            TextExtractionService.TextExtractionResult extraction = textExtractionService.extractText(file);

            // persist Document record BEFORE calling LLM
            Document doc = Document.builder()
                    .originalFileName(original)
                    .storagePath(storagePath)
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .extractedText(extraction.text)
                    .containsImages(extraction.containsImages)
                    .imageCount(extraction.imageCount)
                    .analyzed(false)
                    .build();
            doc = docRepo.save(doc);

            return DocumentUploadResponse.builder()
                    .id(doc.getId())
                    .fileName(doc.getOriginalFileName())
                    .contentType(doc.getContentType())
                    .sizeBytes(doc.getSizeBytes())
                    .containsImages(doc.isContainsImages())
                    .imageCount(doc.getImageCount())
                    .message("Uploaded and text extracted; call /documents/{id}/analyze to run LLM")
                    .build();

        } catch (ApiException ae) { throw ae; }
        catch (Exception e) { throw new ApiException("Upload failed: " + e.getMessage(), e); }
    }

    @Override
    @Transactional
    public DocumentUploadResponse analyzeDocument(Long id) {
        Document doc = docRepo.findById(id).orElseThrow(() -> new ApiException("Document not found: " + id));
        if (doc.isAnalyzed()) {
            return DocumentUploadResponse.builder()
                    .id(doc.getId())
                    .fileName(doc.getOriginalFileName())
                    .contentType(doc.getContentType())
                    .sizeBytes(doc.getSizeBytes())
                    .containsImages(doc.isContainsImages())
                    .imageCount(doc.getImageCount())
                    .message("Already analyzed")
                    .build();
        }

        // call LLM
        LlmService.LlmResult res = llmService.analyze(doc.getOriginalFileName(), doc.getContentType(),
                doc.getExtractedText(), doc.isContainsImages(), doc.getImageCount());

        // save results
        doc.setDocumentType(res.documentType);
        doc.setSummary(res.summary);
        doc.setMetadataJson(res.entitiesJson);
        doc.setAnalyzed(true);
        docRepo.save(doc);

        return DocumentUploadResponse.builder()
                .id(doc.getId())
                .fileName(doc.getOriginalFileName())
                .contentType(doc.getContentType())
                .sizeBytes(doc.getSizeBytes())
                .containsImages(doc.isContainsImages())
                .imageCount(doc.getImageCount())
                .message("LLM analysis complete")
                .build();
    }

    @Override
    public DocumentDetailsDto getDocument(Long id) {
        Document d = docRepo.findById(id).orElseThrow(() -> new ApiException("Document not found: " + id));
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
}
