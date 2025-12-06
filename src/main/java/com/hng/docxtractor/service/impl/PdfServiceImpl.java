package com.hng.docxtractor.service.impl;

import com.hng.docxtractor.ocr.OcrService;
import com.hng.docxtractor.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfServiceImpl.class);
    private final OcrService ocrService;

    @Override
    public String extractText(InputStream pdfStream) {
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text;
        } catch (Exception e) {
            log.warn("PDF text extraction failed: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public int countImages(InputStream pdfStream) {
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int count = 0;
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                // Render very low DPI just to detect if page has any visible content (cheap heuristic)
                BufferedImage bim = renderer.renderImageWithDPI(i, 16, ImageType.RGB);
                if (bim != null) {
                    // quick heuristic: check if image has more than just white pixels (approx)
                    boolean nonBlank = hasNonWhitePixels(bim);
                    if (nonBlank) count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.warn("PDF image count failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public String ocrPdfPages(InputStream pdfStream) {
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                // 300 DPI recommended for OCR quality
                BufferedImage bim = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                if (bim == null) continue;

                // Convert BufferedImage to InputStream for OCR service
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "PNG", baos);
                try (ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray())) {
                    String text = ocrService.doOcr(bis);
                    if (text != null && !text.isBlank()) {
                        sb.append(text).append("\n");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("PDF OCR failed: {}", e.getMessage());
            return "";
        }
    }

    // cheap heuristic to check non-blank page
    private boolean hasNonWhitePixels(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int stepX = Math.max(1, w / 20);
        int stepY = Math.max(1, h / 20);
        for (int x = 0; x < w; x += stepX) {
            for (int y = 0; y < h; y += stepY) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                if (!(r > 240 && g > 240 && b > 240)) {
                    return true;
                }
            }
        }
        return false;
    }
}
