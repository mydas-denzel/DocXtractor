package com.hng.docxtractor.ocr;

import java.io.InputStream;

/**
 * Thin abstraction for OCR so we can swap implementations or mock in tests.
 */
public interface OcrService {
    /**
     * Perform OCR on the provided image InputStream. Returns empty string on failure/no text.
     */
    String doOcr(InputStream imageInputStream);
}
