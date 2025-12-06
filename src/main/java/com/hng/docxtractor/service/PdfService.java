package com.hng.docxtractor.service;

import java.io.InputStream;

public interface PdfService {
    /**
     * Extract text from a PDF InputStream. Consumes the stream.
     */
    String extractText(InputStream pdfStream);

    /**
     * Count pages that contain images (approximation).
     */
    int countImages(InputStream pdfStream);

    /**
     * Render pages and run OCR; returns concatenated text results.
     */
    String ocrPdfPages(InputStream pdfStream);
}
