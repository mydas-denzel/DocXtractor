package com.hng.docxtractor.service.impl;

import com.hng.docxtractor.ocr.OcrService;
import com.hng.docxtractor.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final OcrService ocrService;

    @Override
    public String extractTextFromImage(InputStream imageStream) {
        return ocrService.doOcr(imageStream);
    }
}
