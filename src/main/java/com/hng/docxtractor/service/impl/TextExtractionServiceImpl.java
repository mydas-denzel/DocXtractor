package com.hng.docxtractor.service.impl;

import com.hng.docxtractor.service.TextExtractionService;
import com.hng.docxtractor.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TextExtractionServiceImpl implements TextExtractionService {

    private final OcrService ocrService;
    private final Tika tika = new Tika();
    private static final int TEXT_THRESHOLD = 20;

    @Override
    public TextExtractionResult extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String detected = tika.detect(file.getInputStream(), filename);

        if (detected != null && detected.startsWith("image/")) {
            // OCR image
            String txt = ocrService.doOcr(file.getInputStream());
            return new TextExtractionResult(txt == null ? "" : txt, true, 1);
        }

        if (filename.toLowerCase().endsWith(".pdf") || "application/pdf".equals(detected)) {
            return extractFromPdf(file.getInputStream());
        }

        if (filename.toLowerCase().endsWith(".docx") || detected.contains("officedocument")) {
            return extractFromDocx(file.getInputStream());
        }

        // fallback: try tika
        String content = tika.parseToString(file.getInputStream());
        boolean hasText = content != null && content.trim().length() > TEXT_THRESHOLD;
        return new TextExtractionResult(hasText ? content : "", false, 0);
    }

    private TextExtractionResult extractFromDocx(InputStream is) {
        try (XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            List<XWPFPictureData> pics = doc.getAllPictures();
            int count = pics == null ? 0 : pics.size();
            return new TextExtractionResult(text == null ? "" : text, count > 0, count);
        } catch (Exception e) {
            return new TextExtractionResult("", false, 0);
        }
    }

    private TextExtractionResult extractFromPdf(InputStream is) {
        try (PDDocument pdf = PDDocument.load(is)) {
            // basic text extraction
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            boolean hasText = text != null && text.trim().length() > TEXT_THRESHOLD;

            // count pages with content heuristic
            PDFRenderer renderer = new PDFRenderer(pdf);
            int imagePageCount = 0;
            for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 16, ImageType.RGB);
                if (img != null && hasNonWhitePixels(img)) imagePageCount++;
            }

            // if not enough text and pages have images, do OCR on pages
            if (!hasText && imagePageCount > 0) {
                StringBuilder sb = new StringBuilder();
                PDFRenderer r2 = new PDFRenderer(pdf);
                for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                    BufferedImage bim = r2.renderImageWithDPI(i, 300, ImageType.RGB);
                    if (bim == null) continue;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bim, "PNG", baos);
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray())) {
                        String ocrText = ocrService.doOcr(bis);
                        if (ocrText != null && !ocrText.isBlank()) {
                            sb.append(ocrText).append("\n");
                        }
                    }
                }
                String ocrRes = sb.toString();
                String finalText = (text == null ? "" : text) + "\n" + ocrRes;
                return new TextExtractionResult(finalText.trim(), imagePageCount > 0, imagePageCount);
            }

            return new TextExtractionResult(text == null ? "" : text, imagePageCount > 0, imagePageCount);

        } catch (Exception e) {
            return new TextExtractionResult("", false, 0);
        }
    }

    // cheap heuristic to detect non-white pixels
    private boolean hasNonWhitePixels(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int stepX = Math.max(1, w / 20);
        int stepY = Math.max(1, h / 20);
        for (int x = 0; x < w; x += stepX) {
            for (int y = 0; y < h; y += stepY) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                if (!(r > 240 && g > 240 && b > 240)) return true;
            }
        }
        return false;
    }
}
