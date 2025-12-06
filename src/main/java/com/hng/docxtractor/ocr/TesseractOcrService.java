package com.hng.docxtractor.ocr;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Tesseract-based OCR implementation. If Tess4J or native tessdata is missing,
 * this will log warnings and return empty strings (so the whole pipeline still runs).
 *
 * Make sure to add Tess4J to your pom and set the tessdata path in application.yml:
 * ocr.tessdata-path: /usr/share/tessdata
 */
@Component
@Slf4j
public class TesseractOcrService implements OcrService {

    @Value("${ocr.tessdata-path:/usr/share/tessdata}")
    private String tessDataPath;

    @Value("${ocr.language:eng}")
    private String language;

    private ITesseract tesseract;
    private boolean available = false;

    @PostConstruct
    public void init() {
        try {
            tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage(language);
            // quick test for availability would require an image; we only flag available=true
            available = true;
            log.info("Tesseract OCR configured with tessdata path: {}", tessDataPath);
        } catch (Exception ex) {
            available = false;
            log.warn("Tesseract initialization failed - OCR disabled. {}", ex.getMessage());
        }
    }

    @Override
    public String doOcr(InputStream imageInputStream) {
        if (!available) {
            log.warn("OCR requested but Tesseract is not available. Returning empty result.");
            return "";
        }
        try {
            BufferedImage image = ImageIO.read(imageInputStream);
            if (image == null) {
                return "";
            }
            String result = tesseract.doOCR(image);
            return result == null ? "" : result;
        } catch (TesseractException te) {
            log.warn("Tesseract OCR exception: {}", te.getMessage());
            return "";
        } catch (Exception e) {
            log.warn("Generic OCR failure: {}", e.getMessage());
            return "";
        }
    }
}
