package com.hng.docxtractor.service;

import java.io.InputStream;

public interface DocxService {
    /**
     * Extract text from DOCX.
     */
    String extractText(InputStream docxStream);

    /**
     * Quick check if docx contains images.
     */
    boolean hasImages(InputStream docxStream);

    /**
     * Count images in docx (approx).
     */
    int countImages(InputStream docxStream);
}
