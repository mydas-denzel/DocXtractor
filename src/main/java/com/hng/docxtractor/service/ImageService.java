package com.hng.docxtractor.service;

import java.io.InputStream;

public interface ImageService {
    /**
     * Extract (OCR) text from an uploaded image InputStream.
     */
    String extractTextFromImage(InputStream imageStream);
}
