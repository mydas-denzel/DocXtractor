package com.hng.docxtractor.service;

import org.springframework.web.multipart.MultipartFile;

public interface TextExtractionService {
    /**
     * Extract text from uploaded file and return a result object containing text + metadata
     */
    TextExtractionResult extractText(MultipartFile file) throws Exception;

    class TextExtractionResult {
        public final String text;
        public final boolean containsImages;
        public final int imageCount;
        public TextExtractionResult(String text, boolean containsImages, int imageCount) {
            this.text = text;
            this.containsImages = containsImages;
            this.imageCount = imageCount;
        }
    }
}
