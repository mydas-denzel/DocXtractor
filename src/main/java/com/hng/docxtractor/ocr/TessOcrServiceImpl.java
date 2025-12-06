package com.hng.docxtractor.ocr;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

@Service
@Slf4j
public class TessOcrServiceImpl implements OcrService {

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
            available = true;
            log.info("Tesseract initialized using datapath {}", tessDataPath);
        } catch (Exception ex) {
            log.warn("Tess initialization failed: {}", ex.getMessage());
            available = false;
        }
    }

    @Override
    public String doOcr(InputStream imageInputStream) {
        if (!available) {
            log.warn("OCR not available");
            return "";
        }
        try {
            BufferedImage img = ImageIO.read(imageInputStream);
            if (img == null) return "";
            String res = tesseract.doOCR(img);
            return res == null ? "" : res;
        } catch (TesseractException te) {
            log.warn("Tess exception: {}", te.getMessage());
            return "";
        } catch (Exception e) {
            log.warn("OCR failure: {}", e.getMessage());
            return "";
        }
    }
}
