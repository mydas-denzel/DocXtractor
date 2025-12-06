package com.hng.docxtractor.service.impl;

import com.hng.docxtractor.dto.DocumentAnalysisResponse;
import com.hng.docxtractor.dto.DocumentAnalysisResponse.Summary;
import com.hng.docxtractor.ocr.OcrService;
import com.hng.docxtractor.service.DocxService;
import com.hng.docxtractor.service.DocumentAnalysisService;
import com.hng.docxtractor.service.ImageService;
import com.hng.docxtractor.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class DocumentAnalysisServiceImpl implements DocumentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisServiceImpl.class);

    private final PdfService pdfService;
    private final DocxService docxService;
    private final ImageService imageService;
    private final OcrService ocrService;
    private final Tika tika = new Tika();

    private static final int TEXT_THRESHOLD = 20;

    @Override
    public DocumentAnalysisResponse analyze(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) originalFilename = "unknown";

            String detectedContentType;
            try (InputStream is = file.getInputStream()) {
                detectedContentType = tika.detect(is, originalFilename);
            }

            DocumentAnalysisResponse.DocumentAnalysisResponseBuilder builder =
                    DocumentAnalysisResponse.builder()
                            .fileName(originalFilename)
                            .fileType(detectedContentType);

            // If file is an image (png/jpg/jpeg), OCR it and return
            if (detectedContentType != null && detectedContentType.startsWith("image/")) {
                builder.contentType("IMAGE_BASED");
                String ocr = imageService.extractTextFromImage(file.getInputStream());
                int imageCount = 1;
                builder.textExtracted(ocr == null ? "" : ocr)
                        .containsImages(true)
                        .imageCount(imageCount)
                        .summary(buildSummary(ocr, true));
                return builder.build();
            }

            // PDF handling
            if (originalFilename.toLowerCase().endsWith(".pdf") || "application/pdf".equals(detectedContentType)) {
                try (InputStream is1 = file.getInputStream(); InputStream is2 = file.getInputStream()) {
                    String text = pdfService.extractText(is1);
                    int imageCount = pdfService.countImages(is2);
                    boolean hasImages = imageCount > 0;

                    boolean textOk = text != null && text.trim().length() > TEXT_THRESHOLD;
                    String finalText = text == null ? "" : text;

                    if (!textOk && hasImages) {
                        // run OCR on each page and append
                        try (InputStream is3 = file.getInputStream()) {
                            String ocrText = pdfService.ocrPdfPages(is3);
                            if (ocrText != null && !ocrText.isBlank()) {
                                finalText = (finalText + "\n" + ocrText).trim();
                            }
                        }
                    }

                    String contentType = determineContentType(finalText, hasImages);
                    builder.contentType(contentType)
                            .textExtracted(finalText)
                            .containsImages(hasImages)
                            .imageCount(imageCount)
                            .summary(buildSummary(finalText, hasImages));
                    return builder.build();
                }
            }

            // DOCX handling
            if (originalFilename.toLowerCase().endsWith(".docx") || detectedContentType.contains("officedocument")) {
                try (InputStream is = file.getInputStream()) {
                    String text = docxService.extractText(is);
                    boolean hasImages = docxService.hasImages(file.getInputStream());
                    boolean textOk = text != null && text.trim().length() > TEXT_THRESHOLD;
                    String contentType = determineContentType(text, hasImages);
                    builder.contentType(contentType)
                            .textExtracted(text == null ? "" : text)
                            .containsImages(hasImages)
                            .imageCount(hasImages ? docxService.countImages(file.getInputStream()) : 0)
                            .summary(buildSummary(text, hasImages));
                    return builder.build();
                }
            }

            // fallback: unknown type
            builder.contentType("UNKNOWN")
                    .textExtracted("")
                    .containsImages(false)
                    .imageCount(0)
                    .summary(buildSummary("", false));
            return builder.build();

        } catch (Exception ex) {
            log.error("Failed to analyze file", ex);
            throw new RuntimeException("Failed to analyze document: " + ex.getMessage(), ex);
        }
    }

    private Summary buildSummary(String text, boolean hasImages) {
        boolean hasText = text != null && text.trim().length() > TEXT_THRESHOLD;
        boolean isBlank = !hasText && !hasImages;
        return Summary.builder()
                .isBlank(isBlank)
                .hasText(hasText)
                .hasImages(hasImages)
                .build();
    }

    private String determineContentType(String text, boolean hasImages) {
        boolean hasText = text != null && text.trim().length() > TEXT_THRESHOLD;
        if (hasText && hasImages) return "MIXED_CONTENT";
        if (hasText) return "TEXT_BASED";
        if (hasImages) return "IMAGE_BASED";
        return "UNKNOWN";
    }
}
