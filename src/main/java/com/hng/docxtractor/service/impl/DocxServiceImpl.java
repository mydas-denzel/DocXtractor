package com.hng.docxtractor.service.impl;

import com.hng.docxtractor.service.DocxService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@Slf4j
public class DocxServiceImpl implements DocxService {

    @Override
    public String extractText(InputStream docxStream) {
        try (XWPFDocument doc = new XWPFDocument(docxStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            return text == null ? "" : text;
        } catch (Exception e) {
            log.warn("DOCX extraction failed: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public boolean hasImages(InputStream docxStream) {
        try (XWPFDocument doc = new XWPFDocument(docxStream)) {
            List<XWPFPictureData> pics = doc.getAllPictures();
            return pics != null && !pics.isEmpty();
        } catch (Exception e) {
            log.warn("DOCX hasImages check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int countImages(InputStream docxStream) {
        try (XWPFDocument doc = new XWPFDocument(docxStream)) {
            List<XWPFPictureData> pics = doc.getAllPictures();
            return pics == null ? 0 : pics.size();
        } catch (Exception e) {
            log.warn("DOCX countImages failed: {}", e.getMessage());
            return 0;
        }
    }
}
